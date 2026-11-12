package my.hana.meena.data.system

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Manages encrypted storage of sensitive user API keys using Android Keystore-backed AES-256 GCM.
 * Keys are generated and stored hardware-backed where supported, never in plain DataStore,
 * never logged, and excluded from backup per Meena UI Hard Rule 6.
 */
object KeystoreSecretsManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "meena_secrets_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_NAME = "meena_encrypted_secrets"
    private const val PREF_GEMINI_KEY = "enc_gemini_api_key"

    private val _geminiKeyVersion = MutableStateFlow(0)

    /** Bumps whenever the Gemini key is saved or cleared, so screens can react without polling the Keystore. */
    val geminiKeyVersion: StateFlow<Int> = _geminiKeyVersion.asStateFlow()

    private fun getOrCreateSecretKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEYSTORE).apply { load(null) }
        if (keyStore.containsAlias(KEY_ALIAS)) {
            val entry = keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
            if (entry != null) {
                return entry.secretKey
            }
        }

        val keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEYSTORE)
        val spec = KeyGenParameterSpec.Builder(
            KEY_ALIAS,
            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
        )
            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
            .setKeySize(256)
            .setRandomizedEncryptionRequired(true)
            .build()
        keyGenerator.init(spec)
        return keyGenerator.generateKey()
    }

    fun encrypt(plainText: String): String {
        if (plainText.isEmpty()) return ""
        val secretKey = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + cipherText.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(cipherText, 0, combined, iv.size, cipherText.size)
        return Base64.encodeToString(combined, Base64.NO_WRAP)
    }

    fun decrypt(encodedText: String): String = decryptOrNull(encodedText) ?: ""

    /** Like [decrypt], but returns null on failure so callers can tell "unreadable" from "empty". */
    fun decryptOrNull(encodedText: String): String? {
        if (encodedText.isEmpty()) return ""
        try {
            val combined = Base64.decode(encodedText, Base64.NO_WRAP)
            if (combined.size < GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            val cipherTextSize = combined.size - GCM_IV_LENGTH
            val cipherText = ByteArray(cipherTextSize)
            System.arraycopy(combined, GCM_IV_LENGTH, cipherText, 0, cipherTextSize)

            val secretKey = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, spec)
            val decryptedBytes = cipher.doFinal(cipherText)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            return null
        }
    }

    val fieldCipher: FieldCipher = object : FieldCipher {
        override fun encrypt(plain: String): String = KeystoreSecretsManager.encrypt(plain)
        override fun decryptOrNull(encoded: String): String? = KeystoreSecretsManager.decryptOrNull(encoded)
    }

    fun saveGeminiApiKey(context: Context, key: String) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (key.isBlank()) {
            prefs.edit().remove(PREF_GEMINI_KEY).apply()
        } else {
            val encrypted = encrypt(key.trim())
            prefs.edit().putString(PREF_GEMINI_KEY, encrypted).apply()
        }
        _geminiKeyVersion.update { it + 1 }
    }

    fun getGeminiApiKey(context: Context): String {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val encrypted = prefs.getString(PREF_GEMINI_KEY, null) ?: return ""
        return decrypt(encrypted)
    }

    fun hasGeminiApiKey(context: Context): Boolean {
        return getGeminiApiKey(context).isNotBlank()
    }

    fun clearGeminiApiKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit().remove(PREF_GEMINI_KEY).apply()
        _geminiKeyVersion.update { it + 1 }
    }

    /**
     * Moves a Gemini key that older builds kept in plain settings into encrypted storage.
     * Returns true when the plain copy is safe to erase: the key was already stored encrypted,
     * or it was just stored and reads back intact.
     */
    fun moveLegacyGeminiKey(context: Context, legacyKey: String): Boolean {
        if (legacyKey.isBlank()) return true
        return try {
            if (hasGeminiApiKey(context)) return true
            val key = legacyKey.trim()
            val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            // commit(), not apply(): the plain copy is erased right after, so this write must be on disk first.
            val written = prefs.edit().putString(PREF_GEMINI_KEY, encrypt(key)).commit()
            _geminiKeyVersion.update { it + 1 }
            written && getGeminiApiKey(context) == key
        } catch (_: Exception) {
            false
        }
    }
}
