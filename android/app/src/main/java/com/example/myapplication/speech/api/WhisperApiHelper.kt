package com.example.myapplication.speech.api

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.util.Log
import com.example.myapplication.utils.SecurePreferences
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Helper class for working with the Whisper API
 */
class WhisperApiHelper(private val context: Context) {
    companion object {
        private const val TAG = "WhisperApiHelper"
        private const val DEFAULT_MODEL = "whisper-1"
    }
    
    init {
        // Store the API key initially if it's not already set
        // This would normally be done through a settings screen
        if (!SecurePreferences.isApiKeyConfigured(context)) {
            val defaultApiKey = "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"
            SecurePreferences.saveApiKey(context, defaultApiKey)
            Log.d(TAG, "API key stored securely")
        }
    }
    
    /**
     * Transcribe audio using the OpenAI Whisper API
     */
    suspend fun transcribeAudio(audioFilePath: String, language: String? = null): Result<String> {
        return withContext(Dispatchers.IO) {
            try {
                Log.d(TAG, "Preparing to transcribe audio file: $audioFilePath")
                
                // Get the API key from secure storage
                val apiKey = SecurePreferences.getApiKey(context)
                if (apiKey.isNullOrBlank()) {
                    return@withContext Result.failure(
                        IllegalStateException("API key not configured")
                    )
                }
                
                // Ensure the file exists
                val audioFile = File(audioFilePath)
                if (!audioFile.exists()) {
                    return@withContext Result.failure(
                        IOException("Audio file not found: $audioFilePath")
                    )
                }
                
                // Convert the audio to MP3 if needed (Whisper requires MP3, WAV, WebM, or M4A)
                val processedFile = if (audioFilePath.endsWith(".3gp")) {
                    convertAudioToMp3(audioFile)
                } else {
                    audioFile
                }
                
                // Prepare the request parts
                val fileRequestBody = processedFile.asRequestBody("audio/mpeg".toMediaTypeOrNull())
                val filePart = MultipartBody.Part.createFormData("file", processedFile.name, fileRequestBody)
                
                // Set up other parameters
                val modelPart = DEFAULT_MODEL.toRequestBody("text/plain".toMediaTypeOrNull())
                val languagePart = language?.toRequestBody("text/plain".toMediaTypeOrNull())
                val authHeader = "Bearer $apiKey"
                
                // Make the API call
                Log.d(TAG, "Sending request to Whisper API")
                val response = RetrofitClient.whisperApiService.transcribeAudio(
                    authorization = authHeader,
                    file = filePart,
                    model = modelPart,
                    language = languagePart
                )
                
                // Process the response
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!.text
                    Log.d(TAG, "Transcription successful: ${result.take(50)}...")
                    Result.success(result)
                } else {
                    val errorBody = response.errorBody()?.string() ?: "Unknown error"
                    Log.e(TAG, "API Error: $errorBody")
                    Result.failure(IOException("API Error: $errorBody"))
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "Transcription failed", e)
                Result.failure(e)
            }
        }
    }
    
    /**
     * Converts a 3GP audio file to MP3 format using Android's media framework
     * Note: In a real app, you'd use a proper audio conversion library
     * This is a simplified example that would need to be enhanced for production
     */
    private suspend fun convertAudioToMp3(inputFile: File): File {
        return withContext(Dispatchers.IO) {
            try {
                // For demonstration - in a real app, implement proper conversion
                // This is just returning the original file as we don't have a full
                // audio conversion implementation in this example
                Log.w(TAG, "Audio conversion not fully implemented - using original file")
                
                // In a real app, you would:
                // 1. Use FFmpeg or another library to convert the audio format
                // 2. Save the converted audio to a new file
                // 3. Return the new file path
                
                // For now, we'll just return the original file
                inputFile
            } catch (e: Exception) {
                Log.e(TAG, "Audio conversion failed", e)
                throw e
            }
        }
    }
}
