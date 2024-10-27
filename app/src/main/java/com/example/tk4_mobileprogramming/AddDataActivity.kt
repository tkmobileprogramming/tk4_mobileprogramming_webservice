package com.example.tk4_mobileprogramming

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.libraries.places.api.Places
import com.google.android.libraries.places.api.model.Place
import com.google.android.libraries.places.api.net.FetchPlaceRequest
import com.google.android.libraries.places.api.net.FindAutocompletePredictionsRequest
import com.google.android.libraries.places.api.net.PlacesClient
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AddDataActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var map: GoogleMap
    private var selectedLocation: LatLng? = null
    private lateinit var addressInput: TextInputEditText
    private lateinit var placesClient: PlacesClient
    private lateinit var nameInput: TextInputEditText
    private lateinit var ageInput: TextInputEditText
    private lateinit var symptomsInput: TextInputEditText
    private lateinit var googleSignInAccount: GoogleSignInAccount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_data)

        // Get signed in account
        googleSignInAccount = GoogleSignIn.getLastSignedInAccount(this)
            ?: run {
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
                return
            }

        val toolbar: MaterialToolbar = findViewById(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize Places
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        dbHelper = DatabaseHelper(this)

        // Initialize views
        nameInput = findViewById(R.id.nameInput)
        ageInput = findViewById(R.id.ageInput)
        addressInput = findViewById(R.id.addressInput)
        symptomsInput = findViewById(R.id.symptomsInput)

        // Initialize map
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)

        // Set up address search
        addressInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                searchAddress()
                true
            } else {
                false
            }
        }

        // Set up submit button
        val submitButton: MaterialButton = findViewById(R.id.submitButton)
        submitButton.setOnClickListener {
            val name = nameInput.text.toString()
            val age = ageInput.text.toString().toIntOrNull() ?: 0
            val address = addressInput.text.toString()
            val symptoms = symptomsInput.text.toString()

            if (name.isNotEmpty() && address.isNotEmpty() && symptoms.isNotEmpty()) {
                val survey = Survey(
                    0,
                    name,
                    age,
                    address,
                    symptoms,
                    selectedLocation?.latitude,
                    selectedLocation?.longitude,
                    googleSignInAccount.email
                )
                dbHelper.addSurvey(survey)
                Toast.makeText(this, "Data berhasil ditambahkan", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                Toast.makeText(this, "Mohon lengkapi semua data", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun searchAddress() {
        val address = addressInput.text.toString()
        val request = FindAutocompletePredictionsRequest.builder()
            .setQuery(address)
            .build()

        placesClient.findAutocompletePredictions(request)
            .addOnSuccessListener { response ->
                if (response.autocompletePredictions.isNotEmpty()) {
                    val prediction = response.autocompletePredictions[0]
                    fetchPlaceAndMoveCamera(prediction.placeId)
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Toast.makeText(
                        this,
                        "Place not found: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    private fun fetchPlaceAndMoveCamera(placeId: String) {
        val placeFields = listOf(Place.Field.LAT_LNG, Place.Field.ADDRESS)
        val request = FetchPlaceRequest.builder(placeId, placeFields).build()

        placesClient.fetchPlace(request)
            .addOnSuccessListener { response ->
                val place = response.place
                place.latLng?.let { latLng ->
                    selectedLocation = latLng
                    map.clear()
                    map.addMarker(MarkerOptions().position(latLng))
                    map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15f))
                    // Update address field with formatted address if available
                    place.address?.let { address ->
                        addressInput.setText(address)
                    }
                }
            }
            .addOnFailureListener { exception ->
                if (exception is ApiException) {
                    Toast.makeText(
                        this,
                        "Place details not found: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
    }

    override fun onMapReady(googleMap: GoogleMap) {
        map = googleMap
        map.uiSettings.apply {
            isZoomControlsEnabled = true
            isCompassEnabled = true
        }

        // Set up map click listener
        map.setOnMapClickListener { latLng ->
            selectedLocation = latLng
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
        }

        // Default location (e.g., Jakarta)
        val defaultLocation = LatLng(-6.2088, 106.8456)
        map.moveCamera(CameraUpdateFactory.newLatLngZoom(defaultLocation, 10f))
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }
}