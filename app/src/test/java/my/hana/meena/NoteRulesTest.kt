package my.hana.meena

import my.hana.meena.data.system.NoteRules
import my.hana.meena.data.system.NoteRules.LineType
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
        assertEquals(
            listOf("milk" to LineType.Bullet, "plain" to LineType.Plain),
            NoteRules.lines("- milk\n\nplain")
        )
        assertEquals("milk", NoteRules.title("- milk\n- eggs"))
        assertEquals("", NoteRules.title("   "))
    }

    @Test
    fun lines_parsesTaskAndDone() {
        assertEquals(
            listOf(
                "buy milk" to LineType.Task,
                "clean house" to LineType.Done,
                "remember" to LineType.Bullet,
                "plain" to LineType.Plain
            ),
            NoteRules.lines("* buy milk\nx clean house\n- remember\nplain")
        )
    }

    @Test
    fun isList_recognisesAllMarkerTypes() {
        assertTrue(NoteRules.isList("- a\n- b"))
        assertTrue(NoteRules.isList("* a\n* b"))
        assertTrue(NoteRules.isList("x a\nx b"))
        assertTrue(NoteRules.isList("- a\n* b\nx c"))
        assertFalse(NoteRules.isList("plain\n- b"))
        assertFalse(NoteRules.isList("  "))
    }

    @Test
    fun toList_stripsTaskAndDoneMarkers() {
        assertEquals("- buy milk\n- done thing", NoteRules.toList("* buy milk\nx done thing"))
    }

    @Test
    fun toText_stripsAllMarkers() {
        assertEquals("task\ndone\nbullet\nplain", NoteRules.toText("* task\nx done\n- bullet\nplain"))
    }

    @Test
    fun toggleLine_flipsTaskAndDone() {
        assertEquals("x buy milk", NoteRules.toggleLine("* buy milk", 0))
        assertEquals("* buy milk", NoteRules.toggleLine("x buy milk", 0))
        assertEquals("* first\nx second", NoteRules.toggleLine("* first\n* second", 1))
        // Blank lines are skipped in index counting but preserved.
        assertEquals("x first\n\n* second", NoteRules.toggleLine("* first\n\n* second", 0))
    }

    @Test
    fun toggleLine_leavesBulletAndPlainAlone() {
        assertEquals("- bullet", NoteRules.toggleLine("- bullet", 0))
        assertEquals("plain text", NoteRules.toggleLine("plain text", 0))
    }
}
