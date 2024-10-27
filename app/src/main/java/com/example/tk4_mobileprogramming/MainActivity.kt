package com.example.tk4_mobileprogramming

import android.Manifest
import android.content.Intent
import android.content.IntentSender
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.os.Looper
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

import androidx.core.app.ActivityCompat
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.gms.common.api.ResolvableApiException
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.gms.tasks.Task
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: SurveyAdapter
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var surveyResultsText: TextView
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationCallback: LocationCallback
    private lateinit var locationRequest: LocationRequest
    private var map: GoogleMap? = null
    private var isMapReady = false
    private var isInitialLocationSet = false
    private var userInitiatedMovement = false
    private var currentLocation: Location? = null
    private lateinit var googleSignInAccount: GoogleSignInAccount

    companion object {
        private const val REQUEST_CHECK_SETTINGS = 1001
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) -> {
                // Precise location access granted
                checkLocationSettingsAndStartUpdates()
            }
            permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false) -> {
                // Only approximate location access granted
                checkLocationSettingsAndStartUpdates()
            }
            else -> {
                // No location access granted
                Toast.makeText(
                    this,
                    "Location permission is required to show your current location",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Get signed in account
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
            ?: run {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)

        dbHelper = DatabaseHelper(this)
        setupLocationServices()

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)

        surveyResultsText = findViewById(R.id.surveyResultsText)

        val fab: FloatingActionButton = findViewById(R.id.fab)
        fab.setOnClickListener {
            val intent = Intent(this, AddDataActivity::class.java)
            startActivity(intent)
        }

        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        loadData()
        checkLocationPermission()
    }

    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)

        locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 10000)
            .setWaitForAccurateLocation(false)
            .setMinUpdateIntervalMillis(5000)
            .setMaxUpdateDelayMillis(15000)
            .build()

        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    currentLocation = location
                    if (isMapReady) {
                        showSurveyLocations()
                    }
                }
            }
        }
    }

    private fun checkLocationSettingsAndStartUpdates() {
        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true) // This makes the dialog always show

        val client: SettingsClient = LocationServices.getSettingsClient(this)
        val task: Task<LocationSettingsResponse> = client.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            startLocationUpdates()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                try {
                    exception.startResolutionForResult(
                        this@MainActivity,
                        REQUEST_CHECK_SETTINGS
                    )
                } catch (sendEx: IntentSender.SendIntentException) {
                    // Ignore the error.
                }
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CHECK_SETTINGS) {
            when (resultCode) {
                RESULT_OK -> {
                    startLocationUpdates()
                }
                RESULT_CANCELED -> {
                    Toast.makeText(
                        this,
                        "Location services must be enabled to show your current location",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

    private fun updateMapLocation(location: Location) {
        if (!userInitiatedMovement && !isInitialLocationSet) {
            val currentLatLng = LatLng(location.latitude, location.longitude)
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
            isInitialLocationSet = true
        }
    }

    private fun checkLocationPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                checkLocationSettingsAndStartUpdates()
            }
            shouldShowRequestPermissionRationale(Manifest.permission.ACCESS_FINE_LOCATION) -> {
                Toast.makeText(
                    this,
                    "Location permission is required to show your current location",
                    Toast.LENGTH_LONG
                ).show()
                requestLocationPermission()
            }
            else -> {
                requestLocationPermission()
            }
        }
    }

    private fun requestLocationPermission() {
        requestPermissionLauncher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return
        }

        fusedLocationClient.requestLocationUpdates(
            locationRequest,
            locationCallback,
            Looper.getMainLooper()
        )

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location ->
                location?.let {
                    currentLocation = it
                    if (isMapReady) {
                        showSurveyLocations()
                    }
                }
            }
    }

    private fun stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }

    override fun onResume() {
        super.onResume()
        loadData()
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED) {
            checkLocationSettingsAndStartUpdates()
        }
        userInitiatedMovement = false
        isInitialLocationSet = false
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        isMapReady = true

        map?.uiSettings?.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
            isMyLocationButtonEnabled = true
        }

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            map?.isMyLocationEnabled = true
        }

        // Add camera movement listener
        map?.setOnCameraMoveStartedListener { reason ->
            if (reason == GoogleMap.OnCameraMoveStartedListener.REASON_GESTURE) {
                userInitiatedMovement = true
            }
        }

        showSurveyLocations()
    }

    private fun loadData() {
        val data = dbHelper.getAllSurveysByEmail(googleSignInAccount.email ?: "")
        adapter = SurveyAdapter(data) { survey ->
            val intent = Intent(this, DetailActivity::class.java)
            intent.putExtra("SURVEY_ID", survey.id)
            startActivity(intent)
        }
        recyclerView.adapter = adapter

        if (data.isNotEmpty()) {
            surveyResultsText.visibility = View.VISIBLE
        } else {
            surveyResultsText.visibility = View.GONE
        }

        if (isMapReady) {
            showSurveyLocations()
        }
    }

    override fun onPause() {
        super.onPause()
        stopLocationUpdates()
    }

    private fun showSurveyLocations() {
        map?.clear()
        val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email ?: return
        val data = dbHelper.getAllSurveysByEmail(userEmail)
        val markers = mutableListOf<LatLng>()

        data.forEach { survey ->
            if (survey.latitude != null && survey.longitude != null) {
                val position = LatLng(survey.latitude, survey.longitude)
                markers.add(position)
                map?.addMarker(MarkerOptions()
                    .position(position)
                    .title(survey.name)
                    .snippet(survey.symptoms))
            }
        }

        if (markers.isNotEmpty() || currentLocation != null) {
            val builder = LatLngBounds.Builder()

            markers.forEach { builder.include(it) }

            currentLocation?.let { location ->
                val currentLatLng = LatLng(location.latitude, location.longitude)
                builder.include(currentLatLng)
            }

            try {
                val bounds = builder.build()
                val padding = resources.getDimensionPixelSize(R.dimen.map_padding)

                map?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                userInitiatedMovement = true
            } catch (e: Exception) {
                map?.setOnMapLoadedCallback {
                    try {
                        val bounds = builder.build()
                        val padding = resources.getDimensionPixelSize(R.dimen.map_padding)
                        map?.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, padding))
                        userInitiatedMovement = true
                    } catch (e: IllegalStateException) {
                        currentLocation?.let { location ->
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(currentLatLng, 15f))
                        }
                    }
                }
            }
        } else {
            val defaultLocation = LatLng(-6.2088, 106.8456)
            map?.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
        }
    }

}