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
     * Called when speech recognition has ended
     */
    fun onRecognitionEnded()
    
    /**
     * Called when partial results are available during recognition
     */
    fun onPartialResult(text: String)
    
    /**
     * Called when final results are available after recognition
     */
    fun onResult(text: String, audioFilePath: String? = null)
    
    /**
     * Called when an error occurs during recognition
     */
    fun onError(error: String)
}
