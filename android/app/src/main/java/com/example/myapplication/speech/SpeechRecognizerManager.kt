package com.example.myapplication.speech

import android.content.Context

/**
 * Manager class that handles different speech recognition implementations
 */
class SpeechRecognizerManager(private val context: Context) {
    
    companion object {
        const val TYPE_ANDROID = 0
        const val TYPE_WHISPER = 1
        const val TYPE_VOSK = 2
    }
    
    private var currentRecognizer: SpeechRecognizer? = null
    private var currentType = TYPE_ANDROID
    
    /**
     * Create and return a speech recognizer of the specified type
     */
    fun createSpeechRecognizer(type: Int): SpeechRecognizer {
        // Clean up any existing recognizer
        destroyCurrentRecognizer()
        
        // Create the new recognizer
        currentRecognizer = when (type) {
            TYPE_WHISPER -> SpeechRecognizer.createWhisperRecognizer(context)
            TYPE_VOSK -> SpeechRecognizer.createVoskRecognizer(context)
            else -> AndroidSpeechRecognizer(context)
        }
        
        currentType = type
        return currentRecognizer!!
    }
    
    /**
     * Get the current active recognizer, creating an Android one if none exists
     */
    fun getCurrentRecognizer(): SpeechRecognizer {
        if (currentRecognizer == null) {
            currentRecognizer = AndroidSpeechRecognizer(context)
            currentType = TYPE_ANDROID
        }
        return currentRecognizer!!
    }
    
    /**
     * Get the type of the current recognizer
     */
    fun getCurrentRecognizerType(): Int {
        return currentType
    }
    
    /**
     * Release resources used by the current recognizer
     */
    fun destroyCurrentRecognizer() {
        currentRecognizer?.destroy()
        currentRecognizer = null
    }
}
