package com.example.audia4b6

import MainActivity2
import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private var bluetoothGatt: BluetoothGatt? = null
    private var characteristic: BluetoothGattCharacteristic? = null

    private val serviceUUID = UUID.fromString("4fafc201-1fb5-459e-8fcc-c5c9c331914b")
    private val characteristicUUID = UUID.fromString("beb5483e-36e1-4688-b7f5-ea07361b26a8")

    private val REQUEST_ENABLE_BT = 1
    private val PERMISSIONS_REQUEST_CODE = 2

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)

        val buttonNavigate: ImageButton = findViewById(R.id.nav_map_Button)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, MainActivity2::class.java)
            startActivity(intent)
        }

        val connectButton: Button = findViewById(R.id.connectButton)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        connectButton.setOnClickListener {
            if (checkPermissions()) {
                scanBLEDevices()
            }
        }
    }

    private fun checkPermissions(): Boolean {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        return if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSIONS_REQUEST_CODE)
            false
        } else {
            true
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            PERMISSIONS_REQUEST_CODE -> {
                if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                    scanBLEDevices()
                } else {
                    Toast.makeText(this, "Berechtigungen sind erforderlich, um BLE zu verwenden", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun scanBLEDevices() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Scan-Berechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothAdapter.bluetoothLeScanner.startScan(object : ScanCallback() {
            @SuppressLint("MissingPermission")
            override fun onScanResult(callbackType: Int, result: ScanResult?) {
                val device = result?.device
                if (device?.name == "Audi A4 B6 (Alpha)") {
                    bluetoothAdapter.bluetoothLeScanner.stopScan(this)
                    connectToDevice(device)
                }
            }
        })
    }

    private fun connectToDevice(device: BluetoothDevice) {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Toast.makeText(this, "Verbindungs-Berechtigung nicht erteilt", Toast.LENGTH_SHORT).show()
            return
        }

        bluetoothGatt = device.connectGatt(this, false, object : BluetoothGattCallback() {
            @SuppressLint("MissingPermission")
            override fun onConnectionStateChange(gatt: BluetoothGatt?, status: Int, newState: Int) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    bluetoothGatt?.discoverServices()
                }
            }

            override fun onServicesDiscovered(gatt: BluetoothGatt?, status: Int) {
                val service = bluetoothGatt?.getService(serviceUUID)
                characteristic = service?.getCharacteristic(characteristicUUID)
            }
        })
    }

    @SuppressLint("MissingPermission")
    private fun sendMessage(message: String) {
        characteristic?.let {
            it.value = message.toByteArray()
            bluetoothGatt?.writeCharacteristic(it)
        }
    }
}
