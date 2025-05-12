package com.example.myapplication.speech

import android.content.Context
import android.media.MediaRecorder
import android.os.Build
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Offline speech recognizer based on the Vosk library.
 *
 * The recognizer loads a compact model from the app assets (e.g. `model-en-us`) and
 * performs on-device transcription with real-time partial results.
 *
 * Copy the desired language model directory into `android/app/src/main/assets/`.
 * You can download ready-to-use models from https://alphacephei.com/vosk/models .
 * For English try `vosk-model-small-en-us-0.15` (~50 MB, works on most phones).
 */
class VoskSpeechRecognizer(private val context: Context) : SpeechRecognizer, RecognitionListener {

    private val TAG = "VoskRecognizer"
    private var listener: SpeechRecognitionListener? = null

    private var model: Model? = null
    private var recognizer: Recognizer? = null
    private var speechService: SpeechService? = null
    private var isDestroyed = false
    
    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var recordingFile: File? = null
    private var isRecording = false
    
    // Audio storage directory
    private val audioStorageDir: File by lazy {
        val dir = File(context.getExternalFilesDir(null), "audio_recordings")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    /** true when SpeechService is active */
    private var isRunning = false

    /*--------------------------------------------------------------------------------------------*/
    /*  Public API                                                                                */
    /*--------------------------------------------------------------------------------------------*/

    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }

    override fun startListening(language: String) {
        if (isDestroyed) {
            listener?.onError("Recognizer has been destroyed")
            return
        }

        // Start audio recording
        try {
            prepareRecording()
            startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recording", e)
            listener?.onError("Failed to start recording: ${e.message}")
            return
        }

        // Map Android locale (en-US, es-ES, …) to model folder (en-us, es, …)
        val langPrefix = language.substring(0, 2).lowercase(Locale.getDefault())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                initModelIfNeeded(langPrefix)
                withContext(Dispatchers.Main) {
                    if (!isDestroyed) {
                        startRecognitionInternal()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recognizer", e)
                withContext(Dispatchers.Main) {
                    listener?.onError("Failed to start recognition: ${e.message}")
                }
            }
        }
    }

    override fun stopListening() {
        try {
            speechService?.stop()
            stopRecording()
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech service", e)
        } finally {
            isRunning = false
        }
    }

    override fun isListening(): Boolean = isRunning || isRecording

    override fun destroy() {
        isDestroyed = true
        stopListening()
        try {
            speechService?.shutdown()
            mediaRecorder?.release()
        } catch (e: Exception) {
            Log.e(TAG, "Error shutting down speech service", e)
        }
        try {
            recognizer?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing recognizer", e)
        }
        try {
            model?.close()
        } catch (e: Exception) {
            Log.e(TAG, "Error closing model", e)
        }
        speechService = null
        recognizer = null
        model = null
        mediaRecorder = null
        recordingFile = null
    }

    /*--------------------------------------------------------------------------------------------*/
    /*  RecognitionListener callbacks                                                             */
    /*--------------------------------------------------------------------------------------------*/

