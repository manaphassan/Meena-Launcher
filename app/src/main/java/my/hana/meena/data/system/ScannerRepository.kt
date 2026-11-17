package my.hana.meena.data.system

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

@Serializable
data class ScanEntry(
    val content: String,
    val format: String,
    val timestamp: Long
)

object ScannerRepository {
    private const val PREFS_NAME = "meena_scanner"
    private const val KEY_ENC = "scans_enc"
    private const val KEY_PLAIN = "scans_plain"
    const val MAX_HISTORY = 50

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val json = Json { ignoreUnknownKeys = true }

    private val _scans = MutableStateFlow<List<ScanEntry>>(emptyList())
    val scans: StateFlow<List<ScanEntry>> = _scans.asStateFlow()

    @Volatile
    private var loaded = false

    private fun store(context: Context) =
        SharedPrefsStore(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun load(context: Context) {
        if (loaded) return
        scope.launch {
            when (val read = SecureFields.read(store(context), KeystoreSecretsManager.fieldCipher, KEY_ENC, KEY_PLAIN)) {
                is SecureRead.Value -> {
                    val list = read.text?.let { decode(it) }
                    _scans.value = list.orEmpty()
                }
                SecureRead.Unreadable -> { /* can't read, start empty */ }
            }
            loaded = true
        }
    }

    fun add(context: Context, entry: ScanEntry) {
        val updated = (listOf(entry) + _scans.value)
            .distinctBy { it.content + it.format }
            .take(MAX_HISTORY)
        save(context, updated)
    }

    fun remove(context: Context, index: Int) {
        val current = _scans.value
        if (index !in current.indices) return
        save(context, current.filterIndexed { i, _ -> i != index })
    }

    fun clear(context: Context) {
        save(context, emptyList())
    }

    private fun save(context: Context, updated: List<ScanEntry>) {
        _scans.value = updated
        scope.launch {
            SecureFields.write(
                store(context), KeystoreSecretsManager.fieldCipher, KEY_ENC, KEY_PLAIN,
                if (updated.isEmpty()) null else encode(updated)
            )
        }
    }

    private fun encode(entries: List<ScanEntry>): String =
        json.encodeToString(ListSerializer(ScanEntry.serializer()), entries)

    private fun decode(saved: String): List<ScanEntry>? =
        runCatching { json.decodeFromString(ListSerializer(ScanEntry.serializer()), saved) }.getOrNull()

    /** Human-readable format name from ML Kit format constant. */
    fun formatName(mlKitFormat: Int): String = when (mlKitFormat) {
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_QR_CODE -> "qr"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_13 -> "ean-13"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_EAN_8 -> "ean-8"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_A -> "upc-a"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_UPC_E -> "upc-e"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_128 -> "code-128"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_39 -> "code-39"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODE_93 -> "code-93"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_ITF -> "itf"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_PDF417 -> "pdf417"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_AZTEC -> "aztec"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_DATA_MATRIX -> "data matrix"
        com.google.mlkit.vision.barcode.common.Barcode.FORMAT_CODABAR -> "codabar"
        else -> "barcode"
    }
}
