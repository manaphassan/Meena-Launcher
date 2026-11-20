package my.hana.meena.data.system

import android.content.SharedPreferences

/** Minimal key-value store, so [SecureFields] can be unit-tested without Android. */
interface PlainStore {
    fun get(key: String): String?
    fun put(key: String, value: String): Boolean
    fun remove(key: String): Boolean
}

interface FieldCipher {
    fun encrypt(plain: String): String
    /** Returns null when the text can't be decrypted (wrong or missing key, corrupt data). */
    fun decryptOrNull(encoded: String): String?
}

sealed class SecureRead {
    /** [text] is null when nothing is stored. */
    data class Value(val text: String?) : SecureRead()
    /** An encrypted copy exists but can't be decrypted. Callers must not overwrite it. */
    object Unreadable : SecureRead()
}

/**
 * Reads and writes one encrypted field, moving any plain copy left by older builds.
 * The plain copy is erased only after the encrypted copy reads back identical, so a failed
 * move keeps the original and retries on the next read.
 */
object SecureFields {

    fun read(store: PlainStore, cipher: FieldCipher, encryptedKey: String, legacyPlainKey: String): SecureRead {
        val encrypted = store.get(encryptedKey)
        if (encrypted != null) {
            val text = cipher.decryptOrNull(encrypted) ?: return SecureRead.Unreadable
            if (store.get(legacyPlainKey) != null) store.remove(legacyPlainKey)
            return SecureRead.Value(text)
        }

        val legacy = store.get(legacyPlainKey) ?: return SecureRead.Value(null)
        val moved = try {
            store.put(encryptedKey, cipher.encrypt(legacy)) &&
                store.get(encryptedKey)?.let { cipher.decryptOrNull(it) } == legacy
        } catch (_: Exception) {
            false
        }
        if (moved) {
            store.remove(legacyPlainKey)
        } else {
            // Drop a bad encrypted copy so the next read retries from the plain one.
            store.remove(encryptedKey)
        }
        return SecureRead.Value(legacy)
    }

    /** Stores [value] encrypted, or removes the field when [value] is null or empty. */
    fun write(store: PlainStore, cipher: FieldCipher, encryptedKey: String, legacyPlainKey: String, value: String?): Boolean {
        if (value.isNullOrEmpty()) {
            val removedEncrypted = store.remove(encryptedKey)
            val removedPlain = store.remove(legacyPlainKey)
            return removedEncrypted && removedPlain
        }
        val written = try {
            store.put(encryptedKey, cipher.encrypt(value))
        } catch (_: Exception) {
            false
        }
        if (written) store.remove(legacyPlainKey)
        return written
    }
}

/** Uses commit() so callers know each write reached disk before erasing anything. */
// The KTX edit {} returns nothing, and these callers need commit()'s result.
@android.annotation.SuppressLint("UseKtx")
class SharedPrefsStore(private val prefs: SharedPreferences) : PlainStore {
    override fun get(key: String): String? = prefs.getString(key, null)
    override fun put(key: String, value: String): Boolean = prefs.edit().putString(key, value).commit()
    override fun remove(key: String): Boolean = prefs.edit().remove(key).commit()
}
