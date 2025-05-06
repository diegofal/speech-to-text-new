package com.example.myapplication.speech

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException
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

    /** true when SpeechService is active */
    private var isRunning = false

    /*--------------------------------------------------------------------------------------------*/
    /*  Public API                                                                                */
    /*--------------------------------------------------------------------------------------------*/

    override fun setListener(listener: SpeechRecognitionListener) {
        this.listener = listener
    }

    override fun startListening(language: String) {
        // Map Android locale (en-US, es-ES, …) to model folder (en-us, es, …)
        val langPrefix = language.substring(0, 2).lowercase(Locale.getDefault())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                initModelIfNeeded(langPrefix)
                startRecognitionInternal()
            } catch (e: Exception) {
                Log.e(TAG, "Failed to start recognizer", e)
                listener?.onError("${e.message}")
            }
        }
    }

    override fun stopListening() {
        speechService?.stop()
        isRunning = false
    }

    override fun isListening(): Boolean = isRunning

    override fun destroy() {
        stopListening()
        speechService?.shutdown()
        recognizer?.close()
        model?.close()
        speechService = null
        recognizer = null
        model = null
    }

    /*--------------------------------------------------------------------------------------------*/
    /*  RecognitionListener callbacks                                                             */
    /*--------------------------------------------------------------------------------------------*/

    override fun onPartialResult(hypothesis: String?) {
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "partial")
            if (!text.isNullOrEmpty()) listener?.onPartialResult(text)
        }
    }

    override fun onResult(hypothesis: String?) {
        if (!hypothesis.isNullOrEmpty()) {
            val text = parseJsonText(hypothesis, "text")
            if (!text.isNullOrEmpty()) listener?.onResult(text)
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
    /*  Internal helpers                                                                          */
    /*--------------------------------------------------------------------------------------------*/

    private suspend fun initModelIfNeeded(langPrefix: String) {
        if (model != null) return
        listener?.onPartialResult("Loading offline model…")
        try {
            // Copy assets model directory to internal storage (blocking) the first time
            val modelDirName = when (langPrefix) {
                "es" -> "model-es"
                "fr" -> "model-fr"
                else -> "model-en-us" // default to English
            }
            val modelPath = StorageService.sync(context, modelDirName, "models")
            model = Model(modelPath)
            Log.d(TAG, "Model loaded from $modelPath")
        } catch (io: IOException) {
            throw IOException("Failed to load Vosk model: ${io.message}")
        }
    }

    private fun startRecognitionInternal() {
        if (model == null) throw IllegalStateException("Model not initialised")
        if (isRunning) return

        recognizer = Recognizer(model, 16000.0f)
        speechService = SpeechService(recognizer, 16000.0f)
        speechService?.startListening(this)
        isRunning = true
        listener?.onRecognitionStarted()
        Log.d(TAG, "Offline recognizer started")
    }

    private fun parseJsonText(json: String, key: String): String? {
        return try {
            JSONObject(json).optString(key)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to parse Vosk JSON", e); null
        }
    }

    /*--------------------------------------------------------------------------------------------*/
    /*  Factory                                                                                   */
    /*--------------------------------------------------------------------------------------------*/

    class Factory : SpeechRecognizer.Factory {
        override fun create(context: Context): SpeechRecognizer = VoskSpeechRecognizer(context)
    }
}
