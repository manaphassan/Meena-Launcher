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

    /** Swaps the note at [index] for [input]; null when the edit is empty or the note is gone. */
    fun replace(notes: List<String>, index: Int, input: String): List<String>? {
        if (index !in notes.indices) return null
        val note = clean(input) ?: return null
        return notes.toMutableList().also { it[index] = note }
    }

    /** Bullet-journal line types: a prefix decides how the line looks. */
    enum class LineType { Plain, Bullet, Task, Done }

    /** List lines are stored as plain text starting with this, so saved notes keep their format. */
    const val BULLET = "- "
    /** Task lines start with this; tap to mark done. */
    const val TASK = "* "
    /** Done lines start with this; tap to undo. */
    const val DONE = "x "

    /** True when every line of [note] has a marker (bullet, task or done). */
    fun isList(note: String): Boolean {
        val lines = note.lines().filter { it.isNotBlank() }
        return lines.isNotEmpty() && lines.all {
            val t = it.trimStart()
            t.startsWith(BULLET) || t.startsWith(TASK) || t.startsWith(DONE)
        }
    }

    /**
     * One list line per line of the note. A single line is split at commas and semicolons
     * first, so "milk, eggs, bread" becomes three lines.
     */
    fun toList(note: String): String {
        val lines = note.lines().map { it.trim() }.filter { it.isNotEmpty() }
        val items =
            if (lines.size == 1) lines.single().split(',', ';').map { it.trim() }.filter { it.isNotEmpty() } else lines
        return items.joinToString("\n") { BULLET + stripMarker(it) }
    }

    /** Plain lines again, one per list line. */
    fun toText(note: String): String =
        note.lines().joinToString("\n") { line -> stripMarker(line.trimStart()).ifEmpty { line } }

    /** Strips any leading marker and trims. Returns the bare text, or empty when there was nothing. */
    private fun stripMarker(line: String): String {
        val t = line.trimStart()
        return when {
            t.startsWith(BULLET) -> t.removePrefix(BULLET).trim()
            t.startsWith(TASK) -> t.removePrefix(TASK).trim()
            t.startsWith(DONE) -> t.removePrefix(DONE).trim()
            else -> t
        }
    }

    /** Lines to draw: the text and its bullet-journal type. */
    fun lines(note: String): List<Pair<String, LineType>> =
        note.lines().filter { it.isNotBlank() }.map { line ->
            val trimmed = line.trimStart()
            when {
                trimmed.startsWith(DONE) -> trimmed.removePrefix(DONE).trim() to LineType.Done
                trimmed.startsWith(TASK) -> trimmed.removePrefix(TASK).trim() to LineType.Task
                trimmed.startsWith(BULLET) -> trimmed.removePrefix(BULLET).trim() to LineType.Bullet
                else -> line to LineType.Plain
            }
        }

    /** Flips a task line to done or vice-versa; bullet and plain lines are left alone. */
    fun toggleLine(note: String, lineIndex: Int): String {
        var index = 0
        return note.lines().joinToString("\n") { line ->
            if (line.isBlank()) line
            else {
                val current = index++
                if (current != lineIndex) line
                else {
                    val trimmed = line.trimStart()
                    when {
                        trimmed.startsWith(TASK) -> DONE + trimmed.removePrefix(TASK)
                        trimmed.startsWith(DONE) -> TASK + trimmed.removePrefix(DONE)
                        else -> line
                    }
                }
            }
        }
    }

    /** A short title for a calendar reminder: the first line, without the list mark. */
    fun title(note: String, max: Int = 60): String =
        lines(note).firstOrNull()?.first?.take(max)?.trim().orEmpty()

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

    /** Returns false when the edit was empty or can't be saved. */
    fun update(context: Context, index: Int, input: String): Boolean {
        if (_unreadable.value) return false
        val updated = NoteRules.replace(_notes.value, index, input) ?: return false
        save(context, updated)
        return true
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
