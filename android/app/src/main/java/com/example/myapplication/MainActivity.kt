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
    private var isListening = false
    private var continuousListening = true
    private var allResults = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resultTextView = findViewById(R.id.resultTextView)
        partialResultTextView = findViewById(R.id.partialResultTextView)

        // Initialize speech recognizer
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
        speechRecognizer.setRecognitionListener(this)

        startButton.setOnClickListener {
            checkPermissionAndStartListening()
        }

        stopButton.setOnClickListener {
            continuousListening = false
            stopListening()
        }

        updateButtonState()
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
            continuousListening = true
            allResults.clear() // Clear previous results when starting a new session
            resultTextView.text = ""
            startListening()
        }
    }

    private fun startListening() {
        if (isListening) {
            return // Don't start if already listening
        }
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            
            // Enable dictation mode for continuous speech recognition
            putExtra("android.speech.extra.DICTATION_MODE", true)
            
            // Set longer timeout for dictation sessions
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 5000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 3000)
            
            // Receive multiple results
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
        }

        try {
            partialResultTextView.text = ""
            speechRecognizer.startListening(intent)
            isListening = true
            updateButtonState()
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            Toast.makeText(this, "Error starting speech recognition", Toast.LENGTH_SHORT).show()
        }
    }

    private fun restartListeningWithDelay() {
        stopListening()
        // Add a delay before restarting to prevent client-side errors
        handler.postDelayed({
            if (continuousListening) {
                startListening()
            }
        }, 500) // 500ms delay for dictation mode
    }

    private fun stopListening() {
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
                startListening()
            } else {
                Toast.makeText(this, "Permission denied for speech recognition", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        continuousListening = false
        handler.removeCallbacksAndMessages(null) // Remove any pending callbacks
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
        // Optionally, you could use this to show a volume indicator
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Not used
    }

    override fun onEndOfSpeech() {
        Log.d(TAG, "onEndOfSpeech")
        
        // In dictation mode, this is called when there's a pause
        // So we should restart listening to continue the dictation
        if (continuousListening) {
            restartListeningWithDelay()
        } else {
            isListening = false
            updateButtonState()
        }
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
        
        // Some errors are normal in continuous mode and should be handled by restarting
        if (continuousListening && 
           (error == SpeechRecognizer.ERROR_NO_MATCH ||
            error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ||
            error == SpeechRecognizer.ERROR_CLIENT ||
            error == SpeechRecognizer.ERROR_RECOGNIZER_BUSY)) {
            
            Log.d(TAG, "Restarting after recoverable error")
            isListening = false
            handler.postDelayed({
                if (continuousListening) {
                    startListening()
                }
            }, 1000) // Use a longer delay for error recovery
        } else {
            // For more serious errors, notify the user
            Toast.makeText(this, "Error: $errorMessage", Toast.LENGTH_SHORT).show()
            isListening = false
            updateButtonState()
        }
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.isNotEmpty()) {
            val text = matches[0]
            
            // Append to all results with a space
            if (allResults.isNotEmpty() && !text.startsWith(" ")) {
                allResults.append(" ")
            }
            allResults.append(text)
            
            // Show full transcription in the result text view
            resultTextView.text = allResults.toString()
            Log.d(TAG, "onResults: $text")
            
            // In dictation mode, we'll get results as phrases complete
            // but we want to keep listening for the next phrase
            if (continuousListening) {
                restartListeningWithDelay()
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
        // This callback is for future Android versions that might add events
        when (eventType) {
            // Handle different event types if needed
            else -> Log.d(TAG, "onEvent: $eventType")
        }
    }
}