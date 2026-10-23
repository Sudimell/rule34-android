package app.rule34.client.data

object QueryBuilder {
    fun build(tags: List<String>, raw: String, blacklist: Collection<String>): String {
        val tokens = LinkedHashSet<String>()
        tags.forEach { addToken(tokens, it) }
        raw.split(Regex("\\s+")).forEach { addToken(tokens, it) }

        val included = tokens.filterNot { it.startsWith("-") }.toSet()
        val excluded = tokens.filter { it.startsWith("-") }.map { it.drop(1) }.toSet()
        blacklist.forEach { tag ->
            val normalized = normalizeTag(tag)
            if (normalized.isEmpty() || normalized in included || normalized in excluded) return@forEach
            tokens += "-$normalized"
        }
        return tokens.joinToString(" ")
    }

    fun normalizeTag(tag: String): String =
        tag.trim().replace(' ', '_').lowercase()

    private fun addToken(tokens: LinkedHashSet<String>, rawToken: String) {
        val token = rawToken.trim()
        if (token.isEmpty()) return
        val normalized = when {
            token.startsWith("-") -> "-" + normalizeTag(token.drop(1))
            token.any { it == ':' || it == '>' || it == '<' } -> token.lowercase()
            else -> normalizeTag(token)
        }
        if (normalized.isNotEmpty() && normalized != "-") tokens += normalized
    }
}
