package io.github.kanggod9.diettracker.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import io.github.kanggod9.diettracker.integration.GatewayConnection
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Stores only the app-to-gateway access token. The OpenAI and USDA provider keys must remain gateway secrets.
 * Android backup is disabled, so this device-bound ciphertext is not copied to another device.
 */
class SecureConfigStore(context: Context) {
    private val preferences =
        context.applicationContext.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun saveGateway(endpoint: String, accessToken: String) {
        val validated = GatewayConnection(endpoint.trim(), accessToken.trim())
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey(createIfMissing = true))
        cipher.updateAAD(ASSOCIATED_DATA)
        val encrypted = cipher.doFinal(validated.accessToken.toByteArray(Charsets.UTF_8))
        preferences.edit()
            .putString(KEY_ENDPOINT, validated.endpoint)
            .putString(KEY_TOKEN_IV, Base64.encodeToString(cipher.iv, Base64.NO_WRAP))
            .putString(KEY_TOKEN_CIPHERTEXT, Base64.encodeToString(encrypted, Base64.NO_WRAP))
            .apply()
    }

    fun connection(): GatewayConnection? {
        val endpoint = preferences.getString(KEY_ENDPOINT, null) ?: return null
        val iv = preferences.getString(KEY_TOKEN_IV, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        } ?: return null
        val encrypted = preferences.getString(KEY_TOKEN_CIPHERTEXT, null)?.let {
            runCatching { Base64.decode(it, Base64.NO_WRAP) }.getOrNull()
        } ?: return null

        return runCatching {
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, secretKey(createIfMissing = false), GCMParameterSpec(128, iv))
            cipher.updateAAD(ASSOCIATED_DATA)
            val token = cipher.doFinal(encrypted).toString(Charsets.UTF_8)
            GatewayConnection(endpoint, token)
        }.getOrElse {
            clearGateway()
            null
        }
    }

    fun configuredEndpoint(): String? = preferences.getString(KEY_ENDPOINT, null)
    fun isConfigured(): Boolean = connection() != null

    fun clearGateway() {
        preferences.edit()
            .remove(KEY_ENDPOINT)
            .remove(KEY_TOKEN_IV)
            .remove(KEY_TOKEN_CIPHERTEXT)
            .apply()
    }

    private fun secretKey(createIfMissing: Boolean): SecretKey {
        val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        (keyStore.getKey(KEY_ALIAS, null) as? SecretKey)?.let { return it }
        check(createIfMissing) { "Device gateway key is missing" }

        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER)
        generator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            )
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        return generator.generateKey()
    }

    companion object {
        private const val PREFERENCES_NAME = "secure-gateway"
        private const val KEY_ENDPOINT = "endpoint"
        private const val KEY_TOKEN_IV = "token_iv"
        private const val KEY_TOKEN_CIPHERTEXT = "token_ciphertext"
        private const val KEY_ALIAS = "diet_tracker_gateway_token_v1"
        private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private val ASSOCIATED_DATA = "diet-tracker-gateway-token-v1".toByteArray(Charsets.UTF_8)
    }
}
