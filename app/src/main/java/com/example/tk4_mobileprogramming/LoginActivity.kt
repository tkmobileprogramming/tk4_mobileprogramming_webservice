package com.example.tk4_mobileprogramming

import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.snackbar.Snackbar
import android.os.Handler
import android.os.Looper
import androidx.core.content.ContextCompat

class LoginActivity : AppCompatActivity() {
    private lateinit var googleSignInClient: GoogleSignInClient
    private val WEB_CLIENT_ID = "1084289998497-fjekooto9f62jkb2hdkp00u75oafoa70.apps.googleusercontent.com"

    private val signInLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)

            // Create a standard Material Design Snackbar that appears at the bottom
            Snackbar.make(
                findViewById(android.R.id.content), // Use the root view instead of button
                "Welcome ${account.displayName}",
                Snackbar.LENGTH_SHORT
            ).apply {
                // Set animation mode
                animationMode = Snackbar.ANIMATION_MODE_SLIDE

                // Show the snackbar
                show()
            }

            // Delay the activity transition
            Handler(Looper.getMainLooper()).postDelayed({
                startActivity(Intent(this, MainActivity::class.java))
                finish()
            }, 1500)

        } catch (e: Exception) {
            Log.e("LoginActivity", "Sign in failed", e)
            Snackbar.make(
                findViewById(android.R.id.content),
                "Sign in failed",
                Snackbar.LENGTH_SHORT
            ).apply {
                setBackgroundTint(ContextCompat.getColor(context, R.color.error_light))
                setTextColor(ContextCompat.getColor(context, R.color.white))
                animationMode = Snackbar.ANIMATION_MODE_SLIDE
                show()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        setupGoogleSignIn()
        checkExistingAccount()
        setupSignInButton()
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .requestProfile()
            .requestScopes(Scope("openid"))
            .requestId()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)
        googleSignInClient.signOut() // Clear any existing sessions
    }

    private fun checkExistingAccount() {
        val account = GoogleSignIn.getLastSignedInAccount(this)
        if (account != null && !account.isExpired) {
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }
    }

    private fun setupSignInButton() {
        findViewById<MaterialButton>(R.id.signInButton).setOnClickListener {
            signInLauncher.launch(googleSignInClient.signInIntent)
        }
    }

    override fun onResume() {
        super.onResume()
        checkGooglePlayServices()
    }

    private fun checkGooglePlayServices() {
        val googleApiAvailability = com.google.android.gms.common.GoogleApiAvailability.getInstance()
        val resultCode = googleApiAvailability.isGooglePlayServicesAvailable(this)
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            googleApiAvailability.getErrorDialog(this, resultCode, 1)?.show()
        }
    }
}