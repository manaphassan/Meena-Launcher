package my.hana.meena

import kotlinx.serialization.json.Json
import my.hana.meena.data.model.HomeTitle
import my.hana.meena.data.model.MeenaUserSettings
import org.junit.Assert.assertEquals
import org.junit.Test

class HomeTitleTest {

    @Test
    fun clean_lowercasesAndKeepsInnerSpaces() {
        assertEquals("my phone ", HomeTitle.clean("My Phone "))
    }

    @Test
    fun clean_flattensLineBreaksAndCapsLength() {
        assertEquals("a b", HomeTitle.clean("a\nb"))
        assertEquals(HomeTitle.MAX_LENGTH, HomeTitle.clean("x".repeat(50)).length)
    }

    @Test
    fun display_trimsAndEmptyMeansHidden() {
        assertEquals("hana", HomeTitle.display("  hana "))
        assertEquals("", HomeTitle.display("   "))
    }

    @Test
    fun oldSettingsWithoutTheField_keepTheDefaultName() {
        val json = Json { ignoreUnknownKeys = true }
        assertEquals("meena", json.decodeFromString<MeenaUserSettings>("{}").homeTitle)
        assertEquals("meena", MeenaUserSettings().homeTitle)
    }
}
