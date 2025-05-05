package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : AppCompatActivity(), RecognitionListener {
    private val TAG = "SpeechRecognitionDemo"
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1

    private lateinit var speechRecognizer: SpeechRecognizer
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var partialResultTextView: TextView
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var radioEnglish: RadioButton
    private lateinit var radioSpanish: RadioButton
    
    private var isListening = false
    private var keepRecognizing = false // Flag to control continuous recognition
    private var allResults = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    
    // Language settings
    private var selectedLanguage = "en-US" // Default to English

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resultTextView = findViewById(R.id.resultTextView)
        partialResultTextView = findViewById(R.id.partialResultTextView)
        languageRadioGroup = findViewById(R.id.languageRadioGroup)
        radioEnglish = findViewById(R.id.radioEnglish)
        radioSpanish = findViewById(R.id.radioSpanish)

        // Initialize speech recognizer
        initializeSpeechRecognizer()

        // Set up language selection listener
        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioEnglish -> {
                    selectedLanguage = "en-US"
                    updateLabelsForLanguage(isEnglish = true)
                }
                R.id.radioSpanish -> {
                    selectedLanguage = "es-ES"
                    updateLabelsForLanguage(isEnglish = false)
                }
            }
            
            // If we're already listening, we need to restart with the new language
            if (isListening) {
                stopRecognition()
                handler.postDelayed({
                    startRecognition()
                }, 200)
            }
        }

        startButton.setOnClickListener {
            checkPermissionAndStartListening()
        }

        stopButton.setOnClickListener {
            keepRecognizing = false
            stopRecognition()
        }

        updateButtonState()
        updateLabelsForLanguage(radioEnglish.isChecked)
    }
    
    private fun initializeSpeechRecognizer() {
        if (::speechRecognizer.isInitialized) {
            speechRecognizer.destroy()
        }
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)
    }
    
    private fun updateLabelsForLanguage(isEnglish: Boolean) {
        if (isEnglish) {
            startButton.text = "Start Listening"
            stopButton.text = "Stop Listening"
            findViewById<TextView>(R.id.partialLabelTextView).text = "Listening:"
            findViewById<TextView>(R.id.resultLabelTextView).text = "Final Result:"
        } else {
            startButton.text = "Comenzar a Escuchar"
            stopButton.text = "Dejar de Escuchar"
            findViewById<TextView>(R.id.partialLabelTextView).text = "Escuchando:"
            findViewById<TextView>(R.id.resultLabelTextView).text = "Resultado Final:"
        }
    }

    private fun checkPermissionAndStartListening() {
        val permission = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
        if (permission != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.RECORD_AUDIO),
                RECORD_AUDIO_PERMISSION_REQUEST
            )
        } else {
            allResults.clear() // Clear previous results when starting a new session
            resultTextView.text = ""
            partialResultTextView.text = ""
            keepRecognizing = true
            startRecognition()
        }
    }
    
    private fun createRecognizerIntent(): Intent {
        return Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, selectedLanguage)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            // Add dictation mode for better continuous recognition
            putExtra("android.speech.extra.DICTATION_MODE", true)
        }
    }

    private fun startRecognition() {
        if (!keepRecognizing) {
            return // Don't start if we're not supposed to keep recognizing
        }
        
        if (isListening) {
            // Already listening, don't start again
            return
        }
        
        try {
            Log.d(TAG, "Starting speech recognition")
            speechRecognizer.startListening(createRecognizerIntent())
            isListening = true
            updateButtonState()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            Toast.makeText(this, "Error starting speech recognition", Toast.LENGTH_SHORT).show()
            isListening = false
            updateButtonState()
        }
    }

    private fun stopRecognition() {
        if (isListening) {
            try {
                speechRecognizer.stopListening()
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping speech recognition: ${e.message}")
            }
            isListening = false
            updateButtonState()
        }
    }

    private fun updateButtonState() {
        startButton.isEnabled = !isListening
        stopButton.isEnabled = isListening
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                keepRecognizing = true
                startRecognition()
            } else {
                val message = if (selectedLanguage == "es-ES") {
                    "Permiso denegado para el reconocimiento de voz"
                } else {
                    "Permission denied for speech recognition"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        // Stop recognition when app goes to background
        keepRecognizing = false
        stopRecognition()
    }

    override fun onDestroy() {
        super.onDestroy()
        keepRecognizing = false
        handler.removeCallbacksAndMessages(null)
        speechRecognizer.destroy()
    }

    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "onReadyForSpeech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "onBeginningOfSpeech")
    }

    override fun onRmsChanged(rmsdB: Float) {
        // Could be used to show a volume indicator
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Not used
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        isListening = false
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
        
        Log.e(TAG, "onError: $errorMessage (code: $error)")
        isListening = false
        updateButtonState()
        
        // For common errors, restart the recognition automatically if we're supposed to keep going
        if (keepRecognizing && (error == SpeechRecognizer.ERROR_NO_MATCH || 
                          error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT)) {
            Log.d(TAG, "Restarting recognition after error")
            handler.postDelayed({
                startRecognition()
            }, 300)
        } else if (error != SpeechRecognizer.ERROR_CLIENT) {
            // For other errors except client errors (which happen during normal operation), show a message
            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val text = matches[0]
            Log.d(TAG, "onResults: $text")
            
            // Append to all results with a space
            if (allResults.isNotEmpty() && !text.startsWith(" ")) {
                allResults.append(" ")
            }
            allResults.append(text)
            
            // Show full transcription in the result text view
            resultTextView.text = allResults.toString()
            
            // Continue listening if we're supposed to keep recognizing
            isListening = false
            updateButtonState()
            
            if (keepRecognizing) {
                Log.d(TAG, "Restarting recognition after results")
                handler.postDelayed({
                    startRecognition()
                }, 300)
            }
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val text = matches[0]
            partialResultTextView.text = text
            Log.d(TAG, "onPartialResults: $text")
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        Log.d(TAG, "onEvent: $eventType")
    }
}