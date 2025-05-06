package com.example.myapplication

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.myapplication.speech.SpeechRecognitionListener
import com.example.myapplication.speech.SpeechRecognizer
import com.example.myapplication.speech.SpeechRecognizerManager
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SpeechRecognitionListener {
    private val TAG = "SpeechTranscriber"
    private val RECORD_AUDIO_PERMISSION_REQUEST = 1

    // UI components
    private lateinit var startButton: Button
    private lateinit var stopButton: Button
    private lateinit var resultTextView: TextView
    private lateinit var partialResultTextView: TextView
    private lateinit var timerTextView: TextView
    private lateinit var languageRadioGroup: RadioGroup
    private lateinit var radioEnglish: RadioButton
    private lateinit var radioSpanish: RadioButton
    private lateinit var recognizerRadioGroup: RadioGroup
    private lateinit var radioAndroid: RadioButton
    private lateinit var radioWhisper: RadioButton
    private lateinit var radioVosk: RadioButton
    
    // Speech recognition
    private lateinit var speechRecognizerManager: SpeechRecognizerManager
    private lateinit var speechRecognizer: SpeechRecognizer
    private var currentRecognizerType = SpeechRecognizerManager.TYPE_ANDROID
    
    // Results tracking
    private val allResults = StringBuilder()
    private val handler = Handler(Looper.getMainLooper())
    
    // Language settings
    private var selectedLanguage = "en-US" // Default to English
    
    // Timestamp tracking
    private var startTime: Long = 0
    private val timestampFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    private var elapsedTimeInSeconds = 0
    private var timerRunnable: Runnable? = null
    
    // Audio recording
    private var mediaRecorder: MediaRecorder? = null
    private var audioFilePath: String? = null
    private var isRecording = false

    companion object {
        const val MENU_VOSK_RECOGNIZER_ID = 103
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Initialize UI components
        initializeUI()
        
        // Initialize speech recognizer
        speechRecognizerManager = SpeechRecognizerManager(this)
        switchSpeechRecognizer(SpeechRecognizerManager.TYPE_ANDROID)
        
        updateButtonState(false)
        updateLabelsForLanguage(radioEnglish.isChecked)
    }
    
    private fun initializeUI() {
        startButton = findViewById(R.id.startButton)
        stopButton = findViewById(R.id.stopButton)
        resultTextView = findViewById(R.id.resultTextView)
        partialResultTextView = findViewById(R.id.partialResultTextView)
        timerTextView = findViewById(R.id.timerTextView)
        languageRadioGroup = findViewById(R.id.languageRadioGroup)
        radioEnglish = findViewById(R.id.radioEnglish)
        radioSpanish = findViewById(R.id.radioSpanish)
        recognizerRadioGroup = findViewById(R.id.recognizerRadioGroup)
        radioAndroid = findViewById(R.id.radioAndroid)
        radioWhisper = findViewById(R.id.radioWhisper)
        radioVosk = findViewById(R.id.radioVosk)

        // Set up language selection listener
        languageRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioEnglish -> {
                    selectedLanguage = "en-US"
                    updateLabelsForLanguage(isEnglish = true)
                }
                R.id.radioSpanish -> {
                    selectedLanguage = "es-ES"
                    updateLabelsForLanguage(isEnglish = false)
                }
            }
        }
        
        // Set up recognizer type selection listener
        recognizerRadioGroup.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.radioAndroid -> {
                    if (currentRecognizerType != SpeechRecognizerManager.TYPE_ANDROID) {
                        switchSpeechRecognizer(SpeechRecognizerManager.TYPE_ANDROID)
                        Toast.makeText(this, "Switched to Android Speech Recognizer", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.radioWhisper -> {
                    if (currentRecognizerType != SpeechRecognizerManager.TYPE_WHISPER) {
                        switchSpeechRecognizer(SpeechRecognizerManager.TYPE_WHISPER)
                        Toast.makeText(this, "Switched to Whisper Speech Recognizer (Experimental)", Toast.LENGTH_SHORT).show()
                    }
                }
                R.id.radioVosk -> {
                    if (currentRecognizerType != SpeechRecognizerManager.TYPE_VOSK) {
                        switchSpeechRecognizer(SpeechRecognizerManager.TYPE_VOSK)
                        Toast.makeText(this, "Switched to Offline Vosk Recognizer", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        startButton.setOnClickListener {
            checkPermissionAndStartListening()
        }

        stopButton.setOnClickListener {
            stopSession()
        }
    }
    
    private fun switchSpeechRecognizer(type: Int) {
        speechRecognizer = speechRecognizerManager.createSpeechRecognizer(type)
        speechRecognizer.setListener(this)
        currentRecognizerType = type
    }
    
    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return true
    }
    
    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            R.id.menu_android_recognizer -> {
                if (currentRecognizerType != SpeechRecognizerManager.TYPE_ANDROID) {
                    switchSpeechRecognizer(SpeechRecognizerManager.TYPE_ANDROID)
                    Toast.makeText(this, "Switched to Android Speech Recognizer", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_whisper_recognizer -> {
                if (currentRecognizerType != SpeechRecognizerManager.TYPE_WHISPER) {
                    switchSpeechRecognizer(SpeechRecognizerManager.TYPE_WHISPER)
                    Toast.makeText(this, "Switched to Whisper Speech Recognizer", Toast.LENGTH_SHORT).show()
                }
                true
            }
            R.id.menu_vosk_recognizer -> {
                if (currentRecognizerType != SpeechRecognizerManager.TYPE_VOSK) {
                    switchSpeechRecognizer(SpeechRecognizerManager.TYPE_VOSK)
                    Toast.makeText(this, "Switched to Offline Vosk Recognizer", Toast.LENGTH_SHORT).show()
                }
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
    
    private fun updateLabelsForLanguage(isEnglish: Boolean) {
        if (isEnglish) {
            startButton.text = "Start Listening"
            stopButton.text = "Stop Listening"
            findViewById<TextView>(R.id.partialLabelTextView).text = "Listening:"
            findViewById<TextView>(R.id.resultLabelTextView).text = "Final Result:"
        } else {
            startButton.text = "Comenzar a Escuchar"
            stopButton.text = "Dejar de Escuchar"
            findViewById<TextView>(R.id.partialLabelTextView).text = "Escuchando:"
            findViewById<TextView>(R.id.resultLabelTextView).text = "Resultado Final:"
        }
    }

    private fun checkPermissionAndStartListening() {
        val permissions = mutableListOf(
            Manifest.permission.RECORD_AUDIO
        )
        
        // Add storage permissions for API < 33 (for audio recording)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
            permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        
        // Check if any permissions are missing
        val missingPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }.toTypedArray()
        
        if (missingPermissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(
                this,
                missingPermissions,
                RECORD_AUDIO_PERMISSION_REQUEST
            )
        } else {
            startSession()
        }
    }
    
    private fun startSession() {
        // Clear previous results when starting a new session
        allResults.clear()
        resultTextView.text = ""
        partialResultTextView.text = ""
        
        // Start timer
        startTimer()
        
        // Start audio recording
        startRecording()
        
        // Start speech recognition
        speechRecognizer.startListening(selectedLanguage)
        
        // Update button state
        updateButtonState(true)
    }
    
    private fun stopSession() {
        // Stop speech recognition
        speechRecognizer.stopListening()
        
        // Stop recording
        stopRecording()
        
        // Stop timer
        stopTimer()
        
        // Save transcript
        saveTranscriptToFile()
        
        // Update button state
        updateButtonState(false)
    }
    
    // Audio recording methods
    private fun startRecording() {
        try {
            // Create file for recording in the app's private storage
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val recordingDir = getExternalFilesDir(Environment.DIRECTORY_MUSIC)
            if (recordingDir != null && !recordingDir.exists()) {
                recordingDir.mkdirs()
            }
            
            if (recordingDir == null) {
                Log.e(TAG, "Error: External files directory is null")
                Toast.makeText(this, "Storage unavailable", Toast.LENGTH_SHORT).show()
                return
            }
            
            val recordingFile = File(recordingDir, "recording_${dateTime}.3gp")
            audioFilePath = recordingFile.absolutePath
            
            Log.d(TAG, "Recording setup: Will store audio at $audioFilePath")
            
            // Initialize media recorder
            try {
                mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    MediaRecorder(this)
                } else {
                    @Suppress("DEPRECATION")
                    MediaRecorder()
                }
                
                mediaRecorder?.apply {
                    setAudioSource(MediaRecorder.AudioSource.MIC)
                    setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                    setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                    setOutputFile(audioFilePath)
                    prepare()
                    start()
                    
                    isRecording = true
                    Log.d(TAG, "Recording started: $audioFilePath")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing recorder: ${e.message}")
                Toast.makeText(this, "Error with recorder: ${e.message}", Toast.LENGTH_SHORT).show()
                
                // Continue without recording
                isRecording = false
                mediaRecorder = null
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error creating recording file: ${e.message}")
            Toast.makeText(this, "Could not start recording audio: ${e.message}", Toast.LENGTH_SHORT).show()
            
            // Continue without recording
            isRecording = false
        }
    }
    
    private fun stopRecording() {
        if (isRecording && mediaRecorder != null) {
            try {
                mediaRecorder?.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
                isRecording = false
                
                // Notify user
                val message = if (selectedLanguage == "es-ES") 
                    "Grabación guardada en: $audioFilePath" 
                else 
                    "Recording saved to: $audioFilePath"
                    
                Toast.makeText(this, message, Toast.LENGTH_LONG).show()
                Log.d(TAG, "Recording stopped and saved: $audioFilePath")
            } catch (e: Exception) {
                Log.e(TAG, "Error stopping recording: ${e.message}")
                mediaRecorder?.release()
                mediaRecorder = null
                isRecording = false
            }
        }
    }
    
    // Timer methods
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        elapsedTimeInSeconds = 0
        updateTimerDisplay()
        
        // Create runnable for timer updates
        timerRunnable = object : Runnable {
            override fun run() {
                elapsedTimeInSeconds = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                updateTimerDisplay()
                handler.postDelayed(this, 1000) // Update every second
            }
        }
        
        // Start timer updates
        handler.post(timerRunnable!!)
    }
    
    private fun stopTimer() {
        timerRunnable?.let {
            handler.removeCallbacks(it)
        }
    }
    
    private fun updateTimerDisplay() {
        val timeString = getFormattedElapsedTime()
        timerTextView.text = timeString
    }
    
    private fun getFormattedElapsedTime(): String {
        val hours = elapsedTimeInSeconds / 3600
        val minutes = (elapsedTimeInSeconds % 3600) / 60
        val seconds = elapsedTimeInSeconds % 60
        return String.format("%02d:%02d:%02d", hours, minutes, seconds)
    }
    
    private fun getCurrentTimestamp(): String {
        val calendar = Calendar.getInstance()
        val date = dateFormat.format(calendar.time)
        val time = timestampFormat.format(calendar.time)
        return "[$date $time]"
    }

    private fun saveTranscriptToFile() {
        if (allResults.isEmpty()) return
        
        try {
            val dateTime = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val transcriptDir = getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
            
            if (transcriptDir == null) {
                Log.e(TAG, "Error: External documents directory is null")
                Toast.makeText(this, "Storage unavailable for saving transcript", Toast.LENGTH_SHORT).show()
                return
            }
            
            if (!transcriptDir.exists()) {
                transcriptDir.mkdirs()
            }
            
            val transcriptFile = File(transcriptDir, "transcript_${dateTime}.txt")
            
            transcriptFile.writeText(allResults.toString())
            
            val message = if (selectedLanguage == "es-ES") 
                "Transcripción guardada en: ${transcriptFile.absolutePath}" 
            else 
                "Transcript saved to: ${transcriptFile.absolutePath}"
                
            Toast.makeText(this, message, Toast.LENGTH_LONG).show()
            Log.d(TAG, "Transcript saved to: ${transcriptFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Error saving transcript: ${e.message}")
            Toast.makeText(this, "Error saving transcript", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateButtonState(isActive: Boolean) {
        startButton.isEnabled = !isActive
        stopButton.isEnabled = isActive
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == RECORD_AUDIO_PERMISSION_REQUEST) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                startSession()
            } else {
                val message = if (selectedLanguage == "es-ES") {
                    "Permisos requeridos para usar la aplicación"
                } else {
                    "Permissions required to use the app"
                }
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        stopSession()
    }

    override fun onDestroy() {
        super.onDestroy()
        stopSession()
        speechRecognizerManager.destroyCurrentRecognizer()
        handler.removeCallbacksAndMessages(null)
    }
    
    // SpeechRecognitionListener implementation
    override fun onRecognitionStarted() {
        Log.d(TAG, "Recognition started")
        runOnUiThread {
            partialResultTextView.text = "Listening..."
        }
    }
    
    override fun onPartialResult(text: String) {
        Log.d(TAG, "Partial result: $text")
        runOnUiThread {
            partialResultTextView.text = text
        }
    }
    
    override fun onResult(text: String) {
        Log.d(TAG, "Final result: $text")
        
        // Get current timestamp
        val timestamp = getCurrentTimestamp()
        val elapsedTime = getFormattedElapsedTime()
        
        // Add to results
        if (allResults.isNotEmpty()) {
            allResults.append("\n\n")
        }
        allResults.append("$timestamp ($elapsedTime)\n$text")
        
        // Update UI
        runOnUiThread {
            resultTextView.text = allResults.toString()
            partialResultTextView.text = ""
        }
    }
    
    override fun onError(errorMessage: String) {
        Log.e(TAG, "Recognition error: $errorMessage")
        runOnUiThread {
            partialResultTextView.text = "Error: $errorMessage"
            Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
        }
    }
}