package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

class PreferencesManager private constructor(context: Context) {
    private val Context._sharedPreferences by preferencesDataStore(name = "preferences")
    private val sharedPreferences = context._sharedPreferences

    private val _encryptedSharedPrefs = context.let {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        EncryptedSharedPreferences.create(
            "${it.packageName}_encrypted_preferences",
            masterKeyAlias,
            it,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun getHasAppLock(): Flow<Boolean> = getPreference(PreferenceKeys.HAS_APP_LOCK, false)
    suspend fun setHasAppLock(value: Boolean) = setPreference(PreferenceKeys.HAS_APP_LOCK, value)

    fun getHasBiometricAppLock(): Flow<Boolean> =
        getPreference(PreferenceKeys.HAS_BIOMETRIC_APP_LOCK, false)

    suspend fun setHasBiometricAppLock(value: Boolean) =
        setPreference(PreferenceKeys.HAS_BIOMETRIC_APP_LOCK, value)

    fun getAppLockPasscode(): String? = _encryptedSharedPrefs.getString("APP_LOCK_PASSCODE", null)
    fun setAppLockPasscode(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("APP_LOCK_PASSCODE", value).commit()

    fun getLoggedInServer(): String? = _encryptedSharedPrefs.getString("LOGGED_IN_SERVER", null)
    fun setLoggedInServer(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("LOGGED_IN_SERVER", value).commit()

    fun getLoggedInUser(): String? = _encryptedSharedPrefs.getString("LOGGED_IN_USER", null)
    fun setLoggedInUser(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("LOGGED_IN_USER", value).commit()

    fun getLoggedInPassword(): String? = _encryptedSharedPrefs.getString("LOGGED_IN_PASSWORD", null)
    fun setLoggedInPassword(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("LOGGED_IN_PASSWORD", value).commit()

    fun getMasterPassword(): String? = _encryptedSharedPrefs.getString("MASTER_KEY", null)
    fun setMasterPassword(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("MASTER_KEY", value).commit()

    fun getCSEv1Keychain(): String? = _encryptedSharedPrefs.getString("CSE_V1_KEYCHAIN", null)
    fun setCSEv1Keychain(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("CSE_V1_KEYCHAIN", value).commit()

    fun getServerSettings(): ServerSettings =
        _encryptedSharedPrefs.getString("SERVER_SETTINGS", null)?.let {
            Json.decodeFromString(it)
        } ?: ServerSettings()

    fun setServerSettings(value: ServerSettings?): Boolean =
        _encryptedSharedPrefs.edit().putString("SERVER_SETTINGS", value?.let {
            Json.encodeToString(it)
        }).commit()

    fun getShowIcons(): Flow<Boolean> = getPreference(PreferenceKeys.SHOW_ICONS, false)
    suspend fun setShowIcons(value: Boolean) = setPreference(PreferenceKeys.SHOW_ICONS, value)

    fun getStartScreen(): Flow<String> =
        getPreference(PreferenceKeys.START_SCREEN, NCPScreen.Passwords.name)

    suspend fun setStartScreen(value: String) = setPreference(PreferenceKeys.START_SCREEN, value)

    private fun <T> getPreference(key: Preferences.Key<T>, defaultValue: T): Flow<T> =
        sharedPreferences.data
            .catch { exception ->
                if (exception is IOException) {
                    emit(emptyPreferences())
                } else {
                    throw exception
                }
            }
            .map { preferences ->
                preferences[key] ?: defaultValue
            }

    private suspend fun <T> setPreference(key: Preferences.Key<T>, value: T) {
        sharedPreferences.edit { preferences ->
            preferences[key] = value
        }
    }

    companion object {
        private var instance: PreferencesManager? = null

        fun getInstance(context: Context): PreferencesManager {
            if (instance == null) instance = PreferencesManager(context)
            return instance as PreferencesManager
        }

        private object PreferenceKeys {
            val SHOW_ICONS = booleanPreferencesKey("SHOW_ICONS")
            val START_SCREEN = stringPreferencesKey("START_SCREEN")
            val HAS_APP_LOCK = booleanPreferencesKey("HAS_APP_LOCK")
            val HAS_BIOMETRIC_APP_LOCK = booleanPreferencesKey("HAS_BIOMETRIC_APP_LOCK")
        }
    }
}