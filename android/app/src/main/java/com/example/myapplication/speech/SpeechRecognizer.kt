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
    fun startListening(language: String)
    
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
    
    /**
     * Factory interface for creating speech recognizers
     */
    interface Factory {
        fun create(context: Context): SpeechRecognizer
    }
}
