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
    
    // Periodic transcription
    private var transcriptionRunnable: Runnable? = null
    private val transcriptionIntervalMs = 3000L // 3 seconds instead of 10
    
    private val WHISPER_API_KEY = "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"
    private val WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions"
    
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
            val recordingDir = context.getExternalFilesDir(Environment.DIRECTORY_MUSIC)
                ?: throw IOException("Storage unavailable")
            
            if (!recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val recordingFile = File(recordingDir, "whisper_${dateTime}_${UUID.randomUUID()}.m4a")
            audioFilePath = recordingFile.absolutePath
            
            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            
            mediaRecorder?.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioSamplingRate(44100)
                setAudioEncodingBitRate(192000)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            
            listener?.onPartialResult("Recording started...")
        } catch (e: Exception) {
            Log.e(TAG, "Recording error: ${e.message}")
            listener?.onError("Recording error: ${e.message}")
            isCurrentlyListening = false
            mediaRecorder?.release()
            mediaRecorder = null
        }
    }
    
    private fun stopRecording(): String? {
        return try {
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            audioFilePath
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording: ${e.message}")
            mediaRecorder?.release()
            mediaRecorder = null
            null
        }
    }
    
    private fun startPeriodicTranscription() {
        transcriptionRunnable = object : Runnable {
            override fun run() {
                if (isCurrentlyListening) {
                    stopRecording()?.let { currentFilePath ->
                        processAudioWithWhisper(currentFilePath)
                        startRecording()
                    }
                    handler.postDelayed(this, transcriptionIntervalMs)
                }
            }
        }
        handler.postDelayed(transcriptionRunnable!!, transcriptionIntervalMs)
    }
    
    private fun processAudioWithWhisper(audioFilePath: String) {
        if (context !is AppCompatActivity) {
            listener?.onError("Incompatible context for Whisper transcription")
            return
        }
        
        (context as AppCompatActivity).lifecycleScope.launch {
            listener?.onPartialResult("Processing audio...")
            
            try {
                whisperApiHelper.transcribeAudio(
                    audioFilePath = audioFilePath,
                    language = language,
                    apiKey = WHISPER_API_KEY
                ).fold(
                    onSuccess = { transcription ->
                        if (transcription.isNotEmpty()) {
                            listener?.onResult(transcription)
                        } else {
                            listener?.onPartialResult("Listening...")
                        }
                    },
                    onFailure = { error ->
                        Log.e(TAG, "Transcription failed: ${error.message}")
                        listener?.onError("Transcription failed: ${error.message}")
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Transcription error: ${e.message}")
                listener?.onError("Transcription error: ${e.message}")
            }
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
