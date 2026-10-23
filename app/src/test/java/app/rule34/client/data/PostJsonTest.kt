package app.rule34.client.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostJsonTest {
    @Test
    fun parsesAPostArray() {
        val posts = PostJson.parsePosts(
            """
            [{
              "id": 42,
              "score": "17",
              "rating": "explicit",
              "tags": "cat ears tail",
              "preview_url": "//wimg.rule34.xxx/thumb.jpg",
              "sample_url": "https://wimg.rule34.xxx/sample.jpg",
              "file_url": "https://wimg.rule34.xxx/file.jpg",
              "width": 1200,
              "height": 800
            }]
            """.trimIndent(),
        )
        assertEquals(1, posts.size)
        assertEquals(42L, posts[0].id)
        assertEquals(17, posts[0].score)
        assertEquals(listOf("cat", "ears", "tail"), posts[0].tags)
        assertEquals("https://wimg.rule34.xxx/thumb.jpg", posts[0].previewUrl)
        assertEquals(MediaKind.IMAGE, posts[0].kind)
    }

    @Test
    fun emptyAttributesObjectIsAnEmptyPage() {
        assertTrue(PostJson.parsePosts("""{"@attributes":{"count":0}}""").isEmpty())
    }

    @Test
    fun missingAuthenticationIsAnError() {
        val error = runCatching {
            PostJson.parsePosts("Missing authentication. Go to api.rule34.xxx")
        }.exceptionOrNull()
        assertTrue(error is Rule34Exception.MissingAuthentication)
    }

    @Test
    fun searchDownObjectIsAnError() {
        val error = runCatching {
            PostJson.parsePosts("""{"success":false,"message":"search down"}""")
        }.exceptionOrNull()
        assertTrue(error is Rule34Exception.SearchDown)
    }

    @Test
    fun parsesAutocompleteLabels() {
        val suggestions = PostJson.parseSuggestions(
            """[{"label":"neko (10)","value":"neko"}]""",
        )
        assertEquals("neko", suggestions.single().value)
        assertEquals("neko (10)", suggestions.single().label)
    }
}
