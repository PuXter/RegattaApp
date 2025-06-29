package com.example.regattaapp

import android.app.Application
import com.google.firebase.FirebaseApp

class RegattaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}
