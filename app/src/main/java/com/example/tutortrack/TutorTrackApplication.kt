package com.example.tutortrack

import android.app.Application
import com.example.tutortrack.data.AppDatabase

class TutorTrackApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        // Initialize the database
        AppDatabase.getDatabase(this)
    }
} 