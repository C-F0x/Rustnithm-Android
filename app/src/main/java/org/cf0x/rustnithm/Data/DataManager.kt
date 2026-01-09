package org.cf0x.rustnithm.Data

import android.content.Context
import android.graphics.ImageDecoder
import android.net.Uri
import androidx.core.graphics.toColorInt
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.palette.graphics.Palette
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class SkinConfig(
    val skinName: String = "Default",
    val seedColor: String = "#FF6750A4",
    val themeMode: Int = 2,
    val airWeight: Float = 0.5f,
    val multiA: Float = 0.15f,
    val multiS: Float = 0.15f
)
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "rustnithm_settings")

class DataManager(context: Context) : ViewModel() {

    private val dataStore = context.dataStore

    private object PreferenceKeys {
        val THEME_MODE = intPreferencesKey("theme_mode")
        val ENABLE_VIBRATION = booleanPreferencesKey("enable_vibration")
        val PERCENT_PAGE = floatPreferencesKey("percent_page")
        val MULTI_A = floatPreferencesKey("multi_a")
        val MULTI_S = floatPreferencesKey("multi_s")
        val SEED_COLOR = longPreferencesKey("seed_color")
        val BACKGROUND_IMAGE_PATH = stringPreferencesKey("background_image_path")
        val TARGET_IP = stringPreferencesKey("target_ip")
        val TARGET_PORT = stringPreferencesKey("target_port")
    }

    private companion object {
        const val DEFAULT_THEME_MODE = 2
        const val DEFAULT_ENABLE_VIBRATION = true
        const val DEFAULT_PERCENT_PAGE = 0.5f
        const val DEFAULT_MULTI_A = 0.15f
        const val DEFAULT_MULTI_S = 0.15f
        const val DEFAULT_SEED_COLOR = 0xFF6750A4L
    }

    val targetIp: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.TARGET_IP] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val targetPort: StateFlow<String> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.TARGET_PORT] ?: "" }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "")

    val backgroundImage: StateFlow<String?> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.BACKGROUND_IMAGE_PATH] }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val themeMode: StateFlow<Int> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.THEME_MODE] ?: DEFAULT_THEME_MODE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_THEME_MODE)

    val enableVibration: StateFlow<Boolean> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.ENABLE_VIBRATION] ?: DEFAULT_ENABLE_VIBRATION }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_ENABLE_VIBRATION)

    val percentPage: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.PERCENT_PAGE] ?: DEFAULT_PERCENT_PAGE }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_PERCENT_PAGE)

    val multiA: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.MULTI_A] ?: DEFAULT_MULTI_A }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_MULTI_A)

    val multiS: StateFlow<Float> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.MULTI_S] ?: DEFAULT_MULTI_S }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_MULTI_S)

    val seedColor: StateFlow<Long> = dataStore.data
        .map { preferences -> preferences[PreferenceKeys.SEED_COLOR] ?: DEFAULT_SEED_COLOR }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), DEFAULT_SEED_COLOR)

    fun updateBackgroundAndPalette(uri: Uri, context: Context) {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences[PreferenceKeys.BACKGROUND_IMAGE_PATH] = uri.toString()
            }

            try {
                val bitmap =
                    ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri))

                Palette.from(bitmap).generate { palette ->
                    val colorInt = palette?.getVibrantColor(
                        palette.getDominantColor(DEFAULT_SEED_COLOR.toInt())
                    )
                    colorInt?.let {
                        updateSeedColor(it.toLong())
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
    fun updateTargetIp(ip: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.TARGET_IP] = ip }
        }
    }

    fun updateTargetPort(port: String) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.TARGET_PORT] = port }
        }
    }

    fun resetBackgroundAndSkin() {
        viewModelScope.launch {
            dataStore.edit { preferences ->
                preferences.remove(PreferenceKeys.BACKGROUND_IMAGE_PATH)
                preferences[PreferenceKeys.SEED_COLOR] = DEFAULT_SEED_COLOR
            }
        }
    }
    fun applySkinConfig(jsonString: String) {
        try {
            val config = Json.decodeFromString<SkinConfig>(jsonString)
            val colorLong = config.seedColor.toColorInt().toLong()
            viewModelScope.launch {
                updateSeedColor(colorLong)
                updateThemeMode(config.themeMode)
                updatePercentPage(config.airWeight)
                updateMultiA(config.multiA)
                updateMultiS(config.multiS)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateThemeMode(mode: Int) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.THEME_MODE] = mode }
        }
    }

    fun updateEnableVibration(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.ENABLE_VIBRATION] = enabled }
        }
    }

    fun updatePercentPage(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.PERCENT_PAGE] = value.coerceIn(0.1f, 0.9f) }
        }
    }

    fun updateMultiA(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.MULTI_A] = value.coerceIn(0f, 0.5f) }
        }
    }

    fun updateMultiS(value: Float) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.MULTI_S] = value.coerceIn(0f, 0.5f) }
        }
    }

    fun updateSeedColor(value: Long) {
        viewModelScope.launch {
            dataStore.edit { it[PreferenceKeys.SEED_COLOR] = value }
        }
    }

    fun resetToDefaults() {
        viewModelScope.launch {
            dataStore.edit { it.clear() }
        }
    }

    class Factory(private val context: Context) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DataManager::class.java)) {
                return DataManager(context) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}