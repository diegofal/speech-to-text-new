package com.example.myapplication.speech

import android.content.Context

/**
 * Interface for speech recognition engines
 */
interface SpeechRecognizer {
    /**
     * Set the listener for recognition results
     */
    fun setListener(listener: SpeechRecognitionListener)
    
    /**
     * Start speech recognition
     */
    fun startListening(language: String = "en-US")
    
    /**
     * Stop speech recognition
     */
    fun stopListening()
    
    /**
     * Check if the recognizer is currently listening
     */
    fun isListening(): Boolean
    
    /**
     * Release resources
     */
    fun destroy()
    
    companion object {
        fun createVoskRecognizer(context: Context): SpeechRecognizer {
            return VoskSpeechRecognizer(context)
        }
        
        fun createWhisperRecognizer(context: Context): SpeechRecognizer {
            return WhisperSpeechRecognizer(context)
        }
    }
}
