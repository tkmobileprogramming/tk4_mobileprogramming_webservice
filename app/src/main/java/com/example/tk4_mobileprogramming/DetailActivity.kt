package com.example.tk4_mobileprogramming

import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.google.android.material.textfield.TextInputEditText
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount


class DetailActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var dbHelper: DatabaseHelper
    private lateinit var map: GoogleMap
    private lateinit var placesClient: PlacesClient

    // View references
    private lateinit var nameInput: TextInputEditText
    private lateinit var ageInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText
    private lateinit var symptomsInput: TextInputEditText

    private var surveyId: Long = -1
    private var currentLat: Double = 0.0
    private var currentLong: Double = 0.0
    private var survey: Survey? = null

    private lateinit var googleSignInAccount: GoogleSignInAccount

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

        // Set up toolbar
        val toolbar = findViewById<com.google.android.material.appbar.MaterialToolbar>(R.id.topAppBar)
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        supportActionBar?.setDisplayShowHomeEnabled(true)

        // Initialize Places API
        if (!Places.isInitialized()) {
            Places.initialize(applicationContext, getString(R.string.google_maps_key))
        }
        placesClient = Places.createClient(this)

        // Initialize view references
        nameInput = findViewById(R.id.nameEditText)
        ageInput = findViewById(R.id.ageEditText)
        addressInput = findViewById(R.id.addressEditText)
        symptomsInput = findViewById(R.id.symptomsEditText)

        // Initialize database helper
        dbHelper = DatabaseHelper(this)

        // Get survey ID from intent
        surveyId = intent.getLongExtra("SURVEY_ID", -1)
        if (surveyId == -1L) {
            Toast.makeText(this, "Error: Survey not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Load survey data with email
        try {
            val userEmail = GoogleSignIn.getLastSignedInAccount(this)?.email
            if (userEmail == null) {
                Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                finish()
                return
            }
            survey = dbHelper.getSurvey(surveyId, userEmail)
            loadFormData()
        } catch (e: IllegalArgumentException) {
            Toast.makeText(this, "Error: Survey not found", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

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

        // Set up update button click listener
        findViewById<com.google.android.material.button.MaterialButton>(R.id.modifySurveyButton)
            .setOnClickListener {
                updateSurveyData()
            }

        // Set up delete button click listener
        findViewById<com.google.android.material.button.MaterialButton>(R.id.deleteButton)
            .setOnClickListener {
                deleteSurvey()
            }
    }

    private fun loadFormData() {
        survey?.let { survey ->
            nameInput.setText(survey.name)
            ageInput.setText(survey.age.toString())
            addressInput.setText(survey.address)
            symptomsInput.setText(survey.symptoms)

            // Store the coordinates
            if (survey.latitude != null && survey.longitude != null) {
                currentLat = survey.latitude
                currentLong = survey.longitude
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
                    currentLat = latLng.latitude
                    currentLong = latLng.longitude
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
            currentLat = latLng.latitude
            currentLong = latLng.longitude
            map.clear()
            map.addMarker(MarkerOptions().position(latLng))
        }

        // Show existing location if available
        survey?.let { survey ->
            if (survey.latitude != null && survey.longitude != null) {
                val location = LatLng(survey.latitude, survey.longitude)
                map.apply {
                    clear()
                    addMarker(MarkerOptions().position(location))
                    moveCamera(CameraUpdateFactory.newLatLngZoom(location, 15f))
                }
            }
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        onBackPressed()
        return true
    }

    private fun deleteSurvey() {
        // Show confirmation dialog first
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Delete")
            .setMessage(getString(R.string.confirm_delete))
            .setPositiveButton(getString(R.string.yes)) { _, _ ->
                // Get the current user's email
                val userEmail = com.google.android.gms.auth.api.signin.GoogleSignIn.getLastSignedInAccount(this)?.email

                if (userEmail != null) {
                    val deleted = dbHelper.deleteSurveyByIdAndEmail(surveyId, userEmail)
                    if (deleted > 0) {
                        Toast.makeText(this, "Survey deleted successfully", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Error deleting survey", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this, "Error: User not authenticated", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
            .setNegativeButton(getString(R.string.no), null)
            .show()
    }

    private fun updateSurveyData() {
        val name = nameInput.text.toString()
        val age = ageInput.text.toString().toIntOrNull() ?: 0
        val address = addressInput.text.toString()
        val symptoms = symptomsInput.text.toString()

        // Validate inputs
        if (name.isEmpty() || address.isEmpty() || symptoms.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show()
            return
        }

        // Show confirmation dialog
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Confirm Update")
            .setMessage("Are you sure you want to update this survey?")
            .setPositiveButton("Yes") { _, _ ->
                // Create updated survey object
                val updatedSurvey = Survey(
                    id = surveyId,
                    name = name,
                    age = age,
                    address = address,
                    symptoms = symptoms,
                    latitude = currentLat,
                    longitude = currentLong,
                    surveyorEmail = survey?.surveyorEmail // Maintain existing value
                )

                // Update in database
                val rowsAffected = dbHelper.updateSurvey(updatedSurvey)
                if (rowsAffected > 0) {
                    Toast.makeText(this, "Survey updated successfully", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Error updating survey", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("No", null)
            .show()
    }
}