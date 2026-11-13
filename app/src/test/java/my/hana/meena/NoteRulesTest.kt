package my.hana.meena

import my.hana.meena.data.system.NoteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NoteRulesTest {

    @Test
    fun clean_trimsAndRejectsBlank() {
        assertEquals("buy milk", NoteRules.clean("  buy milk \n"))
        assertNull(NoteRules.clean("   \n "))
        assertEquals(NoteRules.MAX_LENGTH, NoteRules.clean("x".repeat(900))!!.length)
    }

    @Test
    fun add_putsNewestFirst_andCapsTheList() {
        assertEquals(listOf("b", "a"), NoteRules.add(listOf("a"), "b"))
        val full = List(NoteRules.MAX_NOTES) { "n$it" }
        val added = NoteRules.add(full, "new")
        assertEquals(NoteRules.MAX_NOTES, added.size)
        assertEquals("new", added.first())
    }

    @Test
    fun remove_ignoresAMissingIndex() {
        assertEquals(listOf("a", "c"), NoteRules.remove(listOf("a", "b", "c"), 1))
        assertEquals(listOf("a"), NoteRules.remove(listOf("a"), 5))
    }

    @Test
    fun encodeDecode_roundTrips_andRejectsJunk() {
        val notes = listOf("one", "two \"quoted\"", "line\nbreak")
        assertEquals(notes, NoteRules.decode(NoteRules.encode(notes)))
        assertNull(NoteRules.decode("not json"))
    }
}
