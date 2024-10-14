package com.example.audia4b6

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class MainActivity : Activity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceAddress = "88:13:BF:69:3A:86" // Ersetze dies durch die gefundene MAC-Adresse
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard-UUID für SPP
    private val PERMISSION_REQUEST_CODE = 1
    private var isConnected: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val buttonNavigate: ImageButton = findViewById(R.id.nav_map_Button)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        // Überprüfe und fordere Berechtigungen an
        checkAndRequestPermissions()

        // Hole den Unlock-Button
        val unlockButton = findViewById<ImageButton>(R.id.unlock_Button)
        unlockButton.setOnClickListener {
            if (isConnected) {
                sendBluetoothCommand("ud\n") // Sende "ud" zum Entsperren der Türen
            } else {
                Toast.makeText(this, "Nicht verbunden.", Toast.LENGTH_SHORT).show()
                Log.e("Bluetooth", "Nicht verbunden.")
            }
        }

        // Hole den Lock-Button
        val lockButton = findViewById<ImageButton>(R.id.lock_Button)
        lockButton.setOnClickListener {
            if (isConnected) {
                sendBluetoothCommand("ld\n") // Sende "ld" zum Schließen der Türen
            } else {
                Toast.makeText(this, "Nicht verbunden.", Toast.LENGTH_SHORT).show()
                Log.e("Bluetooth", "Nicht verbunden.")
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsNeeded = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_SCAN)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_CONNECT)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionsNeeded.add(Manifest.permission.BLUETOOTH_ADMIN)
            }
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            permissionsNeeded.add(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        if (permissionsNeeded.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), PERMISSION_REQUEST_CODE)
        } else {
            initializeBluetooth()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                initializeBluetooth()
            } else {
                Log.e("Bluetooth", "Berechtigungen wurden verweigert.")
                Toast.makeText(this, "Berechtigungen wurden verweigert.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initializeBluetooth() {
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter()

        if (bluetoothAdapter == null || !bluetoothAdapter!!.isEnabled) {
            Log.e("Bluetooth", "Bluetooth ist nicht verfügbar oder nicht aktiviert")
            Toast.makeText(this, "Bluetooth ist nicht verfügbar oder nicht aktiviert", Toast.LENGTH_SHORT).show()
            return
        }

        val device: BluetoothDevice = bluetoothAdapter!!.getRemoteDevice(deviceAddress)
        try {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                Log.e("Bluetooth", "Berechtigung zum Verbinden von Bluetooth fehlt.")
                return
            }
            bluetoothSocket = device.createRfcommSocketToServiceRecord(uuid)
            bluetoothSocket!!.connect()
            isConnected = true
            Log.d("Bluetooth", "Bluetooth verbunden")
            Toast.makeText(this, "Bluetooth verbunden", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Verbindung fehlgeschlagen: ${e.message}")
            isConnected = false
            Toast.makeText(this, "Verbindung fehlgeschlagen", Toast.LENGTH_SHORT).show()
        } catch (e: SecurityException) {
            Log.e("Bluetooth", "SecurityException: ${e.message}")
        }
    }

    private fun sendBluetoothCommand(command: String) {
        if (bluetoothSocket != null) {
            try {
                bluetoothSocket!!.outputStream.write(command.toByteArray())
            } catch (e: IOException) {
                Log.e("Bluetooth", "Fehler beim Senden: ${e.message}")
                Toast.makeText(this, "Fehler beim Senden: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun checkBle() {
        val buffer = ByteArray(1024)
        var bytes: Int

        try {
            if (bluetoothSocket!!.inputStream.available() > 0) {
                bytes = bluetoothSocket!!.inputStream.read(buffer)
                val incomingMessage = String(buffer, 0, bytes).trim()

                when (incomingMessage) {
                    "Türen entsperrt" -> {
                        runOnUiThread {
                            Toast.makeText(this, "Türen erfolgreich entsperrt", Toast.LENGTH_SHORT).show()
                        }
                    }
                    "Türen verriegelt" -> {
                        runOnUiThread {
                            Toast.makeText(this, "Türen erfolgreich verriegelt", Toast.LENGTH_SHORT).show()
                        }
                    }
                    else -> {
                        Log.d("Bluetooth", "Nachricht erhalten: $incomingMessage")
                    }
                }
            }
        } catch (e: IOException) {
            Log.e("Bluetooth", "Fehler beim Lesen: ${e.message}")
        }
    }

    override fun onResume() {
        super.onResume()
        Log.d("MainActivity", "Activity is resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Fehler beim Schließen: ${e.message}")
        }
    }
}
