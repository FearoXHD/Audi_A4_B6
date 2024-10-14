package com.example.audia4b6

import android.Manifest
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothSocket
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Binder
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.ActivityCompat
import java.io.IOException
import java.util.*

class BluetoothService : Service() {

    private val binder = LocalBinder()
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var isConnected: Boolean = false
    private val deviceAddress = "88:13:BF:69:3A:86" // Ersetze dies durch deine MAC-Adresse
    private val uuid: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB") // Standard-UUID für SPP

    inner class LocalBinder : Binder() {
        fun getService(): BluetoothService = this@BluetoothService
    }

    override fun onBind(intent: Intent?): IBinder {
        return binder
    }

    fun initializeBluetooth() {
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

    fun closeConnection() {
        try {
            bluetoothSocket?.close()
            isConnected = false
            Log.d("BluetoothService", "Bluetooth-Verbindung geschlossen")
        } catch (e: IOException) {
            Log.e("BluetoothService", "Fehler beim Schließen: ${e.message}")
        }
    }

    fun sendBluetoothCommand(command: String) {
        if (bluetoothSocket != null && isConnected) {
            try {
                bluetoothSocket!!.outputStream.write(command.toByteArray())
            } catch (e: IOException) {
                Log.e("BluetoothService", "Fehler beim Senden: ${e.message}")
            }
        }
    }

    fun isBluetoothConnected(): Boolean {
        return isConnected
    }
}
