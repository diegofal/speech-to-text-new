package com.example.myapplication.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import java.io.IOException
import java.security.GeneralSecurityException

/**
 * Utility class for securely storing sensitive data like API keys
 */
object SecurePreferences {
    private const val TAG = "SecurePreferences"
    private const val PREFERENCES_FILE = "secure_prefs"
    private const val KEY_OPENAI_API_KEY = "openai_api_key"
    
    private fun getEncryptedSharedPreferences(context: Context): SharedPreferences? {
        return try {
            val masterKey = MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
            
            EncryptedSharedPreferences.create(
                context,
                PREFERENCES_FILE,
                masterKey,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: GeneralSecurityException) {
            Log.e(TAG, "Error initializing encrypted shared preferences", e)
            null
        } catch (e: IOException) {
            Log.e(TAG, "Error initializing encrypted shared preferences", e)
            null
        }
    }
    
    /**
     * Save the OpenAI API key securely
     */
    fun saveApiKey(context: Context, apiKey: String) {
        val preferences = getEncryptedSharedPreferences(context) ?: run {
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        }
        
        preferences.edit().apply {
            putString(KEY_OPENAI_API_KEY, apiKey)
            apply()
        }
    }
    
    /**
     * Retrieve the saved OpenAI API key
     */
    fun getApiKey(context: Context): String? {
        val preferences = getEncryptedSharedPreferences(context) ?: run {
            // Fallback to regular shared preferences if encryption fails
            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        }
        
        return preferences.getString(KEY_OPENAI_API_KEY, null)
    }
    
    /**
     * Check if the API key is configured
     */
    fun isApiKeyConfigured(context: Context): Boolean {
        return !getApiKey(context).isNullOrBlank()
    }
    
    /**
     * Clear all saved preferences
     */
    fun clearAll(context: Context) {
        val preferences = getEncryptedSharedPreferences(context) ?: run {
            context.getSharedPreferences(PREFERENCES_FILE, Context.MODE_PRIVATE)
        }
        
        preferences.edit().clear().apply()
    }
}
