package com.iwakura.an9hack

import android.bluetooth.BluetoothAdapter
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.CheckBox
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.graphics.Insets
import androidx.core.view.OnApplyWindowInsetsListener
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class PermissionsActivity : AppCompatActivity() {
    private lateinit var checkBox1: CheckBox
    private lateinit var checkBox2: CheckBox
    private lateinit var checkBox3: CheckBox
    private lateinit var startButton: Button
    private lateinit var mBluetoothAdapter: BluetoothAdapter

    private val permissions1 = arrayOf(
        android.Manifest.permission.BLUETOOTH_CONNECT,
        android.Manifest.permission.BLUETOOTH_SCAN
    )
    private val permissions2 = arrayOf(
        android.Manifest.permission.BLUETOOTH,
        android.Manifest.permission.ACCESS_FINE_LOCATION,
        android.Manifest.permission.ACCESS_COARSE_LOCATION
    )

    private val activityResultLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result: ActivityResult ->
        if (result.resultCode == RESULT_OK) {
            result.data
        } else {
            Toast.makeText(this, "meh, denied :(", Toast.LENGTH_SHORT).show()
        }
        updateCheckboxes()
    }
    private val multiplePermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        updateCheckboxes()
    }
    private val multiplePermissionLauncher2 = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        updateCheckboxes()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_permissions)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), OnApplyWindowInsetsListener { v: View, insets: WindowInsetsCompat ->
            val systemBars: Insets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        })
        startButton = findViewById(R.id.startButton)
        startButton.setOnClickListener {
            startActivity(Intent(this, ScanActivity::class.java))
            finish()
        }
        checkBox1 = findViewById(R.id.checkBox1)
        checkBox2 = findViewById(R.id.checkBox2)
        checkBox3 = findViewById(R.id.checkBox3)
        checkBox1.setOnClickListener { askPerms(1) }
        checkBox2.setOnClickListener { askPerms(2) }
        checkBox3.setOnClickListener { askPerms(3) }
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter()
        initCheckboxes()
        updateCheckboxes()
    }

    override fun onResume() {
        super.onResume()
        updateCheckboxes()
    }

    override fun onRestart() {
        super.onRestart()
        updateCheckboxes()
    }

    private fun initCheckboxes() {
        checkBox1.isChecked = false
        checkBox2.isChecked = false
        checkBox3.isChecked = false
        if (Build.VERSION.SDK_INT >= 31) {
            checkBox1.visibility = View.VISIBLE
            checkBox2.visibility = View.GONE
            checkBox3.visibility = View.VISIBLE
        } else {
            checkBox1.visibility = View.VISIBLE
            checkBox2.visibility = View.VISIBLE
            checkBox3.visibility = View.GONE
        }
    }

    private fun updateCheckboxes() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (mBluetoothAdapter.isEnabled) {
                checkBox1.isChecked = true
                checkBox1.isClickable = false
            } else {
                checkBox1.isChecked = false
                checkBox1.isClickable = true
            }
            if (ActivityCompat.checkSelfPermission(this, permissions1[0]) == 0 && ActivityCompat.checkSelfPermission(this, permissions1[1]) == 0) {
                checkBox3.isChecked = true
                checkBox3.isClickable = false
                checkBox1.isEnabled = true
            } else {
                checkBox3.isChecked = false
                checkBox3.isClickable = true
                checkBox1.isEnabled = false
            }
        } else {
            if (mBluetoothAdapter.isEnabled && ActivityCompat.checkSelfPermission(this, permissions2[0]) == 0) {
                checkBox1.isChecked = true
                checkBox1.isClickable = false
            } else {
                checkBox1.isChecked = false
                checkBox1.isClickable = true
            }
            if (ActivityCompat.checkSelfPermission(this, permissions2[1]) == 0 && ActivityCompat.checkSelfPermission(this, permissions2[2]) == 0) {
                checkBox2.isChecked = true
                checkBox2.isClickable = false
            } else {
                checkBox2.isChecked = false
                checkBox2.isClickable = true
            }
        }
        checkCheckboxes()
    }

    private fun checkCheckboxes() {
        if (Build.VERSION.SDK_INT >= 31) {
            startButton.isEnabled = checkBox1.isChecked && checkBox3.isChecked
        } else {
            startButton.isEnabled = checkBox1.isChecked && checkBox2.isChecked
        }
    }

    private fun askPerms(checkBoxID: Int) {
        if (Build.VERSION.SDK_INT >= 31) {
            if (checkBoxID == 1 && checkBox1.isChecked) askBluetoothPermissions()
            if (checkBoxID == 3 && checkBox3.isChecked) askNearbyDevicesPermission()
            return
        }
        if (checkBoxID == 1 && checkBox1.isChecked) askBluetoothPermissions()
        if (checkBoxID == 2 && checkBox2.isChecked) askLocationPermissions()
    }

    private fun askBluetoothPermissions() {
        if (Build.VERSION.SDK_INT >= 31) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(intent)
        } else if (ActivityCompat.checkSelfPermission(this, permissions2[0]) == 0) {
            val intent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
            activityResultLauncher.launch(intent)
        } else {
            Toast.makeText(this, "WOAH, Android isn't allowing the app to access bluetooth...", Toast.LENGTH_SHORT).show()
        }
    }

    private fun askNearbyDevicesPermission() {
        if (Build.VERSION.SDK_INT >= 31) {
            if (ActivityCompat.checkSelfPermission(this, permissions1[0]) == 0 && ActivityCompat.checkSelfPermission(this, permissions1[1]) == 0) {
                updateCheckboxes(); return
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions1[0]) || ActivityCompat.shouldShowRequestPermissionRationale(this, permissions1[1])) {
                updateCheckboxes()
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Permissions needed")
                    .setMessage(R.string.permissions_nearbyDevices_explanationText)
                    .setNegativeButton("Cancel") { d: DialogInterface, _ -> d.cancel() }
                    .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                        val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                        intent.data = Uri.parse("package:" + packageName)
                        startActivity(intent)
                    }.show()
                return
            }
            multiplePermissionLauncher.launch(permissions1)
        }
    }

    private fun askLocationPermissions() {
        if (Build.VERSION.SDK_INT < 31) {
            if (ActivityCompat.checkSelfPermission(this, permissions2[1]) == 0 && ActivityCompat.checkSelfPermission(this, permissions2[2]) == 0) {
                updateCheckboxes(); return
            }
            if (ActivityCompat.shouldShowRequestPermissionRationale(this, permissions2[1]) || ActivityCompat.shouldShowRequestPermissionRationale(this, permissions2[2])) {
                updateCheckboxes()
                val dialog = AlertDialog.Builder(this)
                dialog.setTitle("Permissions needed")
                    .setMessage(R.string.permissions_location_explanationText)
                    .setNegativeButton("Cancel") { d: DialogInterface, _ -> d.cancel() }
                    .setPositiveButton("OK") { _: DialogInterface, _: Int ->
                        val intent = Intent("android.settings.APPLICATION_DETAILS_SETTINGS")
                        intent.data = Uri.parse("package:" + packageName)
                        startActivity(intent)
                    }.show()
                return
            }
            multiplePermissionLauncher2.launch(arrayOf(android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION))
        }
    }
}


