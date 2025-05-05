package com.example.myapplication

import android.app.Application
import android.util.Log

class MainApplication : Application() {
    private val TAG = "SpeechRecognitionApp"

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Application started")
    }

}