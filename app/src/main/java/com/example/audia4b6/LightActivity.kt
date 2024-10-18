package com.example.audia4b6

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageButton
import android.widget.Switch
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

class LightActivity : AppCompatActivity() {

    private val REQUEST_CODE_PERMISSIONS = 1001
    private lateinit var bluetoothController: BluetoothController // BluetoothController-Instanz

    private var selectedColor: Int = Color.RED

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_light) // Stelle sicher, dass diese Datei existiert

        // Instanziiere den BluetoothController mit dem aktuellen Kontext
        bluetoothController = BluetoothController(this)

        // Berechtigungen überprüfen und anfordern
        checkAndRequestPermissions()

        // LED Switch einrichten
        val ledSwitch = findViewById<Switch>(R.id.switchLed)
        ledSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                sendCommand("ON") // LED einschalten
            } else {
                sendCommand("OFF") // LED ausschalten
            }
        }

        // Color Picker Wheel einrichten
        val colorPickerWheel = findViewById<ColorPickerView>(R.id.colorPickerWheel)
        colorPickerWheel.setOnTouchListener { _, event ->
            if (event.action == MotionEvent.ACTION_MOVE || event.action == MotionEvent.ACTION_DOWN) {
                selectedColor = colorPickerWheel.getColorAtPosition(event.x, event.y)
                updateColorPreview(selectedColor) // Update the preview with the selected color
                sendCommand("COLOR:${selectedColor.toString()}") // Sende die ausgewählte Farbe an dein Gerät
            }
            true
        }

        // Navigation zu MainActivity
        val buttonNavigateCar: ImageButton = findViewById(R.id.nav_car_Button)
        buttonNavigateCar.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // Set up navigation button für MapActivity
        val buttonNavigateMap: ImageButton = findViewById(R.id.nav_map_Button)
        buttonNavigateMap.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = arrayOf(
            Manifest.permission.BLUETOOTH,
            Manifest.permission.BLUETOOTH_ADMIN,
            Manifest.permission.ACCESS_FINE_LOCATION
        )

        // Berechtigungen überprüfen
        val permissionsNeeded = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (permissionsNeeded.isNotEmpty()) {
            // Berechtigungen anfordern, wenn sie fehlen
            ActivityCompat.requestPermissions(this, permissionsNeeded.toTypedArray(), REQUEST_CODE_PERMISSIONS)
        } else {
            // Berechtigungen sind bereits erteilt
            startBluetoothDiscovery()
        }
    }

    private fun startBluetoothDiscovery() {
        try {
            val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            if (bluetoothAdapter.isEnabled) {
                Log.d("Bluetooth", "Bluetooth ist aktiviert")
                bluetoothAdapter.startDiscovery()
            } else {
                Log.e("Bluetooth", "Bluetooth ist nicht aktiviert")
                Toast.makeText(this, "Bitte Bluetooth aktivieren", Toast.LENGTH_SHORT).show()
            }
        } catch (e: SecurityException) {
            Log.e("Bluetooth", "Fehler beim Starten der Discovery: ${e.message}")
            Toast.makeText(this, "Berechtigungen erforderlich, um Bluetooth zu verwenden", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendCommand(command: String) {
        // Hier die Logik zum Senden von Befehlen an das LED-Gerät einfügen
        bluetoothController.sendCommand(command) // Verwende die sendCommand-Methode des BluetoothController
    }

    private fun updateColorPreview(color: Int) {
        val colorPreview = findViewById<View>(R.id.colorPreview)
        colorPreview.setBackgroundColor(color) // Setze die Hintergrundfarbe des Vorschau-Views
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startBluetoothDiscovery() // Starte die Bluetooth-Discovery, wenn die Berechtigungen erteilt sind
            } else {
                Log.e("Bluetooth", "Berechtigungen wurden abgelehnt")
                Toast.makeText(this, "Berechtigungen erforderlich, um Bluetooth zu verwenden", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        // Überprüfe Berechtigungen, bevor die Discovery gestoppt wird
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED) {
            val bluetoothAdapter = (getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
            bluetoothAdapter.cancelDiscovery() // Discovery stoppen, wenn die Berechtigung erteilt wurde
        } else {
            Log.e("Bluetooth", "Berechtigung zum Stoppen der Discovery wurde abgelehnt")
            Toast.makeText(this, "Berechtigung erforderlich, um Discovery zu stoppen", Toast.LENGTH_SHORT).show()
        }
    }
}
