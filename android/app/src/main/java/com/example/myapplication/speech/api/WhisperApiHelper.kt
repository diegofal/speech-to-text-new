package com.example.myapplication.speech.api

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException

/**
 * Helper class for working with the Whisper API
 */
class WhisperApiHelper(private val context: Context) {
    companion object {
        private const val TAG = "WhisperApiHelper"
    }
    
    /**
     * Transcribe audio using the OpenAI Whisper API
     */
    suspend fun transcribeAudio(
        audioFilePath: String,
        language: String? = null,
        apiKey: String
    ): Result<String> {
        return try {
            val audioFile = File(audioFilePath)
            if (!audioFile.exists()) {
                return Result.failure(IOException("Audio file not found"))
            }
            
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "file",
                    audioFile.name,
                    audioFile.asRequestBody("audio/*".toMediaTypeOrNull())
                )
                .addFormDataPart("model", "whisper-1")
                .apply {
                    language?.let {
                        addFormDataPart("language", it)
                    }
                    addFormDataPart("response_format", "text")
                }
                .build()
            
            val response = RetrofitClient.whisperApiService.transcribeAudio(
                authorization = "Bearer $apiKey",
                file = requestBody.parts[0],
                model = requestBody.parts[1].body,
                language = requestBody.parts.getOrNull(2)?.body,
                responseFormat = requestBody.parts.last().body
            )
            
            if (response.isSuccessful && response.body() != null) {
                Result.success(response.body()!!)
            } else {
                val errorBody = response.errorBody()?.string() ?: "Unknown error"
                Result.failure(IOException("API Error: $errorBody"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
