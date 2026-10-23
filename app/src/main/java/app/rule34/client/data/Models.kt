package app.rule34.client.data

data class Credentials(
    val userId: String,
    val apiKey: String,
)

data class Post(
    val id: Long,
    val score: Int,
    val rating: String,
    val tags: List<String>,
    val previewUrl: String,
    val sampleUrl: String,
    val fileUrl: String,
    val width: Int,
    val height: Int,
) {
    val displayUrl: String
        get() = sampleUrl.ifBlank { fileUrl.ifBlank { previewUrl } }

    val originalUrl: String
        get() = fileUrl.ifBlank { sampleUrl.ifBlank { previewUrl } }

    val kind: MediaKind
        get() = mediaKind(originalUrl)
}

enum class MediaKind {
    IMAGE,
    VIDEO,
    ANIMATION,
}

fun mediaKind(url: String): MediaKind {
    val extension = url.substringBefore('?').substringAfterLast('.', "").lowercase()
    return when (extension) {
        "mp4", "webm", "mkv" -> MediaKind.VIDEO
        "gif" -> MediaKind.ANIMATION
        else -> MediaKind.IMAGE
    }
}

fun ratingMark(rating: String): String = when (rating.lowercase()) {
    "e", "explicit" -> "E"
    "q", "questionable" -> "Q"
    "s", "safe" -> "S"
    else -> rating.take(1).uppercase()
}

data class TagSuggestion(
    val value: String,
    val label: String,
)

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK,
    ;

    companion object {
        fun fromStored(value: String?): ThemeMode =
            entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}

sealed class Rule34Exception(message: String) : Exception(message) {
    class MissingAuthentication : Rule34Exception("Missing authentication")
    class SearchDown : Rule34Exception("Search down")
    class Api(message: String) : Rule34Exception(message)
}
