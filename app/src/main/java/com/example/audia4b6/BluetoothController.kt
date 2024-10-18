package com.example.audia4b6

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.io.IOException
import java.io.OutputStream
import java.util.UUID

class BluetoothController(private val context: Context) {
    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bluetoothSocket: BluetoothSocket? = null
    private var outputStream: OutputStream? = null

    // UUID für die serielle Kommunikation (Standard)
    private val MY_UUID: UUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB")

    init {
        // BluetoothManager initialisieren und BluetoothAdapter erhalten
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    // Verbindung zu einem Gerät herstellen
    fun connectToDevice(deviceAddress: String): Boolean {
        // Berechtigungen überprüfen
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            Log.e("BluetoothController", "Berechtigung zum Verbinden von Bluetooth fehlt.")
            return false // Berechtigung fehlt
        }

        val device: BluetoothDevice? = bluetoothAdapter?.getRemoteDevice(deviceAddress)

        return try {
            bluetoothSocket = device?.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothSocket?.connect() // Versuche, eine Verbindung herzustellen
            outputStream = bluetoothSocket?.outputStream
            Log.d("BluetoothController", "Connected to $deviceAddress")
            true // Verbindung erfolgreich
        } catch (e: IOException) {
            Log.e("BluetoothController", "Connection failed: ${e.message}")
            e.printStackTrace()
            false // Verbindung fehlgeschlagen
        }
    }

    // Verbindung trennen
    fun disconnect() {
        try {
            outputStream?.close()
            bluetoothSocket?.close()
            Log.d("BluetoothController", "Disconnected")
        } catch (e: IOException) {
            Log.e("BluetoothController", "Failed to disconnect: ${e.message}")
            e.printStackTrace()
        }
    }

    // Befehl an das verbundene Gerät senden
    fun sendCommand(command: String) {
        try {
            outputStream?.write(command.toByteArray())
            Log.d("BluetoothController", "Sent command: $command")
        } catch (e: IOException) {
            Log.e("BluetoothController", "Error sending command: ${e.message}")
            e.printStackTrace()
        }
    }
}
