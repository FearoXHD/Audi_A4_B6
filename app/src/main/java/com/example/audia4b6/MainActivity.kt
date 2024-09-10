package com.example.audia4b6

import android.Manifest
import android.app.NotificationManager
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothGatt
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.companion.AssociationInfo
import android.companion.AssociationRequest
import android.companion.BluetoothDeviceFilter
import android.companion.CompanionDeviceManager
import android.content.Intent
import android.content.IntentSender
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.ImageButton
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import java.util.concurrent.Executor
import java.util.regex.Pattern

//TODO: Scan only while in app, save mac, try to connect to saved mac every sec or use some
//  system event
@Suppress("DEPRECATION")
class MainActivity : ComponentActivity() {

    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var sharedPreferences: SharedPreferences
    //private lateinit var deviceManager: CompanionDeviceManager

    private val deviceManager: CompanionDeviceManager by lazy {
        getSystemService(COMPANION_DEVICE_SERVICE) as CompanionDeviceManager
    }

    private val executor: Executor =  Executor { it.run() }

    private var bluetoothGatt: BluetoothGatt? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        setContentView(R.layout.activity_main)

        val buttonNavigate: ImageButton = findViewById(R.id.nav_map_Button)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, MapActivity::class.java)
            startActivity(intent)
        }


        sharedPreferences = getSharedPreferences("app_prefs", MODE_PRIVATE)

        // Initialize Bluetooth
        //val bluetoothManager = getSystemService(BLUETOOTH_SERVICE) as BluetoothManager
        //bluetoothAdapter = bluetoothManager.adapter

        Log.i("MyTag", "Requesting permissions")
        // Request necessary permissions
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.POST_NOTIFICATIONS
            ),
            1001
        )
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)} passing\n      in a {@link RequestMultiplePermissions} object for the {@link ActivityResultContract} and\n      handling the result in the {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == 1001) {
            // Check if all permissions are granted
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                // All permissions granted, log the message
                Log.i("MyTag", "Starting Scan")
                if (!isDeviceAssociated()) {
                    associateBle()
                } else {
                    // Device is already associated, handle accordingly
                }
            } else {
                // Handle the case where permissions are denied
                Log.i("MyTag", "Permissions not granted, scan cannot start")
            }
        }
    }

    private fun associateBle()
    {
        NotificationHelper.createNotificationChannel(this@MainActivity, "system", "System Notifications",
            "System Notifications", NotificationManager.IMPORTANCE_LOW)
        NotificationHelper.sendNotification(this@MainActivity, "system", "Started association", "Started scanning for BLE devices...",
            1, R.drawable.ic_launcher_foreground
        )

        val deviceFilter: BluetoothDeviceFilter = BluetoothDeviceFilter.Builder()
            // Match only Bluetooth devices whose name matches the pattern.
            .setNamePattern(Pattern.compile("DSD TECH"))
            .build()

        val pairingRequest: AssociationRequest = AssociationRequest.Builder()
            // Find only devices that match this request filter.
            .addDeviceFilter(deviceFilter)
            // Stop scanning as soon as one device matching the filter is found.
            .setSingleDevice(true)
            .build()

        deviceManager.associate(pairingRequest,
            executor,
            object : CompanionDeviceManager.Callback() {
                // Called when a device is found. Launch the IntentSender so the user
                // can select the device they want to pair with.
                override fun onAssociationPending(intentSender: IntentSender) {
                    NotificationHelper.sendNotification(this@MainActivity, "system", "Association Pending", "Association Pending...",
                        1,
                        R.drawable.ic_launcher_foreground
                    )

                    startIntentSenderForResult(intentSender, 0, null, 0, 0, 0)
                }

                override fun onAssociationCreated(associationInfo: AssociationInfo) {
                    // An association is created.
                    val associationId: Int = associationInfo.id
                    val macAddress: MacAddress? = associationInfo.deviceMacAddress
                    saveAssociationInfo(macAddress)
                    NotificationHelper.sendNotification(this@MainActivity, "system", "Successfully associated ($associationId)", "Successfully associated with $macAddress",
                        1,
                        R.drawable.ic_launcher_foreground
                    )
                }

                override fun onFailure(errorMessage: CharSequence?) {
                    // To handle the failure.
                    NotificationHelper.sendNotification(this@MainActivity, "system", "Association failed", "Failed ($errorMessage)",
                        1,
                        R.drawable.ic_launcher_foreground
                    )
                }

            })
    }

    @Deprecated("This method has been deprecated in favor of using the Activity Result API\n      which brings increased type safety via an {@link ActivityResultContract} and the prebuilt\n      contracts for common intents available in\n      {@link androidx.activity.result.contract.ActivityResultContracts}, provides hooks for\n      testing, and allow receiving results in separate, testable classes independent from your\n      activity. Use\n      {@link #registerForActivityResult(ActivityResultContract, ActivityResultCallback)}\n      with the appropriate {@link ActivityResultContract} and handling the result in the\n      {@link ActivityResultCallback#onActivityResult(Object) callback}.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        when (requestCode) {
            0 -> when(resultCode) {
                RESULT_OK -> {
                    // The user chose to pair the app with a Bluetooth device.
                    val deviceToPair: BluetoothDevice? =
                        data?.getParcelableExtra(CompanionDeviceManager.EXTRA_DEVICE)
                    deviceToPair?.let { device ->
                        if (ActivityCompat.checkSelfPermission(
                                this,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        //connectToDevice(device)
                        deviceManager.startObservingDevicePresence(device.address);
                        // Maintain continuous interaction with a paired device.
                    }
                }
            }
            else -> super.onActivityResult(requestCode, resultCode, data)
        }
    }

    private fun isDeviceAssociated(): Boolean {
        return sharedPreferences.getBoolean("device_associated", false)
    }

    private fun saveAssociationInfo(macAddress: MacAddress?) {
        val editor = sharedPreferences.edit()
        editor.putBoolean("device_associated", true)
        editor.putString("device_mac_address", macAddress.toString())
        editor.apply()
    }



    private fun startScanning() {
        val bluetoothLeScanner: BluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
        val leScanCallback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                val device: BluetoothDevice = result.device

                if (ActivityCompat.checkSelfPermission(
                        this@MainActivity,
                        Manifest.permission.BLUETOOTH_CONNECT
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    // TODO: Consider calling
                    //    ActivityCompat#requestPermissions
                    Log.i("MyTag", "No BT connect perms")
                    return
                }

                if (device.name != null && device.name == "HM-10") {
                    // Device found, connect to it
                    //connectToDevice(device)
                }
            }
        }
        bluetoothLeScanner.startScan(leScanCallback)
    }




    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }
}
