package my.hana.meena

import my.hana.meena.data.system.NoteRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun replace_editsInPlace_andRefusesBlankOrMissing() {
        assertEquals(listOf("a", "B", "c"), NoteRules.replace(listOf("a", "b", "c"), 1, " B "))
        assertNull(NoteRules.replace(listOf("a"), 0, "  "))
        assertNull(NoteRules.replace(listOf("a"), 3, "x"))
    }

    @Test
    fun toList_splitsLinesOrCommas_andToTextUndoesIt() {
        assertEquals("- milk\n- eggs\n- bread", NoteRules.toList("milk, eggs; bread"))
        assertEquals("- call mum\n- pay bills, today", NoteRules.toList("call mum\npay bills, today"))
        assertEquals("- already", NoteRules.toList("- already"))
        assertTrue(NoteRules.isList(NoteRules.toList("a, b")))
        assertFalse(NoteRules.isList("a\n- b"))
        assertEquals("milk\neggs", NoteRules.toText("- milk\n- eggs"))
    }

    @Test
    fun lines_andTitle_dropTheListMark() {
        assertEquals(listOf("milk" to true, "plain" to false), NoteRules.lines("- milk\n\nplain"))
        assertEquals("milk", NoteRules.title("- milk\n- eggs"))
        assertEquals("", NoteRules.title("   "))
    }
}
