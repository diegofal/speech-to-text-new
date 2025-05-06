package com.example.myapplication.speech.api

import com.google.gson.annotations.SerializedName

/**
 * Data model for Whisper API response
 */
data class WhisperResponse(
    @SerializedName("text")
    val text: String,
    
    @SerializedName("task")
    val task: String? = null,
    
    @SerializedName("language")
    val language: String? = null,
    
    @SerializedName("duration")
    val duration: Double? = null,
    
    @SerializedName("segments")
    val segments: List<WhisperSegment>? = null
)

/**
 * Data model for individual segments in the response
 */
data class WhisperSegment(
    @SerializedName("id")
    val id: Int,
    
    @SerializedName("seek")
    val seek: Int,
    
    @SerializedName("start")
    val start: Double,
    
    @SerializedName("end")
    val end: Double,
    
    @SerializedName("text")
    val text: String,
    
    @SerializedName("tokens")
    val tokens: List<Int>? = null,
    
    @SerializedName("temperature")
    val temperature: Double? = null,
    
    @SerializedName("avg_logprob")
    val avgLogprob: Double? = null,
    
    @SerializedName("compression_ratio")
    val compressionRatio: Double? = null,
    
    @SerializedName("no_speech_prob")
    val noSpeechProb: Double? = null
)
