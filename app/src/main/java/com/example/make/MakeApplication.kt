package com.example.make

import android.app.Application
import androidx.room.Room
import com.example.make.data.local.AppDatabase
import okhttp3.MediaType.Companion.toMediaType
import kotlinx.serialization.json.Json
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class MakeApplication : Application() {
    lateinit var database: AppDatabase
    
    // In a real app, use Hilt or Koin
    val gmailApi by lazy {
        Retrofit.Builder()
            .baseUrl("https://gmail.googleapis.com/")
            .addConverterFactory(Json.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(com.example.make.data.remote.GmailApi::class.java)
    }

    val geminiDataSource by lazy {
        val userKey = settingsRepository.getGeminiKey()
        val finalKey = if (userKey.isNotBlank()) userKey else BuildConfig.GEMINI_API_KEY
        com.example.make.data.remote.GeminiDataSource(apiKey = finalKey) 
    }

    val mailRepository by lazy {
        com.example.make.data.repository.MailRepository(
            gmailApi, 
            geminiDataSource, 
            database.intelligenceDao()
        )
    }

    fun getGmailAccessToken(): String? {
        val token = settingsRepository.getGmailToken()
        return if (token.isNotBlank()) token else null
    }

    val aiIntelligenceService by lazy {
        com.example.make.data.service.AiIntelligenceService(
            geminiDataSource,
            database.intelligenceDao()
        )
    }

    val settingsRepository by lazy {
        com.example.make.data.repository.SettingsRepository(applicationContext)
    }

    override fun onCreate() {
        super.onCreate()
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "make-database"
        )
        .fallbackToDestructiveMigration()
        .build()
    }
}
