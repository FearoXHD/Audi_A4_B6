package com.example.audia4b6

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.MapView
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.CameraPosition


class MapActivity : AppCompatActivity(), OnMapReadyCallback {

    private lateinit var mapView: MapView
    private var googleMap: GoogleMap? = null
    private lateinit var fusedLocationProviderClient: FusedLocationProviderClient
    private val locationRequest: LocationRequest = LocationRequest.create().apply {
        interval = 1000
        fastestInterval = 500
        priority = LocationRequest.PRIORITY_HIGH_ACCURACY
    }
    private var shouldCenterMap = false // Flag to control map centering

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_map)

        // Initialize map view
        mapView = findViewById(R.id.mapView)
        mapView.onCreate(savedInstanceState)
        mapView.getMapAsync(this)

        // Initialize location services
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this)

        // Set up navigation button
        val buttonNavigate: ImageButton = findViewById(R.id.nav_car_Button)
        buttonNavigate.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }
    }

    override fun onMapReady(map: GoogleMap) {
        googleMap = map

        // Check for permissions and request location updates
        if (ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(
                this, Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.ACCESS_FINE_LOCATION), 1
            )
            return
        }
        googleMap?.isMyLocationEnabled = true

        fusedLocationProviderClient.requestLocationUpdates(
            locationRequest, locationCallback, Looper.getMainLooper()
        )
    }

    private val locationCallback = object : LocationCallback() {
        override fun onLocationResult(locationResult: LocationResult) {
            super.onLocationResult(locationResult)
            val location: Location? = locationResult.lastLocation
            if (location != null) {
                val latLng = LatLng(location.latitude, location.longitude)
                if (shouldCenterMap) {
                    googleMap?.apply {
                        val currentCameraPosition = cameraPosition
                        val newCameraPosition = CameraPosition.Builder()
                            .target(latLng)
                            .zoom(currentCameraPosition.zoom)
                            .build()
                        moveCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
                    }
                }
            }
        }
    }

    // Handle permissions request result
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 1) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                googleMap?.let { onMapReady(it) }
            } else {
                // Handle permission denial
            }
        }
    }

    // Method to center map on the current location
    fun centerMapOnLocation(location: Location) {
        shouldCenterMap = true
        val latLng = LatLng(location.latitude, location.longitude)
        googleMap?.apply {
            val currentCameraPosition = cameraPosition
            val newCameraPosition = CameraPosition.Builder()
                .target(latLng)
                .zoom(currentCameraPosition.zoom)
                .build()
            moveCamera(CameraUpdateFactory.newCameraPosition(newCameraPosition))
        }
    }

    // Method to stop centering the map
    fun stopCenteringMap() {
        shouldCenterMap = false
    }


    override fun onResume() {
        super.onResume()
        mapView.onResume()
    }

    override fun onPause() {
        super.onPause()
        mapView.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mapView.onDestroy()
    }

    override fun onLowMemory() {
        super.onLowMemory()
        mapView.onLowMemory()
    }
}
