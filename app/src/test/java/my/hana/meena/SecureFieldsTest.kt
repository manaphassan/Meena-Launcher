package my.hana.meena

import my.hana.meena.data.system.FieldCipher
import my.hana.meena.data.system.PlainStore
import my.hana.meena.data.system.SecureFields
import my.hana.meena.data.system.SecureRead
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureFieldsTest {

    private val enc = "enc_field"
    private val plain = "plain_field"

    private class FakeStore(
        val data: MutableMap<String, String> = mutableMapOf(),
        var failPuts: Boolean = false
    ) : PlainStore {
        override fun get(key: String): String? = data[key]
        override fun put(key: String, value: String): Boolean {
            if (failPuts) return false
            data[key] = value
            return true
        }
        override fun remove(key: String): Boolean {
            data.remove(key)
            return true
        }
    }

    private open class FakeCipher : FieldCipher {
        var unreadable = false
        var throwOnEncrypt = false
        override fun encrypt(plain: String): String {
            if (throwOnEncrypt) throw IllegalStateException("keystore unavailable")
            return "x:" + plain.reversed()
        }
        override fun decryptOrNull(encoded: String): String? =
            if (unreadable || !encoded.startsWith("x:")) null else encoded.removePrefix("x:").reversed()
    }

    @Test
    fun read_movesPlainCopyAndErasesItAfterReadBack() {
        val store = FakeStore(mutableMapOf(plain to "[{\"symbol\":\"bursa\"}]"))
        val result = SecureFields.read(store, FakeCipher(), enc, plain)

        assertEquals(SecureRead.Value("[{\"symbol\":\"bursa\"}]"), result)
        assertNull(store.get(plain))
        assertNotNull(store.get(enc))
        assertFalse(store.get(enc)!!.contains("bursa"))
    }

    @Test
    fun read_prefersEncryptedCopyAndDropsLeftoverPlain() {
        val cipher = FakeCipher()
        val store = FakeStore(mutableMapOf(enc to cipher.encrypt("new"), plain to "old"))

        assertEquals(SecureRead.Value("new"), SecureFields.read(store, cipher, enc, plain))
        assertNull(store.get(plain))
    }

    @Test
    fun read_unreadableEncryptedCopyIsKeptUntouched() {
        val cipher = FakeCipher()
        val stored = cipher.encrypt("holdings")
        cipher.unreadable = true
        val store = FakeStore(mutableMapOf(enc to stored, plain to "old"))

        assertEquals(SecureRead.Unreadable, SecureFields.read(store, cipher, enc, plain))
        assertEquals(stored, store.get(enc))
        assertEquals("old", store.get(plain))
    }

    @Test
    fun read_keepsPlainCopyWhenReadBackDoesNotMatch() {
        val cipher = object : FakeCipher() {
            override fun encrypt(plain: String): String = "x:corrupt"
        }
        val store = FakeStore(mutableMapOf(plain to "holdings"))

        assertEquals(SecureRead.Value("holdings"), SecureFields.read(store, cipher, enc, plain))
        assertEquals("holdings", store.get(plain))
        assertNull(store.get(enc))
    }

    @Test
    fun read_keepsPlainCopyWhenWriteFails() {
        val store = FakeStore(mutableMapOf(plain to "holdings"), failPuts = true)

        assertEquals(SecureRead.Value("holdings"), SecureFields.read(store, FakeCipher(), enc, plain))
        assertEquals("holdings", store.get(plain))
    }

    @Test
    fun read_keepsPlainCopyWhenEncryptThrows() {
        val cipher = FakeCipher().apply { throwOnEncrypt = true }
        val store = FakeStore(mutableMapOf(plain to "holdings"))

        assertEquals(SecureRead.Value("holdings"), SecureFields.read(store, cipher, enc, plain))
        assertEquals("holdings", store.get(plain))
    }

    @Test
    fun read_nothingStoredReturnsNull() {
        assertEquals(SecureRead.Value(null), SecureFields.read(FakeStore(), FakeCipher(), enc, plain))
    }

    @Test
    fun write_storesOnlyEncryptedCopy() {
        val cipher = FakeCipher()
        val store = FakeStore(mutableMapOf(plain to "old"))

        assertTrue(SecureFields.write(store, cipher, enc, plain, "https://docs.google.com/spreadsheets/d/abc"))
        assertNull(store.get(plain))
        assertEquals("https://docs.google.com/spreadsheets/d/abc", cipher.decryptOrNull(store.get(enc)!!))
    }

    @Test
    fun write_emptyValueRemovesBothCopies() {
        val store = FakeStore(mutableMapOf(enc to "x:dlo", plain to "old"))

        assertTrue(SecureFields.write(store, FakeCipher(), enc, plain, ""))
        assertTrue(store.data.isEmpty())
    }

    @Test
    fun write_failureKeepsPreviousData() {
        val cipher = FakeCipher().apply { throwOnEncrypt = true }
        val store = FakeStore(mutableMapOf(enc to "x:dlo"))

        assertFalse(SecureFields.write(store, cipher, enc, plain, "new"))
        assertEquals("x:dlo", store.get(enc))
    }
}
