package com.readwise.ai.service

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.readwise.ai.model.AIProvider
import com.readwise.core.database.dao.AIConfigDao
import com.readwise.core.database.entity.AIConfigEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages AI service configurations
 * Handles secure storage of API keys and configuration settings
 */
@Singleton
class AIConfigManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val aiConfigDao: AIConfigDao
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs: SharedPreferences = EncryptedSharedPreferences.create(
        context,
        "ai_config_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    companion object {
        private const val KEY_DEFAULT_PROVIDER = "default_provider"
        private const val KEY_API_KEY_PREFIX = "api_key_"
        private const val KEY_BASE_URL_PREFIX = "base_url_"
    }

    /**
     * Get default provider
     */
    fun getDefaultProvider(): AIProvider {
        val providerName = sharedPrefs.getString(KEY_DEFAULT_PROVIDER, AIProvider.OPENAI.name)
        return try {
            AIProvider.valueOf(providerName ?: AIProvider.OPENAI.name)
        } catch (e: Exception) {
            AIProvider.OPENAI
        }
    }

    /**
     * Set default provider
     */
    fun setDefaultProvider(provider: AIProvider) {
        sharedPrefs.edit()
            .putString(KEY_DEFAULT_PROVIDER, provider.name)
            .apply()
    }

    /**
     * Get API key for provider (from secure storage)
     */
    fun getApiKey(provider: AIProvider): String? {
        return sharedPrefs.getString("${KEY_API_KEY_PREFIX}${provider.name}", null)
    }

    /**
     * Save API key for provider (encrypted storage)
     */
    fun saveApiKey(provider: AIProvider, apiKey: String) {
        sharedPrefs.edit()
            .putString("${KEY_API_KEY_PREFIX}${provider.name}", apiKey)
            .apply()
    }

    /**
     * Remove API key for provider
     */
    fun removeApiKey(provider: AIProvider) {
        sharedPrefs.edit()
            .remove("${KEY_API_KEY_PREFIX}${provider.name}")
            .apply()
    }

    /**
     * Get custom base URL for provider
     */
    fun getBaseUrl(provider: AIProvider): String? {
        return sharedPrefs.getString("${KEY_BASE_URL_PREFIX}${provider.name}", null)
    }

    /**
     * Set custom base URL for provider
     */
    fun setBaseUrl(provider: AIProvider, url: String?) {
        if (url != null) {
            sharedPrefs.edit()
                .putString("${KEY_BASE_URL_PREFIX}${provider.name}", url)
                .apply()
        } else {
            sharedPrefs.edit()
                .remove("${KEY_BASE_URL_PREFIX}${provider.name}")
                .apply()
        }
    }

    /**
     * Save configuration to database
     */
    suspend fun saveConfig(config: AIConfigEntity) {
        aiConfigDao.insert(config)
    }

    /**
     * Get all saved configurations
     */
    fun getAllConfigs(): Flow<List<AIConfigEntity>> {
        return aiConfigDao.getAllConfigs()
    }

    /**
     * Get configuration by ID
     */
    suspend fun getConfigById(id: String): AIConfigEntity? {
        return aiConfigDao.getConfigById(id)
    }

    /**
     * Get default configuration
     */
    fun getDefaultConfig(): Flow<AIConfigEntity?> {
        return aiConfigDao.getDefaultConfig()
    }

    /**
     * Delete configuration
     */
    suspend fun deleteConfig(config: AIConfigEntity) {
        aiConfigDao.delete(config)
    }

    /**
     * Set as default configuration
     */
    suspend fun setAsDefault(config: AIConfigEntity) {
        // Unset current default
        aiConfigDao.updateDefaultStatus(false)
        // Set new default
        aiConfigDao.insert(config.copy(isDefault = true))
    }

    /**
     * Check if any provider is configured
     */
    fun hasAnyConfig(): Boolean {
        return sharedPrefs.all.keys.any { it.startsWith(KEY_API_KEY_PREFIX) }
    }

    /**
     * Get list of configured providers
     */
    fun getConfiguredProviders(): List<AIProvider> {
        return AIProvider.values().filter { provider ->
            getApiKey(provider) != null
        }
    }

    /**
     * Clear all stored API keys
     */
    fun clearAllApiKeys() {
        AIProvider.values().forEach { provider ->
            removeApiKey(provider)
        }
    }
}
