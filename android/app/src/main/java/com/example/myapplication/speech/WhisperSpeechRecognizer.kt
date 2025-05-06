package com.example.myapplication.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.lifecycle.lifecycleScope
import androidx.appcompat.app.AppCompatActivity
import com.example.myapplication.speech.api.WhisperApiHelper
import kotlinx.coroutines.launch
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * Implementation of SpeechRecognizer that uses OpenAI's Whisper API
 */
class WhisperSpeechRecognizer(private val context: Context) : SpeechRecognizer {
    private val TAG = "WhisperSpeechRecognizer"
    private var listener: SpeechRecognitionListener? = null
    private var isCurrentlyListening = false
    private var language = "en"
    private val handler = Handler(Looper.getMainLooper())
    
    // Whisper API helper
    private val whisperApiHelper = WhisperApiHelper(context)
    
    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var recordingStartTimeMs: Long = 0
    
    // Periodic transcription
    private var transcriptionRunnable: Runnable? = null
    private val transcriptionIntervalMs = 10000L // 10 seconds
    
    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }
    
    override fun startListening(language: String) {
        this.language = convertLanguageCode(language)
        isCurrentlyListening = true
        listener?.onRecognitionStarted()
        
        // Start recording audio for Whisper
        startRecording()
        
        // Start periodic transcription
        startPeriodicTranscription()
    }
    
    private fun isApiKeyConfigured(): Boolean {
        // Check if API key is stored in secure preferences
        return true // We're now automatically storing the key
    }
    
    private fun startRecording() {
        try {
            // Create file for recording
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val recordingDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            if (recordingDir != null && !recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            if (recordingDir == null) {
                Log.e(TAG, "Error: External files directory is null")
                listener?.onError("Storage unavailable")
                isCurrentlyListening = false
                return
            }
            
            val recordingFile = File(recordingDir, "whisper_${dateTime}_${UUID.randomUUID()}.m4a")
            audioFilePath = recordingFile.absolutePath
            
            Log.d(TAG, "Whisper recording setup: Will store audio at $audioFilePath")
            
            // Initialize media recorder with higher quality for better transcription
            try {
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(context)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.MPEG_4) // Better for Whisper
                    setAudioEncoder(MediaRecorder.AudioEncoder.AAC) // Better quality
                    setAudioSamplingRate(44100) // CD quality
                    setAudioEncodingBitRate(192000) // High quality bitrate
                    setOutputFile(audioFilePath)
                    prepare()
                    start()
                    
                    recordingStartTimeMs = System.currentTimeMillis()
                    Log.d(TAG, "Whisper recording started: $audioFilePath")
                    listener?.onPartialResult("Recording started... Will transcribe soon.")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing recorder: ${e.message}")
                listener?.onError("Error with recorder: ${e.message}")
                isCurrentlyListening = false
                mediaRecorder = null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating recording file: ${e.message}")
            listener?.onError("Could not start recording audio: ${e.message}")
            isCurrentlyListening = false
        }
    }
    
    private fun stopRecording(): String? {
        if (mediaRecorder != null) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                
                Log.d(TAG, "Whisper recording stopped: $audioFilePath")
                return audioFilePath
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording: ${e.message}")
                mediaRecorder?.release()
                mediaRecorder = null
            }
        }
        return null
    }
    
    private fun startPeriodicTranscription() {
        // Create runnable for periodic transcription
        transcriptionRunnable = object : Runnable {
            override fun run() {
                if (isCurrentlyListening) {
                    // Temporarily stop recording to process the current audio
                    val currentFilePath = stopRecording()
                    
                    if (currentFilePath != null) {
                        // Process the audio file with Whisper API
                        processAudioWithWhisper(currentFilePath)
                        
                        // Start a new recording session
                        startRecording()
                    }
                    
                    // Schedule the next transcription
                    handler.postDelayed(this, transcriptionIntervalMs)
                }
            }
        }
        
        // Schedule first transcription after the interval
        handler.postDelayed(transcriptionRunnable!!, transcriptionIntervalMs)
    }
    
    private fun processAudioWithWhisper(audioFilePath: String) {
        if (context is AppCompatActivity) {
            (context as AppCompatActivity).lifecycleScope.launch {
                listener?.onPartialResult("Processing audio with Whisper...")
                
                try {
                    val result = whisperApiHelper.transcribeAudio(audioFilePath, language)
                    
                    result.fold(
                        onSuccess = { transcription ->
                            if (transcription.isNotEmpty()) {
                                listener?.onResult(transcription)
                            } else {
                                listener?.onPartialResult("No speech detected.")
                            }
                        },
                        onFailure = { error ->
                            Log.e(TAG, "Whisper transcription failed", error)
                            listener?.onError("Transcription failed: ${error.message}")
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during transcription", e)
                    listener?.onError("Exception during transcription: ${e.message}")
                }
            }
        } else {
            Log.e(TAG, "Context is not an AppCompatActivity")
            listener?.onError("Incompatible context for Whisper transcription")
        }
    }
    
    private fun convertLanguageCode(languageCode: String): String {
        // Convert Android language codes (e.g., en-US) to Whisper language codes (e.g., en)
        return languageCode.split("-")[0].lowercase(Locale.getDefault())
    }
    
    override fun stopListening() {
        isCurrentlyListening = false
        transcriptionRunnable?.let { handler.removeCallbacks(it) }
        stopRecording()
    }
    
    override fun isListening(): Boolean {
        return isCurrentlyListening
    }
    
    override fun destroy() {
        stopListening()
        mediaRecorder?.release()
        mediaRecorder = null
        audioFilePath = null
    }
    
    companion object {
        class Factory : SpeechRecognizer.Factory {
            override fun create(context: Context): SpeechRecognizer = WhisperSpeechRecognizer(context)
        }
    }
}
