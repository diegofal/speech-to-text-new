package com.example.myapplication.audio

import android.content.Context
import android.media.MediaPlayer
import android.util.Log
import java.io.File

class AudioPlayer(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    private var currentFile: File? = null
    private var onCompletionListener: (() -> Unit)? = null

    fun play(file: File, onComplete: () -> Unit = {}) {
        stop()
        currentFile = file
        onCompletionListener = onComplete

        try {
            mediaPlayer = MediaPlayer().apply {
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    onCompletionListener?.invoke()
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error playing audio", e)
            releasePlayer()
        }
    }

    fun pause() {
        mediaPlayer?.pause()
    }

    fun resume() {
        mediaPlayer?.start()
    }

    fun stop() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            reset()
            release()
        }
        mediaPlayer = null
        currentFile = null
    }

    fun isPlaying(): Boolean = mediaPlayer?.isPlaying ?: false

    fun getCurrentPosition(): Int = mediaPlayer?.currentPosition ?: 0

    fun getDuration(): Int = mediaPlayer?.duration ?: 0

    private fun releasePlayer() {
        mediaPlayer?.apply {
            reset()
            release()
        }
        mediaPlayer = null
        currentFile = null
    }

    companion object {
        private const val TAG = "AudioPlayer"
    }
} 