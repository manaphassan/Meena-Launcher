package my.hana.meena.data.system

import android.content.Context
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

/** Rules for the notes widget, kept free of Android so they can be unit-tested. */
object NoteRules {
    const val MAX_LENGTH = 500
    const val MAX_NOTES = 20

    /** Trimmed note text, or null when there is nothing to save. */
    fun clean(input: String): String? = input.trim().take(MAX_LENGTH).trim().ifEmpty { null }

    /** Newest note first; the oldest drop off past [MAX_NOTES]. */
    fun add(notes: List<String>, note: String): List<String> = (listOf(note) + notes).take(MAX_NOTES)

    fun remove(notes: List<String>, index: Int): List<String> =
        if (index in notes.indices) notes.filterIndexed { i, _ -> i != index } else notes

    fun encode(notes: List<String>): String = Json.encodeToString(ListSerializer(String.serializer()), notes)

    /** Null when the saved text isn't a list of notes. */
    fun decode(saved: String): List<String>? =
        runCatching { Json.decodeFromString(ListSerializer(String.serializer()), saved) }.getOrNull()
}

/** Notes stay on the phone, encrypted with the same Android Keystore key as the portfolio. */
object NotesRepository {
    private const val PREFS_NAME = "meena_notes"
    private const val KEY_ENC = "notes_enc"
    private const val KEY_PLAIN = "notes_plain" // never written; SecureFields wants a legacy key

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _notes = MutableStateFlow<List<String>>(emptyList())
    val notes: StateFlow<List<String>> = _notes.asStateFlow()

    /** True when saved notes exist but this phone can't decrypt them; writes are refused so they aren't lost. */
    private val _unreadable = MutableStateFlow(false)
    val unreadable: StateFlow<Boolean> = _unreadable.asStateFlow()

    @Volatile
    private var loaded = false

    private fun store(context: Context) =
        SharedPrefsStore(context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE))

    fun load(context: Context) {
        if (loaded) return
        scope.launch {
            when (val read =
                SecureFields.read(store(context), KeystoreSecretsManager.fieldCipher, KEY_ENC, KEY_PLAIN)) {
                is SecureRead.Value -> {
                    val list = read.text?.let { NoteRules.decode(it) }
                    if (read.text != null && list == null) {
                        _unreadable.value = true
                    } else {
                        _notes.value = list.orEmpty()
                        _unreadable.value = false
                    }
                }

                SecureRead.Unreadable -> _unreadable.value = true
            }
            loaded = true
        }
    }

    /** Returns false when the note was empty or can't be saved. */
    fun add(context: Context, input: String): Boolean {
        val note = NoteRules.clean(input) ?: return false
        if (_unreadable.value) return false
        save(context, NoteRules.add(_notes.value, note))
        return true
    }

    fun remove(context: Context, index: Int) {
        if (_unreadable.value) return
        save(context, NoteRules.remove(_notes.value, index))
    }

    private fun save(context: Context, updated: List<String>) {
        _notes.value = updated
        scope.launch {
            SecureFields.write(
                store(context), KeystoreSecretsManager.fieldCipher, KEY_ENC, KEY_PLAIN,
                if (updated.isEmpty()) null else NoteRules.encode(updated)
            )
        }
    }
}
