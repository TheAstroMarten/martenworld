package com.martenworld.app.data.connection

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/** API keys are kept in Keystore-backed encrypted preferences and never exported. */
@Singleton
class ApiKeyStore @Inject constructor(@ApplicationContext context: Context) {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        EncryptedSharedPreferences.create(
            context,
            "api_keys",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    fun get(connectionId: String): String? = prefs.getString(connectionId, null)?.takeIf { it.isNotBlank() }

    fun set(connectionId: String, key: String?) {
        prefs.edit {
            if (key.isNullOrBlank()) remove(connectionId) else putString(connectionId, key)
        }
    }

    fun remove(connectionId: String) = set(connectionId, null)
}
