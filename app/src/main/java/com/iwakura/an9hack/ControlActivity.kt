package com.iwakura.an9hack

import android.bluetooth.*
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentResultListener
import androidx.viewpager2.widget.ViewPager2
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import java.math.BigInteger
import java.util.UUID

class ControlActivity : AppCompatActivity() {
    companion object {
        const val EXTRAS_BLE_ADDRESS = "BLE_ADDRESS"
        val UART_SERVICE: UUID = UUID.fromString("6e400001-b5a3-f393-e0a9-e50e24dcca9e")
        val UART_TX_CHAR: UUID = UUID.fromString("6e400003-b5a3-f393-e0a9-e50e24dcca9e")
        val UART_RX_CHAR: UUID = UUID.fromString("6e400002-b5a3-f393-e0a9-e50e24dcca9e")
    }

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothGatt: BluetoothGatt
    private lateinit var logText: TextView
    private val commandCreator = CommandCreator()
    private var connectCount = 0

    private val gattCallback: BluetoothGattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            super.onConnectionStateChange(gatt, status, newState)
            if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_CONNECTED) {
                addLogText("meow!~ connected!")
                Handler(Looper.getMainLooper()).postDelayed({
                    val started = gatt.discoverServices()
                    if (started) addLogText("Discovering Services...") else addLogText("Couldn't start Discovering Services")
                }, 1000)
            } else if (status == BluetoothGatt.GATT_SUCCESS && newState == BluetoothProfile.STATE_DISCONNECTED) {
                addLogText("Disconnected")
                controlTabs(false)
            } else if (status != BluetoothGatt.GATT_SUCCESS) {
                addLogText("Couldn't Connect")
                gatt.disconnect()
                if (connectCount < 3) {
                    addLogText("Trying again...")
                    connect(intent.getStringExtra(EXTRAS_BLE_ADDRESS)!!)
                } else {
                    runOnUiThread {
                        AlertDialog.Builder(this@ControlActivity)
                            .setTitle("oh... not connect")
                            .setMessage(R.string.control_dialog_could_not_connect)
                            .setPositiveButton("OK") { dialogInterface: DialogInterface, _: Int -> finish() }
                            .show()
                    }
                }
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            super.onServicesDiscovered(gatt, status)
            if (status == BluetoothGatt.GATT_SUCCESS) {
                addLogText("Services Discovered!")
                for (service in bluetoothGatt.services) {
                    if (service.uuid == UART_SERVICE) enableNotifications(service.getCharacteristic(UART_TX_CHAR))
                }
            }
        }

        override fun onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorRead(gatt, descriptor, status)
            val ok = bluetoothGatt.setCharacteristicNotification(descriptor.characteristic, true)
            if (ok) {
                addLogText("Setting Enable Notifications for Characteristic...")
                if (Build.VERSION.SDK_INT >= 33) {
                    val st = bluetoothGatt.writeDescriptor(descriptor, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
                    if (st != BluetoothStatusCodes.SUCCESS) addLogText("Couldn't write Enable Indication to Descriptor")
                } else {
                    if (!descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) addLogText("Couldn't set value for descriptor")
                    val wrote = bluetoothGatt.writeDescriptor(descriptor)
                    if (!wrote) addLogText("Couldn't write Enable Indication to Descriptor")
                }
            } else addLogText("Couldn't Set Enable Notifications for Characteristic")
        }

        override fun onDescriptorWrite(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor, status: Int) {
            super.onDescriptorWrite(gatt, descriptor, status)
            if (descriptor.uuid == UUID.fromString("00002902-0000-1000-8000-00805f9b34fb") && status == BluetoothGatt.GATT_SUCCESS) {
                addLogText("Successfully enabled Notifications")
                writeFirstStage()
            }
        }

        override fun onCharacteristicWrite(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic, status: Int) {
            super.onCharacteristicWrite(gatt, characteristic, status)
            if (characteristic == bluetoothGatt.getService(UART_SERVICE).getCharacteristic(UART_RX_CHAR) && status == BluetoothGatt.GATT_SUCCESS) {
                addLogText("Successfully Wrote to Characteristic")
            }
        }

        override fun onCharacteristicChanged(gatt: BluetoothGatt, characteristic: BluetoothGattCharacteristic) {
            super.onCharacteristicChanged(gatt, characteristic)
            val value = characteristic.value
            Log.i("onCC", "Characteristic: ${characteristic.uuid}, changed to: ${toHex(value)}")
            if (characteristic.uuid == UART_TX_CHAR) {
                addLogText("Got Message, Intercepting...")
                commandCreator.interceptMessage(value)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_control)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), OnApplyWindowInsetsListener { v: View, insets: WindowInsetsCompat ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        })
        supportFragmentManager.setFragmentResultListener("tab1tx", this, FragmentResultListener { _, bundle ->
            val result = bundle.getByteArray("data") ?: return@FragmentResultListener
            when (result[0]) {
                0.toByte() -> writeCommand(commandCreator.getUnlockCommand())
                1.toByte() -> writeCommand(commandCreator.getLockCommand())
                2.toByte() -> {
                    val mode = when (result[1].toInt()) { 0 -> 1.toByte(); 1 -> 2.toByte(); 2 -> 3.toByte(); else -> 0 }
                    val sw = result[2]
                    var headlight: Byte = 0
                    var throttle: Byte = 0
                    when (sw.toInt()) {
                        0 -> { headlight = 1; throttle = 1 }
                        1 -> { headlight = 2 }
                        2 -> { throttle = 2 }
                        3 -> { headlight = 2; throttle = 2 }
                    }
                    writeCommand(commandCreator.getSetScooterCommand(headlight, mode, throttle))
                }
            }
        })
        init()
        if (checkPerms()) start() else startActivity(Intent(this, PermissionsActivity::class.java))
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                bluetoothGatt.disconnect()
                bluetoothGatt.close()
                finish()
            }
        })
    }

    private fun start() {
        addLogText("Started")
        connect(intent.getStringExtra(EXTRAS_BLE_ADDRESS)!!)
    }

    private fun connect(address: String) {
        addLogText("Connecting to: $address")
        val device = bluetoothAdapter.getRemoteDevice(address)
        bluetoothGatt = device.connectGatt(this, false, gattCallback, BluetoothDevice.TRANSPORT_LE)
        connectCount++
    }

    private fun enableNotifications(characteristicData: BluetoothGattCharacteristic) {
        addLogText("Enabling Notifications...")
        val descriptor = characteristicData.getDescriptor(UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"))
        if (!bluetoothGatt.readDescriptor(descriptor)) addLogText("Couldn't Read Descriptor.")
    }

    private fun writeFirstStage() {
        addLogText("Trying to write GetKey Command...")
        val characteristic = bluetoothGatt.getService(UART_SERVICE).getCharacteristic(UART_RX_CHAR)
        characteristic.value = commandCreator.getKeyCommand(getString(R.string.control_defaultBleKey))
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val status = bluetoothGatt.writeCharacteristic(characteristic)
        if (status) addLogText("Writing Command...") else addLogText("Couldn't write command!")
    }

    private fun init() {
        val viewPager: ViewPager2 = findViewById(R.id.viewPager2)
        val tabLayout: TabLayout = findViewById(R.id.tabs)
        val scrollView: ScrollView = findViewById(R.id.scrollView)
        logText = findViewById(R.id.logTextView)
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        logText.text = ""
        scrollView.post { scrollView.fullScroll(View.FOCUS_DOWN) }
        viewPager.adapter = TabPagerAdapter(this)
        TabLayoutMediator(tabLayout, viewPager) { tab, i -> tab.text = tabNames(i) }.attach()
        controlTabs(false)
        commandCreator.init(this)
    }

    private fun writeCommand(command: ByteArray) {
        val characteristic = bluetoothGatt.getService(UART_SERVICE).getCharacteristic(UART_RX_CHAR)
        characteristic.value = command
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        val status = bluetoothGatt.writeCharacteristic(characteristic)
        if (status) addLogText("Writing Command...") else addLogText("Couldn't write command!")
    }

    private fun tabNames(position: Int): String? = when (position) {
        0 -> resources.getString(R.string.control_tab1Text)
        1 -> resources.getString(R.string.control_tab2Text)
        2 -> resources.getString(R.string.control_tab3Text)
        else -> null
    }

    fun addLogText(text: String?) {
        if (text != null) runOnUiThread {
            logText.append(text + '\n')
            logText.postInvalidate()
        }
    }

    fun controlTabs(enabled: Boolean) {
        val result = Bundle()
        result.putBoolean("data", enabled)
        supportFragmentManager.setFragmentResult("tab1rx", result)
    }

    private fun checkPerms(): Boolean {
        val permissions = if (Build.VERSION.SDK_INT >= 31) arrayOf(
            android.Manifest.permission.BLUETOOTH_CONNECT,
            android.Manifest.permission.BLUETOOTH_SCAN
        ) else arrayOf(
            android.Manifest.permission.BLUETOOTH,
            android.Manifest.permission.ACCESS_FINE_LOCATION,
            android.Manifest.permission.ACCESS_COARSE_LOCATION
        )
        return hasPermissions(this, *permissions) && bluetoothAdapter.isEnabled
    }

    private fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
        if (context != null && permissions.isNotEmpty()) {
            for (permission in permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != 0) return false
            }
            return true
        }
        return true
    }

    private fun toHex(bytes: ByteArray): String {
        val bi = BigInteger(1, bytes)
        return String.format("%0" + (bytes.size shl 1) + "X", bi)
    }
}


