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
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.ImageButton
import android.widget.TextView
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.io.IOException
import java.util.*

class MainActivity : Activity() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private val deviceAddress = "88:13:BF:61:95:2A" // Ersetze dies durch die gefundene MAC-Adresse
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard-UUID für SPP
    private val PERMISSION_REQUEST_CODE = 1
    private var isConnected: Boolean = false
    private lateinit var carStatusTextView: TextView
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Initialisiere den TextView für den Status
        carStatusTextView = findViewById(R.id.carStatusTextView)
        carStatusTextView.text = "Status: Unknown" // Initialer Status auf "Unknown" setzen

        val buttonNavigate: ImageButton = findViewById(R.id.nav_map_Button)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }

        val buttonLight: ImageButton = findViewById(R.id.light_Button)
        buttonLight.setOnClickListener {
            val intent = Intent(this, LightActivity::class.java)
            startActivity(intent)
        }


        // Überprüfe und fordere Berechtigungen an
        checkAndRequestPermissions()

        // Hole den Unlock-Button und setze den OnClickListener
        val unlockButton = findViewById<ImageButton>(R.id.unlock_Button)
        unlockButton.setOnClickListener {
            if (isConnected) {
                sendBluetoothCommand("ud\n") // Sende "ud" zum Entsperren der Türen
                carStatusTextView.text = "Status: Unlocked"
            } else {
                Toast.makeText(this, "Nicht verbunden.", Toast.LENGTH_SHORT).show()
                Log.e("Bluetooth", "Nicht verbunden.")
            }
        }

        // Hole den Lock-Button und setze den OnClickListener
        val lockButton = findViewById<ImageButton>(R.id.lock_Button)
        lockButton.setOnClickListener {
            if (isConnected) {
                sendBluetoothCommand("ld\n") // Sende "ld" zum Verriegeln der Türen
                carStatusTextView.text = "Status: Locked"
            } else {
                Toast.makeText(this, "Nicht verbunden.", Toast.LENGTH_SHORT).show()
                Log.e("Bluetooth", "Nicht verbunden.")
            }
        }

        // Starte die Überprüfung auf eingehende Bluetooth-Daten
        startBluetoothDataCheck()
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

            // Status abfragen, sobald die Verbindung hergestellt ist
            sendBluetoothCommand("ds\n")
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

    private fun startBluetoothDataCheck() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                checkBle()
                handler.postDelayed(this, 1000) // Überprüfe alle 1 Sekunde
            }
        }, 1000)
    }

    private fun checkBle() {
        val buffer = ByteArray(1024)
        var bytes: Int

        try {
            if (bluetoothSocket != null && bluetoothSocket!!.inputStream.available() > 0) {
                bytes = bluetoothSocket!!.inputStream.read(buffer)
                val incomingMessage = String(buffer, 0, bytes).trim()

                // Status abhängig von der Nachricht setzen
                when (incomingMessage) {
                    "Türen Gesperrt" -> {
                        runOnUiThread {
                            carStatusTextView.text = "Status: Locked"
                        }
                    }
                    "Türen Entsperrt" -> {
                        runOnUiThread {
                            carStatusTextView.text = "Status: Unlocked"
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
        if (!isConnected) {
            initializeBluetooth()
        }
        Log.d("MainActivity", "Activity is resumed")
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacksAndMessages(null)
        try {
            bluetoothSocket?.close()
        } catch (e: IOException) {
            Log.e("Bluetooth", "Fehler beim Schließen: ${e.message}")
        }
    }
}