    override fun onPartialResult(hypothesis: String?) {
        Log.d(TAG, "Received partial result: $hypothesis")
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "partial")
            if (!text.isNullOrEmpty()) {
                Log.d(TAG, "Parsed partial result: $text")
                listener?.onPartialResult(text)
            } else {
                Log.w(TAG, "Empty partial result after parsing")
            }
        } else {
            Log.w(TAG, "Null or empty hypothesis received")
        }
    }

    override fun onResult(hypothesis: String?) {
        Log.d(TAG, "Received final result: $hypothesis")
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "text")
            if (text != null && text.isNotEmpty()) {
                Log.d(TAG, "Parsed final result: $text")
                listener?.onResult(text, recordingFile?.absolutePath)
            } else {
                Log.w(TAG, "Empty final result after parsing")
            }
        } else {
            Log.w(TAG, "Null or empty hypothesis received")
        }
    }

    override fun onFinalResult(hypothesis: String?) {
        // Same as onResult for Vosk. Handle gracefully.
        onResult(hypothesis)
    }

    override fun onError(e: Exception?) {
        Log.e(TAG, "Recognition error", e)
        listener?.onError(e?.message ?: "Unknown error")
        isRunning = false
    }

    override fun onTimeout() {
        // Treat timeout as normal end
        stopListening()
    }

    /*--------------------------------------------------------------------------------------------*/
    /*  Audio Recording                                                                           */
    /*--------------------------------------------------------------------------------------------*/

    private fun prepareRecording() {
        // Create file with timestamp in the audio storage directory
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fileName = "recording_$timestamp.m4a"
        recordingFile = File(audioStorageDir, fileName)
        
        Log.d(TAG, "Preparing recording at: ${recordingFile?.absolutePath}")
        
        // Initialize MediaRecorder
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
            setOutputFile(recordingFile?.absolutePath)
            
            try {
                prepare()
            } catch (e: IOException) {
                Log.e(TAG, "Failed to prepare recorder", e)
                throw e
            }
        }
    }
    
    private fun startRecording() {
        mediaRecorder?.start()
        isRecording = true
    }
    
    private fun stopRecording(): String? {
        return try {
            if (!isRecording) {
                Log.d(TAG, "Not recording, nothing to stop")
                return null
            }
            
            Log.d(TAG, "Stopping recording...")
            mediaRecorder?.apply {
                stop()
                release()
            }
            mediaRecorder = null
            isRecording = false
            
            val filePath = recordingFile?.absolutePath
            if (filePath != null) {
                val file = File(filePath)
                if (file.exists() && file.length() > 0) {
                    Log.d(TAG, "Recording stopped successfully. File saved at: $filePath, size: ${file.length()} bytes")
                } else {
                    Log.e(TAG, "Recording stopped but file is missing or empty: $filePath")
                }
            } else {
                Log.e(TAG, "Recording stopped but no file path available")
            }
            
            filePath
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping recording", e)
            throw e
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    /*  Internal helpers                                                                          */
    /*--------------------------------------------------------------------------------------------*/

    private suspend fun initModelIfNeeded(langPrefix: String) {
        if (model != null) {
            Log.d(TAG, "Model already initialized")
            return
        }
        if (isDestroyed) {
            Log.d(TAG, "Recognizer is destroyed, skipping model initialization")
            return
        }

        listener?.onPartialResult("Loading offline model…")
        try {
            // Copy assets model directory to internal storage (blocking) the first time
            val modelDirName = when (langPrefix) {
                "es" -> "model-es"
                "fr" -> "model-fr"
                else -> "model-en-us" // default to English
            }

            // List available assets
            val assets = context.assets.list("") ?: emptyArray()
            Log.d(TAG, "Available assets: ${assets.joinToString()}")

            // Check if model exists in assets
            if (!assets.contains(modelDirName)) {
                throw IOException("Model directory '$modelDirName' not found in assets. Available assets: ${assets.joinToString()}")
            }

            // List model directory contents
            val modelContents = context.assets.list(modelDirName) ?: emptyArray()
            Log.d(TAG, "Model directory contents: ${modelContents.joinToString()}")

            // Copy model to internal storage
            val modelPath = StorageService.sync(context, modelDirName, "models")
            Log.d(TAG, "Model path after sync: $modelPath")

            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                throw IOException("Failed to copy model to internal storage. Path '$modelPath' does not exist")
            }

            // Check model directory structure
            val modelDirContents = modelFile.list() ?: emptyArray()
            Log.d(TAG, "Model directory contents after sync: ${modelDirContents.joinToString()}")

            // Initialize model
            try {
                model = Model(modelPath)
                Log.d(TAG, "Model successfully loaded from $modelPath")
            } catch (e: Exception) {
                throw IOException("Failed to initialize Vosk model from path '$modelPath': ${e.message}")
            }
        } catch (io: IOException) {
            Log.e(TAG, "Failed to load Vosk model", io)
            throw IOException("Failed to load Vosk model: ${io.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error loading model", e)
            throw e
        }
    }

    private fun startRecognitionInternal() {
        if (isDestroyed) {
            Log.d(TAG, "Recognizer is destroyed, skipping recognition start")
            return
        }
        if (model == null) {
            val error = "Model not initialized"
            Log.e(TAG, error)
            throw IllegalStateException(error)
        }
        if (isRunning) {
            Log.d(TAG, "Recognition already running")
            return
        }

        try {
            Log.d(TAG, "Creating new recognizer")
            recognizer = Recognizer(model, 16000.0f)
            Log.d(TAG, "Creating speech service")
            speechService = SpeechService(recognizer, 16000.0f)
            Log.d(TAG, "Starting speech service")
            speechService?.startListening(this)
            isRunning = true
            listener?.onRecognitionStarted()
            Log.d(TAG, "Offline recognizer started successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to start recognition", e)
            throw e
        }
    }

    private fun parseJsonText(json: String, key: String): String? {
        return try {
            Log.d(TAG, "Parsing JSON: $json")
            val jsonObj = JSONObject(json)
            when (key) {
                "partial" -> {
                    val partial = jsonObj.optString("partial")
                    Log.d(TAG, "Extracted partial: $partial")
                    if (partial.isNotEmpty()) partial else null
                }
                "text" -> {
                    val text = jsonObj.optString("text")
                    Log.d(TAG, "Extracted text: $text")
                    if (text.isNotEmpty()) text else null
                }
                else -> {
                    Log.w(TAG, "Unknown key: $key")
                    null
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Vosk JSON: $json", e)
            null
        }
    }
}
