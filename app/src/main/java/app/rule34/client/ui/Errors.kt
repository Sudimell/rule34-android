package app.rule34.client.ui

import app.rule34.client.data.Rule34Exception
import java.io.IOException

fun Throwable.userMessage(): String = when (this) {
    is Rule34Exception.MissingAuthentication -> "Ключ или user id не подошли."
    is Rule34Exception.SearchDown -> "Поиск rule34 сейчас не отвечает."
    is Rule34Exception.Api -> message ?: "Ошибка API."
    is IOException -> "Нет сети."
    else -> message?.takeIf { it.isNotBlank() } ?: "Что-то пошло не так."
}
