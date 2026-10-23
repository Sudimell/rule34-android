package app.rule34.client.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull

object PostJson {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    fun parsePosts(body: String): List<Post> {
        val trimmed = body.trim()
        if (trimmed.isEmpty() || trimmed == "null" || trimmed == "[]") return emptyList()
        if (!trimmed.startsWith("[") && !trimmed.startsWith("{")) {
            throw plainTextError(trimmed)
        }
        val element = try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            throw plainTextError(trimmed)
        }
        return when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonObject)?.toPost() }
            is JsonObject -> parseObject(element)
            else -> emptyList()
        }
    }

    fun parseSuggestions(body: String): List<TagSuggestion> {
        val trimmed = body.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val element = try {
            json.parseToJsonElement(trimmed)
        } catch (_: Exception) {
            return emptyList()
        }
        val array = element as? JsonArray ?: return emptyList()
        return array.mapNotNull { item ->
            val objectValue = item as? JsonObject ?: return@mapNotNull null
            val value = objectValue.string("value") ?: return@mapNotNull null
            TagSuggestion(value = value, label = objectValue.string("label") ?: value)
        }
    }

    private fun parseObject(objectValue: JsonObject): List<Post> {
        val success = objectValue["success"]?.asBoolean()
        if (success == false) {
            val message = objectValue.string("message").orEmpty()
            throw when {
                message.contains("search down", ignoreCase = true) -> Rule34Exception.SearchDown()
                message.contains("authentication", ignoreCase = true) -> Rule34Exception.MissingAuthentication()
                else -> Rule34Exception.Api(message.ifBlank { "API error" })
            }
        }
        if (objectValue.containsKey("id")) {
            return listOfNotNull(objectValue.toPost())
        }
        return emptyList()
    }

    private fun JsonObject.toPost(): Post? {
        val id = long("id") ?: return null
        val tags = string("tags").orEmpty().split(Regex("\\s+")).filter { it.isNotBlank() }
        val fileUrl = string("file_url").orEmpty().withHttps()
        val sampleUrl = string("sample_url").orEmpty().withHttps().ifBlank { fileUrl }
        val previewUrl = string("preview_url").orEmpty().withHttps().ifBlank { sampleUrl }
        return Post(
            id = id,
            score = int("score") ?: 0,
            rating = string("rating").orEmpty(),
            tags = tags,
            previewUrl = previewUrl,
            sampleUrl = sampleUrl,
            fileUrl = fileUrl,
            width = int("width") ?: 0,
            height = int("height") ?: 0,
        )
    }

    private fun plainTextError(body: String): Rule34Exception = when {
        body.contains("authentication", ignoreCase = true) -> Rule34Exception.MissingAuthentication()
        body.contains("search down", ignoreCase = true) -> Rule34Exception.SearchDown()
        else -> Rule34Exception.Api(body.lineSequence().firstOrNull()?.take(180) ?: "Empty response")
    }

    private fun JsonObject.string(name: String): String? =
        (this[name] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.long(name: String): Long? =
        (this[name] as? JsonPrimitive)?.contentOrNull?.toLongOrNull()

    private fun JsonObject.int(name: String): Int? =
        (this[name] as? JsonPrimitive)?.contentOrNull?.toIntOrNull()

    private fun JsonElement.asBoolean(): Boolean? =
        (this as? JsonPrimitive)?.booleanOrNull
            ?: (this as? JsonPrimitive)?.contentOrNull?.toBooleanStrictOrNull()

    private fun String.withHttps(): String = when {
        startsWith("//") -> "https:$this"
        else -> this
    }
}
