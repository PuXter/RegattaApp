package com.example.regattaapp

import android.Manifest
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import com.example.regattaapp.navigation.AppNavGraph
import com.example.regattaapp.ui.theme.RegattaAppTheme
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth

class MainActivity : ComponentActivity() {

    private val locationPermissionRequest = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
        if (!granted) {
            // Można pokazać komunikat – na razie zostawiamy pusto
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        FirebaseAuth.getInstance().signInAnonymously()

        super.onCreate(savedInstanceState)

        Firebase.auth.signInAnonymously()
            .addOnSuccessListener { result ->
                val uid = result.user?.uid
                Log.d("FirebaseAuth", "Zalogowano anonimowo: $uid")
            }
            .addOnFailureListener { e ->
                Log.e("FirebaseAuth", "Błąd logowania", e)
            }


        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )

        setContent {
            RegattaAppTheme {
                val navController = rememberNavController()
                AppNavGraph(navController)
            }
        }
    }
}
