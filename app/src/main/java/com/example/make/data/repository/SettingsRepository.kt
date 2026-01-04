package com.example.make.data.repository

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

class SettingsRepository(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("make_settings", Context.MODE_PRIVATE)
    
    private val securePrefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
            
        EncryptedSharedPreferences.create(
            context,
            "secure_make_settings",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _keywords = MutableStateFlow(loadKeywords())
    val keywords = _keywords.asStateFlow()

    private val _accounts = MutableStateFlow(loadAccounts())
    val accounts = _accounts.asStateFlow()

    private val _searchEngines = MutableStateFlow(loadSearchEngines())
    val searchEngines = _searchEngines.asStateFlow()

    private val _enabledEngineNames = MutableStateFlow(loadEnabledEngineNames())
    val enabledEngineNames = _enabledEngineNames.asStateFlow()

    fun getGeminiKey(): String = securePrefs.getString("secure_gemini_key", "") ?: ""
    fun saveGeminiKey(key: String) = securePrefs.edit { putString("secure_gemini_key", key) }

    fun getChatGPTKey(): String = securePrefs.getString("secure_chatgpt_key", "") ?: ""
    fun saveChatGPTKey(key: String) = securePrefs.edit { putString("secure_chatgpt_key", key) }

    fun getGmailToken(): String = securePrefs.getString("secure_gmail_token", "") ?: ""
    fun saveGmailToken(token: String) = securePrefs.edit { putString("secure_gmail_token", token) }

    private fun loadKeywords(): List<String> {
        val set = prefs.getStringSet("keywords", emptySet()) ?: emptySet()
        if (set.isEmpty()) return listOf("AI", "Semiconductor", "Economy")
        return set.toList()
    }

    private fun loadAccounts(): List<com.example.make.data.model.EmailAccountConfig> {
        val set = prefs.getStringSet("accounts", emptySet()) ?: emptySet()
        if (set.isEmpty()) return emptyList()
        return set.mapNotNull { deserializeAccount(it) }
    }

    private fun loadSearchEngines(): List<Pair<String, String>> {
        val set = prefs.getStringSet("engines", emptySet()) ?: emptySet()
        if (set.isEmpty()) return listOf(
            "Google" to "https://google.com/search?q=",
            "Naver" to "https://search.naver.com/search.naver?query=",
            "Daum" to "https://search.daum.net/search?w=tot&q="
        )
        return set.map { 
            val p = it.split("|")
            if (p.size == 2) p[0] to p[1] else "Google" to "https://google.com/search?q="
        }
    }

    private fun loadEnabledEngineNames(): Set<String> {
        val set = prefs.getStringSet("enabled_engines", null)
        if (set == null) return setOf("Google") // Default
        return set
    }

    fun addKeyword(keyword: String) {
        val current = _keywords.value.toMutableSet()
        current.add(keyword)
        prefs.edit { putStringSet("keywords", current) }
        _keywords.value = current.toList()
    }

    fun removeKeyword(keyword: String) {
        val current = _keywords.value.toMutableSet()
        current.remove(keyword)
        prefs.edit { putStringSet("keywords", current) }
        _keywords.value = current.toList()
    }

    fun addSearchEngine(name: String, url: String) {
        val current = _searchEngines.value.toMutableList()
        current.add(name to url)
        saveEngines(current)
    }

    fun removeSearchEngine(name: String) {
        val current = _searchEngines.value.filter { it.first != name }
        saveEngines(current)
    }

    fun toggleEngine(name: String, isEnabled: Boolean) {
        val current = _enabledEngineNames.value.toMutableSet()
        if (isEnabled) {
            current.add(name)
        } else {
            if (current.size > 1) { // Prevent disabling all
                current.remove(name)
            }
        }
        _enabledEngineNames.value = current
        prefs.edit { putStringSet("enabled_engines", current) }
    }

    private fun saveEngines(list: List<Pair<String, String>>) {
        _searchEngines.value = list
        val set = list.map { "${it.first}|${it.second}" }.toSet()
        prefs.edit { putStringSet("engines", set) }
    }


    fun addAccount(account: com.example.make.data.model.EmailAccountConfig) {
        val current = _accounts.value.toMutableList()
        current.add(account)
        saveAccounts(current)
    }

    fun removeAccount(id: String) {
        val current = _accounts.value.filter { it.id != id }
        saveAccounts(current)
    }

    fun toggleAccount(id: String, isEnabled: Boolean) {
        val current = _accounts.value.map { 
            if (it.id == id) it.copy(isEnabled = isEnabled) else it 
        }
        saveAccounts(current)
    }

    private fun saveAccounts(list: List<com.example.make.data.model.EmailAccountConfig>) {
        _accounts.value = list
        val set = list.map { serializeAccount(it) }.toSet()
        prefs.edit { putStringSet("accounts", set) }
    }

    private fun serializeAccount(account: com.example.make.data.model.EmailAccountConfig): String {
        return "${account.id}|${account.email}|${account.provider}|${account.isEnabled}"
    }

    private fun deserializeAccount(str: String): com.example.make.data.model.EmailAccountConfig? {
        val parts = str.split("|")
        if (parts.size != 4) return null
        return com.example.make.data.model.EmailAccountConfig(parts[0], parts[1], parts[2], parts[3].toBoolean())
    }
}
