package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule
import com.facebook.react.modules.core.PermissionAwareActivity
import com.facebook.react.modules.core.PermissionListener
import java.util.*

class SpeechRecognitionModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext), RecognitionListener, PermissionListener {
    private val TAG = "SpeechRecognitionModule"
    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var pendingPromise: Promise? = null
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1

    override fun getName(): String {
        return "SpeechRecognitionModule"
    }

    @ReactMethod
    fun initialize(promise: Promise) {
        try {
            if (speechRecognizer != null) {
                speechRecognizer?.destroy()
                speechRecognizer = null
            }

            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(reactApplicationContext)
            speechRecognizer?.setRecognitionListener(this)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error initializing speech recognizer: ${e.message}")
            promise.reject("INIT_ERROR", "Error initializing speech recognizer: ${e.message}")
        }
    }

    @ReactMethod
    fun checkPermission(promise: Promise) {
        val permission = ContextCompat.checkSelfPermission(
            reactApplicationContext,
            Manifest.permission.RECORD_AUDIO
        )
        val hasPermission = permission == PackageManager.PERMISSION_GRANTED
        promise.resolve(hasPermission)
    }

    @ReactMethod
    fun requestPermission(promise: Promise) {
        pendingPromise = promise
        val activity = currentActivity as? PermissionAwareActivity
            ?: run {
                promise.reject("ACTIVITY_ERROR", "Activity not found or not a PermissionAwareActivity")
                return
            }

        activity.requestPermissions(
            arrayOf(Manifest.permission.RECORD_AUDIO),
            RECORD_AUDIO_PERMISSION_REQUEST,
            this
        )
    }

    @ReactMethod
    fun startListening(locale: String, promise: Promise) {
        if (isListening) {
            promise.reject("ALREADY_LISTENING", "Speech recognition is already running")
            return
        }

        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, locale)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            }

            speechRecognizer?.startListening(intent)
            isListening = true
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error starting speech recognition: ${e.message}")
            promise.reject("START_ERROR", "Error starting speech recognition: ${e.message}")
        }
    }

    @ReactMethod
    fun stopListening(promise: Promise) {
        if (!isListening) {
            promise.resolve(false)
            return
        }

        try {
            speechRecognizer?.stopListening()
            isListening = false
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping speech recognition: ${e.message}")
            promise.reject("STOP_ERROR", "Error stopping speech recognition: ${e.message}")
        }
    }

    @ReactMethod
    fun destroy(promise: Promise) {
        try {
            speechRecognizer?.destroy()
            speechRecognizer = null
            isListening = false
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error destroying speech recognition: ${e.message}")
            promise.reject("DESTROY_ERROR", "Error destroying speech recognition: ${e.message}")
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<String>, grantResults: IntArray): Boolean {
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            val granted = grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED
            pendingPromise?.resolve(granted)
            pendingPromise = null
            return true
        }
        return false
    }

    // RecognitionListener implementation
    override fun onReadyForSpeech(params: Bundle?) {
        sendEvent("onSpeechStart", Arguments.createMap())
    }

    override fun onBeginningOfSpeech() {
        // Not used
    }

    override fun onRmsChanged(rmsdB: Float) {
        val map = Arguments.createMap()
        map.putDouble("value", rmsdB.toDouble())
        sendEvent("onSpeechVolumeChanged", map)
    }

    override fun onBufferReceived(buffer: ByteArray?) {
        // Not used
    }

    override fun onEndOfSpeech() {
        sendEvent("onSpeechEnd", Arguments.createMap())
    }

    override fun onError(error: Int) {
        val map = Arguments.createMap()
        map.putInt("error", error)
        map.putString("message", getErrorMessage(error))
        sendEvent("onSpeechError", map)
        isListening = false
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.size > 0) {
            val map = Arguments.createMap()
            val array = Arguments.createArray()
            for (match in matches) {
                array.pushString(match)
            }
            map.putArray("value", array)
            sendEvent("onSpeechResults", map)
        }
        isListening = false
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (matches != null && matches.size > 0) {
            val map = Arguments.createMap()
            val array = Arguments.createArray()
            for (match in matches) {
                array.pushString(match)
            }
            map.putArray("value", array)
            sendEvent("onSpeechPartialResults", map)
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {
        // Not used
    }

    private fun sendEvent(eventName: String, params: WritableMap) {
        reactApplicationContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }

    private fun getErrorMessage(error: Int): String {
        return when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No recognition result matched"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognition service busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Unknown error"
        }
    }
}
