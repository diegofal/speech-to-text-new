package com.example.myapplication.speech

/**
 * Interface for speech recognition result callbacks
 */
interface SpeechRecognitionListener {
    /**
     * Called when speech recognition has started
     */
    fun onRecognitionStarted()
    
    /**
     * Called when partial results are available during recognition
     */
    fun onPartialResult(text: String)
    
    /**
     * Called when final results are available after recognition
     */
    fun onResult(text: String)
    
    /**
     * Called when an error occurs during recognition
     */
    fun onError(errorMessage: String)
}
