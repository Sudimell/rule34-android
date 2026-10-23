package app.rule34.client.data

import org.junit.Assert.assertEquals
import org.junit.Test

class QueryBuilderTest {
    @Test
    fun joinsChipsRawAndBlacklist() {
        val query = QueryBuilder.build(
            tags = listOf("Cat Ears", "tail"),
            raw = "score:>10  rating:explicit",
            blacklist = listOf("gore", "tail"),
        )
        assertEquals("cat_ears tail score:>10 rating:explicit -gore", query)
    }

    @Test
    fun skipsBlacklistWhenTheTagIsExplicit() {
        val query = QueryBuilder.build(
            tags = listOf("gore"),
            raw = "",
            blacklist = listOf("gore"),
        )
        assertEquals("gore", query)
    }

    @Test
    fun doesNotDuplicateAnExclusion() {
        val query = QueryBuilder.build(
            tags = emptyList(),
            raw = "-Gore",
            blacklist = listOf("gore"),
        )
        assertEquals("-gore", query)
    }

    @Test
    fun emptyInputStaysEmpty() {
        assertEquals("", QueryBuilder.build(emptyList(), "   ", emptyList()))
    }
}
