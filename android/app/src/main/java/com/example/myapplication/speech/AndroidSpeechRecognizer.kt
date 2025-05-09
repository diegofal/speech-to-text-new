package com.example.myapplication.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Implementation of SpeechRecognizer that uses Android's built-in speech recognition
 */
class AndroidSpeechRecognizer(private val context: Context) : com.example.myapplication.speech.SpeechRecognizer {
    private val TAG = "AndroidRecognizer"
    private var listener: SpeechRecognitionListener? = null
    private var recognizer: SpeechRecognizer? = null
    private var isListening = false
    private var language = "en-US"
    private val handler = Handler(Looper.getMainLooper())
    private var continuousRecognition = false
    private var lastPartialResults = ""
    private var noMatchRetryCount = 0
    private val MAX_RETRY_ATTEMPTS = 3
    private var isDestroyed = false
    
    init {
        if (context is AppCompatActivity) {
            setupSpeechRecognizer()
        } else {
            Log.e(TAG, "Context must be an AppCompatActivity")
        }
    }
    
    private fun setupSpeechRecognizer() {
        if (isDestroyed) return
        
        try {
            if (SpeechRecognizer.isRecognitionAvailable(context)) {
                recognizer?.destroy() // Clean up any existing instance
                recognizer = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer?.setRecognitionListener(createRecognitionListener())
            } else {
                Log.e(TAG, "Speech recognition not available")
                listener?.onError("Speech recognition not available on this device")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error setting up speech recognizer: ${e.message}")
            listener?.onError("Failed to initialize speech recognition: ${e.message}")
        }
    }
    
    private fun createRecognitionListener() = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            noMatchRetryCount = 0
            isListening = true
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Beginning of speech")
            noMatchRetryCount = 0
        }

        override fun onRmsChanged(rmsdB: Float) {
            // Ignore audio level changes
        }

        override fun onBufferReceived(buffer: ByteArray?) {
            // Ignore raw audio buffer
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "End of speech")
            isListening = false
        }

        override fun onError(error: Int) {
            isListening = false
            val errorMessage = when (error) {
                SpeechRecognizer.ERROR_AUDIO -> {
                    recreateSpeechRecognizer()
                    "Audio recording error"
                }
                SpeechRecognizer.ERROR_CLIENT -> {
                    recreateSpeechRecognizer()
                    "Client side error"
                }
                SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
                SpeechRecognizer.ERROR_NETWORK -> {
                    recreateSpeechRecognizer()
                    "Network error"
                }
                SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> {
                    recreateSpeechRecognizer()
                    "Network timeout"
                }
                SpeechRecognizer.ERROR_NO_MATCH -> {
                    if (noMatchRetryCount < MAX_RETRY_ATTEMPTS) {
                        noMatchRetryCount++
                        Log.d(TAG, "No match found, retrying (attempt $noMatchRetryCount)")
                        handler.postDelayed({ startRecognitionIntent() }, 100)
                        return
                    }
                    "No speech detected"
                }
                SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> {
                    recreateSpeechRecognizer()
                    Log.d(TAG, "Recognizer busy, retrying after delay")
                    handler.postDelayed({ startRecognitionIntent() }, 500)
                    return
                }
                SpeechRecognizer.ERROR_SERVER -> {
                    recreateSpeechRecognizer()
                    "Server error"
                }
                SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                    if (continuousRecognition) {
                        Log.d(TAG, "Speech timeout, restarting recognition")
                        handler.postDelayed({ startRecognitionIntent() }, 100)
                        return
                    }
                    "No speech input"
                }
                else -> "Unknown error"
            }
            Log.e(TAG, "Recognition error: $errorMessage")
            listener?.onError(errorMessage)
            
            if (continuousRecognition && error != SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                handler.postDelayed({ startRecognitionIntent() }, 1000)
            } else {
                isListening = false
            }
        }

        override fun onResults(results: Bundle?) {
            isListening = false
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val recognizedText = matches[0]
                if (recognizedText.isNotEmpty()) {
                    Log.d(TAG, "Final result: $recognizedText")
                    listener?.onResult(recognizedText)
                    lastPartialResults = ""
                    noMatchRetryCount = 0
                }
            }
            
            if (continuousRecognition) {
                handler.postDelayed({
                    if (continuousRecognition && !isDestroyed) {
                        startRecognitionIntent()
                    }
                }, 50)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val partialText = matches[0]
                if (partialText.isNotEmpty() && partialText != lastPartialResults) {
                    Log.d(TAG, "Partial result: $partialText")
                    listener?.onPartialResult(partialText)
                    lastPartialResults = partialText
                    noMatchRetryCount = 0
                }
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) {
            // Ignore other events
        }
    }
    
    private fun recreateSpeechRecognizer() {
        try {
            recognizer?.destroy()
            recognizer = null
            setupSpeechRecognizer()
        } catch (e: Exception) {
            Log.e(TAG, "Error recreating speech recognizer: ${e.message}")
        }
    }
    
    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }
    
    override fun startListening(language: String) {
        if (isDestroyed || isListening) return
        
        this.language = language
        this.continuousRecognition = true
        this.noMatchRetryCount = 0
        this.lastPartialResults = ""
        
        if (recognizer == null) {
            setupSpeechRecognizer()
        }
        
        startRecognitionIntent()
    }
    
    private fun startRecognitionIntent() {
        if (isListening || recognizer == null || isDestroyed) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 0)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 1500)
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_CONFIDENCE_SCORES, true)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
        }
        
        try {
            if (context is AppCompatActivity) {
                (context as AppCompatActivity).startActivityForResult(intent, 1)
            } else {
                Log.e(TAG, "Context is not an AppCompatActivity")
                listener?.onError("Speech recognition not available")
            }
            isListening = true
            listener?.onRecognitionStarted()
            Log.d(TAG, "Started recognition intent")
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recognition: ${e.message}")
            listener?.onError("Could not start speech recognition: ${e.message}")
            isListening = false
            continuousRecognition = false
            recreateSpeechRecognizer()
        }
    }
    
    override fun stopListening() {
        continuousRecognition = false
        isListening = false
        try {
            recognizer?.stopListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recognition: ${e.message}")
        }
    }
    
    override fun isListening(): Boolean {
        return isListening || continuousRecognition
    }
    
    override fun destroy() {
        isDestroyed = true
        continuousRecognition = false
        isListening = false
        try {
            recognizer?.destroy()
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognizer: ${e.message}")
        }
        recognizer = null
        handler.removeCallbacksAndMessages(null)
    }
}
