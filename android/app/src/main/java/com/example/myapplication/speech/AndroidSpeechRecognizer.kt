package com.example.myapplication.speech

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognizerIntent
import android.util.Log
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

/**
 * Implementation of SpeechRecognizer that uses Android's built-in speech recognition dialog
 */
class AndroidSpeechRecognizer(private val context: Context) : SpeechRecognizer {
    private val TAG = "AndroidSpeechRecognizer"
    private var listener: SpeechRecognitionListener? = null
    private var isCurrentlyListening = false
    private var language = "en-US"
    private val handler = Handler(Looper.getMainLooper())
    private var continuousRecognition = false
    
    // Activity result launcher for speech recognition
    private var resultLauncher: ActivityResultLauncher<Intent>? = null
    
    init {
        if (context is AppCompatActivity) {
            setupActivityResultLauncher(context)
        } else {
            Log.e(TAG, "Context must be an AppCompatActivity")
        }
    }
    
    private fun setupActivityResultLauncher(activity: AppCompatActivity) {
        resultLauncher = activity.registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val results = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                if (!results.isNullOrEmpty()) {
                    val recognizedText = results[0]
                    if (recognizedText.isNotEmpty()) {
                        Log.d(TAG, "Recognized: $recognizedText")
                        listener?.onResult(recognizedText)
                    }
                }
            } else {
                Log.d(TAG, "Recognition canceled or failed")
                listener?.onError("Recognition canceled or no results")
            }
            
            // Restart recognition if in continuous mode
            isCurrentlyListening = false
            if (continuousRecognition) {
                handler.postDelayed({
                    if (continuousRecognition) {
                        startRecognitionIntent()
                    }
                }, 500) // Short delay to avoid busy state
            }
        }
    }
    
    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }
    
    override fun startListening(language: String) {
        this.language = language
        this.continuousRecognition = true
        startRecognitionIntent()
    }
    
    private fun startRecognitionIntent() {
        if (isCurrentlyListening || resultLauncher == null) return
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, language)
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak now...")
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        try {
            resultLauncher?.launch(intent)
            isCurrentlyListening = true
            listener?.onRecognitionStarted()
            Log.d(TAG, "Started recognition intent")
        } catch (e: Exception) {
            Log.e(TAG, "Could not start recognition: ${e.message}")
            listener?.onError("Could not start speech recognition: ${e.message}")
            isCurrentlyListening = false
            continuousRecognition = false
        }
    }
    
    override fun stopListening() {
        continuousRecognition = false
        isCurrentlyListening = false
    }
    
    override fun isListening(): Boolean {
        return isCurrentlyListening || continuousRecognition
    }
    
    override fun destroy() {
        continuousRecognition = false
        isCurrentlyListening = false
        handler.removeCallbacksAndMessages(null)
    }
    
    /**
     * Factory for creating Android speech recognizers
     */
    class Factory : SpeechRecognizer.Factory {
        override fun create(context: Context): SpeechRecognizer {
            return AndroidSpeechRecognizer(context)
        }
    }
}
