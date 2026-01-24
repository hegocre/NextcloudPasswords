package com.hegocre.nextcloudpasswords.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.hegocre.nextcloudpasswords.data.password.RequestedPassword
import com.hegocre.nextcloudpasswords.data.serversettings.ServerSettings
import com.hegocre.nextcloudpasswords.ui.NCPScreen
import com.hegocre.nextcloudpasswords.ui.theme.NCPTheme
import dev.spght.encryptedprefs.EncryptedSharedPreferences
import dev.spght.encryptedprefs.MasterKey
import dev.spght.encryptedprefs.MasterKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.io.IOException

class PreferencesManager private constructor(context: Context) {
    private val Context._sharedPreferences by preferencesDataStore(name = "preferences")
    private val sharedPreferences = context._sharedPreferences

    private var makingSharedPrefsAttempts = 0

    private fun makeEncryptedSharedPrefs(context: Context): SharedPreferences {
        val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
        return EncryptedSharedPreferences.create(
            context,
            "${context.packageName}_encrypted_preferences",
            MasterKey(context, masterKeyAlias),
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    private val _encryptedSharedPrefs = context.let {
        try {
            makingSharedPrefsAttempts += 1
            makeEncryptedSharedPrefs(it)
        } catch (e: Exception) {
            if (makingSharedPrefsAttempts > 1) throw e

            it.getSharedPreferences(
                "${context.packageName}_encrypted_preferences",
                Context.MODE_PRIVATE
            ).edit().clear().apply()

            makeEncryptedSharedPrefs(it)
        }
    }

    suspend fun clear(): Boolean {
        sharedPreferences.edit {
            it.clear()
        }
        return _encryptedSharedPrefs.edit().clear().commit()
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

    fun getMasterPassword(): String? = _encryptedSharedPrefs.getString("MASTER_KEY", null)
    fun setMasterPassword(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("MASTER_KEY", value).commit()

    fun getServers(): String? = _encryptedSharedPrefs.getString("SERVERS", null)
    fun setServers(servers: String): Boolean = _encryptedSharedPrefs.edit().putString("SERVERS", servers).commit()

    fun getCSEv1Keychain(): String? = _encryptedSharedPrefs.getString("CSE_V1_KEYCHAIN", null)
    fun setCSEv1Keychain(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("CSE_V1_KEYCHAIN", value).commit()

    fun getServerSettings(): ServerSettings = try {
        _encryptedSharedPrefs.getString("SERVER_SETTINGS", null)?.let {
            Json.decodeFromString(it)
        } ?: ServerSettings()
    } catch (_: Exception) {
        ServerSettings()
    }

    fun setServerSettings(value: ServerSettings?): Boolean =
        _encryptedSharedPrefs.edit().putString("SERVER_SETTINGS", value?.let {
            Json.encodeToString(it)
        }).commit()

    fun getSkipCertificateValidation(): Boolean =
        _encryptedSharedPrefs.getBoolean("SKIP_CERTIFICATE_VALIDATION", false)

    fun setSkipCertificateValidation(value: Boolean): Boolean =
        _encryptedSharedPrefs.edit().putBoolean("SKIP_CERTIFICATE_VALIDATION", value).commit()

    fun getUseInlineAutofill(): Boolean =
        _encryptedSharedPrefs.getBoolean("USE_INLINE_AUTOFILL", false)

    fun setUseInlineAutofill(value: Boolean): Boolean =
        _encryptedSharedPrefs.edit().putBoolean("USE_INLINE_AUTOFILL", value).commit()

    fun getPasswordGenerationOptions(): String? =
        _encryptedSharedPrefs.getString(
            "PASSWORD_GENERATION_OPTIONS",
            "${RequestedPassword.STRENGTH_STANDARD};true;true"
        )

    fun setPasswordGenerationOptions(value: String?): Boolean =
        _encryptedSharedPrefs.edit().putString("PASSWORD_GENERATION_OPTIONS", value).commit()

    fun getShowIcons(): Flow<Boolean> = getPreference(PreferenceKeys.SHOW_ICONS, false)
    suspend fun setShowIcons(value: Boolean) = setPreference(PreferenceKeys.SHOW_ICONS, value)

    fun getOrderBy(): Flow<String> = getPreference(PreferenceKeys.ORDER_BY, ORDER_BY_TITLE_ASCENDING)
    suspend fun setOrderBy(value: String) = setPreference(PreferenceKeys.ORDER_BY, value)

    fun getStartScreen(): Flow<String> =
        getPreference(PreferenceKeys.START_SCREEN, NCPScreen.Passwords.name)

    suspend fun setStartScreen(value: String) = setPreference(PreferenceKeys.START_SCREEN, value)

    fun getAppTheme(): Flow<String> = getPreference(PreferenceKeys.APP_THEME, NCPTheme.SYSTEM)
    suspend fun setAppTheme(value: String) = setPreference(PreferenceKeys.APP_THEME, value)

    fun getInstanceColor(): Flow<String> = getPreference(PreferenceKeys.INSTANCE_COLOR, "#745bca")
    suspend fun setInstanceColor(value: String) =
        setPreference(PreferenceKeys.INSTANCE_COLOR, value)

    fun getUseInstanceColor(): Flow<Boolean> =
        getPreference(PreferenceKeys.USE_NEXTCLOUD_INSTANCE_COLOR, false)

    suspend fun setUseInstanceColor(value: Boolean) =
        setPreference(PreferenceKeys.USE_NEXTCLOUD_INSTANCE_COLOR, value)

    fun getUseSystemDynamicColor(): Flow<Boolean> =
        getPreference(PreferenceKeys.USE_SYSTEM_DYNAMIC_COLOR, false)

    suspend fun setUseSystemDynamicColor(value: Boolean) =
        setPreference(PreferenceKeys.USE_SYSTEM_DYNAMIC_COLOR, value)

    fun getSearchByUsername(): Flow<Boolean> =
        getPreference(PreferenceKeys.SEARCH_BY_USERNAME, true)

    suspend fun setSearchByUsername(value: Boolean) =
        setPreference(PreferenceKeys.SEARCH_BY_USERNAME, value)

    fun getUseStrictUrlMatching(): Flow<Boolean> =
        getPreference(PreferenceKeys.USE_STRICT_URL_MATCHING, true)

    suspend fun setUseStrictUrlMatching(value: Boolean) =
        setPreference(PreferenceKeys.USE_STRICT_URL_MATCHING, value)

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
            synchronized(this) {
                if (instance == null) instance = PreferencesManager(context)
                return instance as PreferencesManager
            }
        }

        const val ORDER_BY_TITLE_ASCENDING = "ORDER_BY_TITLE_ASCENDING"
        const val ORDER_BY_TITLE_DESCENDING = "ORDER_BY_TITLE_DESCENDING"
        const val ORDER_BY_DATE_ASCENDING = "ORDER_BY_DATE_ASCENDING"
        const val ORDER_BY_DATE_DESCENDING = "ORDER_BY_DATE_DESCENDING"

        private object PreferenceKeys {
            val SHOW_ICONS = booleanPreferencesKey("SHOW_ICONS")
            val START_SCREEN = stringPreferencesKey("START_SCREEN")
            val HAS_APP_LOCK = booleanPreferencesKey("HAS_APP_LOCK")
            val HAS_BIOMETRIC_APP_LOCK = booleanPreferencesKey("HAS_BIOMETRIC_APP_LOCK")
            val APP_THEME = stringPreferencesKey("APP_THEME")
            val USE_NEXTCLOUD_INSTANCE_COLOR = booleanPreferencesKey("USE_NEXTCLOUD_INSTANCE_COLOR")
            val USE_SYSTEM_DYNAMIC_COLOR = booleanPreferencesKey("USE_SYSTEM_DYNAMIC_COLOR")
            val INSTANCE_COLOR = stringPreferencesKey("INSTANCE_COLOR")
            val SEARCH_BY_USERNAME = booleanPreferencesKey("SEARCH_BY_USERNAME")
            val USE_STRICT_URL_MATCHING = booleanPreferencesKey("USE_STRICT_URL_MATCHING")
            val ORDER_BY = stringPreferencesKey("ORDER_BY")
        }
    }
}