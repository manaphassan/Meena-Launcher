package my.hana.meena

import my.hana.meena.data.system.FavouriteRules
import org.junit.Assert.assertEquals
import org.junit.Test

class WideTileRowsTest {

    @Test
    fun normalTiles_fillRowsOfThree() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3, 4)), FavouriteRules.packRows(listOf(1, 1, 1, 1, 1), 3))
    }

    @Test
    fun wideTile_takesTwoCells() {
        // wide + normal fills a row; then three normals
        assertEquals(listOf(listOf(0, 1), listOf(2, 3, 4)), FavouriteRules.packRows(listOf(2, 1, 1, 1, 1), 3))
    }

    @Test
    fun wideTileThatDoesNotFit_startsTheNextRow() {
        // two normals then a wide: the wide can't fit in the one cell left
        assertEquals(listOf(listOf(0, 1), listOf(2, 3)), FavouriteRules.packRows(listOf(1, 1, 2, 1), 3))
    }

    @Test
    fun wideTile_onSmallTilesRow() {
        assertEquals(listOf(listOf(0, 1, 2), listOf(3)), FavouriteRules.packRows(listOf(2, 1, 1, 2), 4))
    }

    @Test
    fun widthIsClampedToTheRow() {
        assertEquals(listOf(listOf(0), listOf(1)), FavouriteRules.packRows(listOf(5, 1), 1))
        assertEquals(emptyList<List<Int>>(), FavouriteRules.packRows(emptyList(), 3))
    }
}
