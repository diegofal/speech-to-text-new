package com.example.myapplication

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.example.myapplication.speech.SpeechRecognitionListener
import com.example.myapplication.speech.SpeechRecognizer
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup

class MainActivity : AppCompatActivity(), SpeechRecognitionListener {
    private val TAG = "SpeechTranscriber"
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1

    // UI components
    private lateinit var resultTextView: TextView
    private lateinit var resultScrollView: ScrollView
    private lateinit var recognizerToggleGroup: MaterialButtonToggleGroup
    private lateinit var btnStartStop: MaterialButton
    
    // Speech recognition
    private var currentRecognizer: SpeechRecognizer? = null
    private var isListening = false
    
    // Results tracking
    private var lastPartialResult: String = ""
    private var lastFinalResult: String = ""
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        try {
            initializeUI()
            
            // Only initialize recognizer if we have permission
            if (checkPermission()) {
                initializeRecognizer()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing app", e)
            Toast.makeText(this, "Error initializing app: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun initializeUI() {
        resultTextView = findViewById(R.id.tvResult)
        resultScrollView = findViewById(R.id.resultScrollView)
        recognizerToggleGroup = findViewById(R.id.recognizerToggleGroup)
        btnStartStop = findViewById(R.id.btnStartStop)
        
        // Set up recognizer toggle group
        recognizerToggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                try {
                    stopListening() // Stop current recognition if running
                    currentRecognizer?.destroy()
                    currentRecognizer = when (checkedId) {
                        R.id.btnVosk -> SpeechRecognizer.createVoskRecognizer(this)
                        R.id.btnWhisper -> SpeechRecognizer.createWhisperRecognizer(this)
                        else -> null
                    }
                    currentRecognizer?.setListener(this)
                } catch (e: Exception) {
                    Log.e(TAG, "Error switching recognizer", e)
                    Toast.makeText(this, "Error switching recognizer: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Set initial recognizer
        recognizerToggleGroup.check(R.id.btnVosk)
        
        // Set up start/stop button
        btnStartStop.setOnClickListener {
            if (checkPermission()) {
                if (isListening) {
                    stopListening()
                } else {
                    startListening()
                }
            } else {
                requestPermission()
            }
        }
    }

    private fun initializeRecognizer() {
        try {
            currentRecognizer = SpeechRecognizer.createVoskRecognizer(this)
            currentRecognizer?.setListener(this)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing Vosk recognizer", e)
            Toast.makeText(this, "Error initializing speech recognition: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun startListening() {
        if (!isListening) {
            try {
                lastPartialResult = ""
                lastFinalResult = ""
                resultTextView.text = ""
                currentRecognizer?.startListening("en-US") // Default to English
                isListening = true
                updateUI()
            } catch (e: Exception) {
                Log.e(TAG, "Error starting recognition", e)
                Toast.makeText(this, "Error starting recognition: ${e.message}", Toast.LENGTH_SHORT).show()
                isListening = false
                updateUI()
            }
        }
    }
    
    private fun stopListening() {
        if (isListening) {
            try {
                currentRecognizer?.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recognition", e)
            } finally {
                isListening = false
                updateUI()
            }
        }
    }
    
    private fun updateUI() {
        try {
            btnStartStop.apply {
                text = if (isListening) "Stop Listening" else "Start Listening"
                setIconResource(if (isListening) R.drawable.ic_mic_off else R.drawable.ic_mic)
            }
            recognizerToggleGroup.isEnabled = !isListening
        } catch (e: Exception) {
            Log.e(TAG, "Error updating UI", e)
        }
    }

    private fun checkPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.RECORD_AUDIO
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    private fun requestPermission() {
        requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_REQUEST
        )
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initializeRecognizer()
                startListening()
            } else {
                Toast.makeText(this, "Permission denied. Cannot perform speech recognition.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        try {
            currentRecognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying recognizer", e)
        }
    }
    
    // SpeechRecognitionListener implementation
    override fun onRecognitionStarted() {
        Log.d(TAG, "Recognition started")
    }
    
    override fun onPartialResult(text: String) {
        Log.d(TAG, "Partial result: $text")
        if (text == lastPartialResult) return
        
        runOnUiThread {
            try {
                lastPartialResult = text
                // Replace the last line if it was a partial result
                val currentText = resultTextView.text.toString()
                val lines = currentText.split("\n")
                if (lines.isNotEmpty() && lines.last().startsWith("_")) {
                    // Remove the last line (partial result)
                    resultTextView.text = lines.dropLast(1).joinToString("\n")
                }
                // Add the new partial result with an underscore prefix
                resultTextView.append("\n_$text")
                resultScrollView.post {
                    resultScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating partial result", e)
            }
        }
    }
    
    override fun onResult(text: String) {
        Log.d(TAG, "Final result: $text")
        if (text == lastFinalResult) return
        
        runOnUiThread {
            try {
                lastFinalResult = text
                // Remove the last line if it was a partial result
                val currentText = resultTextView.text.toString()
                val lines = currentText.split("\n")
                if (lines.isNotEmpty() && lines.last().startsWith("_")) {
                    resultTextView.text = lines.dropLast(1).joinToString("\n")
                }
                // Add the final result
                if (resultTextView.text.isNotEmpty()) {
                    resultTextView.append("\n")
                }
                resultTextView.append(text)
                resultScrollView.post {
                    resultScrollView.fullScroll(ScrollView.FOCUS_DOWN)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error updating final result", e)
            }
        }
    }
    
    override fun onError(errorMessage: String) {
        Log.e(TAG, "Recognition error: $errorMessage")
        runOnUiThread {
            try {
                Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error showing error message", e)
            }
        }
    }
}