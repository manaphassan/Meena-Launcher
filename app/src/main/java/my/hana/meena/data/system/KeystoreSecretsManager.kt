package my.hana.meena.data.system

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Encrypts small values (portfolio holdings, sheet link) with an Android Keystore-backed
 * AES-256-GCM key. Values are never logged and are excluded from backup per hard rule 6.
 */
object KeystoreSecretsManager {
    private const val ANDROID_KEYSTORE = "AndroidKeyStore"
    private const val KEY_ALIAS = "meena_secrets_master_key"
    private const val TRANSFORMATION = "AES/GCM/NoPadding"
    private const val GCM_IV_LENGTH = 12
    private const val GCM_TAG_LENGTH = 128
    private const val PREFS_NAME = "meena_encrypted_secrets"
    // Only used to delete keys saved by builds that had the gemini brief.
    private const val PREF_LEGACY_GEMINI_KEY = "enc_gemini_api_key"

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

    /** The gemini brief was removed; delete any key it saved. */
    fun deleteLegacyGeminiKey(context: Context) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (prefs.contains(PREF_LEGACY_GEMINI_KEY)) {
            prefs.edit().remove(PREF_LEGACY_GEMINI_KEY).commit()
        }
    }
}
