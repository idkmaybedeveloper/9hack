package com.iwakura.an9hack

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class ScanActivity : AppCompatActivity() {
    companion object { const val EXTRAS_BLE_ADDRESS = "BLE_ADDRESS" }

    private lateinit var bleArrayAdapter: ArrayAdapter<String>
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var scanButton: Button
    private val bluetoothDeviceList: MutableList<BluetoothDevice> = mutableListOf()
    private var isScanning = false
    private val handler = Handler()

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (!bluetoothDeviceList.contains(result.device)) {
                bluetoothDeviceList.add(result.device)
                val name = result.device.name ?: "No Name"
                bleArrayAdapter.add("$name\n${result.device.address}")
                bleArrayAdapter.notifyDataSetChanged()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_scan)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), OnApplyWindowInsetsListener { v: View, insets: WindowInsetsCompat ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        })
        scanButton = findViewById(R.id.scan_button)
        scanButton.setOnClickListener { checkAndStart() }
        val scanList: ListView = findViewById(R.id.device_list)
        scanList.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            Toast.makeText(this, "Address: ${bluetoothDeviceList[position].address}", Toast.LENGTH_SHORT).show()
            val intent = Intent(this, ControlActivity::class.java)
            intent.putExtra(EXTRAS_BLE_ADDRESS, bluetoothDeviceList[position].address)
            scanLeDevice(false)
            bluetoothAdapter.cancelDiscovery()
            startActivity(intent)
            finish()
        }
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        bleArrayAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1)
        scanList.adapter = bleArrayAdapter
    }

    private fun checkAndStart() {
        if (checkPerms()) {
            scanLeDevice(!isScanning)
        } else {
            startActivity(Intent(this, PermissionsActivity::class.java))
        }
    }

    private fun scanLeDevice(scanning: Boolean) {
        if (scanning) {
            handler.postDelayed({
                scanButton.setText(R.string.scan_scanButtonText)
                bluetoothLeScanner.stopScan(leScanCallback)
                bluetoothLeScanner.flushPendingScanResults(leScanCallback)
                Log.d("Scan", "Stopped Scanning")
                isScanning = false
            }, 10_000)
            bluetoothDeviceList.clear()
            bleArrayAdapter.clear()
            bleArrayAdapter.notifyDataSetChanged()
            scanButton.setText(R.string.scan_scanButtonText2)
            bluetoothLeScanner.startScan(leScanCallback)
            Log.d("Scan", "Scanning...")
            isScanning = true
            return
        }
        scanButton.setText(R.string.scan_scanButtonText)
        handler.removeCallbacksAndMessages(null)
        bluetoothLeScanner.stopScan(leScanCallback)
        bluetoothLeScanner.flushPendingScanResults(leScanCallback)
        Log.d("Scan", "Stopped Scanning")
        isScanning = false
    }

    override fun onPause() {
        super.onPause()
        scanLeDevice(false)
    }

    override fun onStop() {
        super.onStop()
        scanLeDevice(false)
        bluetoothAdapter.cancelDiscovery()
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
}


