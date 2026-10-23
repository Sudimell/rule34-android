package app.rule34.client.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import app.rule34.client.data.Post

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey val id: Long,
    val score: Int,
    val rating: String,
    val tags: String,
    val previewUrl: String,
    val sampleUrl: String,
    val fileUrl: String,
    val width: Int,
    val height: Int,
    val savedAt: Long,
)

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: Long,
    val score: Int,
    val rating: String,
    val tags: String,
    val previewUrl: String,
    val sampleUrl: String,
    val fileUrl: String,
    val width: Int,
    val height: Int,
    val viewedAt: Long,
)

@Entity(tableName = "blacklist")
data class BlacklistTagEntity(
    @PrimaryKey val tag: String,
    val createdAt: Long,
)

fun Post.toFavorite(savedAt: Long) = FavoriteEntity(
    id = id,
    score = score,
    rating = rating,
    tags = tags.joinToString(" "),
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    width = width,
    height = height,
    savedAt = savedAt,
)

fun Post.toHistory(viewedAt: Long) = HistoryEntity(
    id = id,
    score = score,
    rating = rating,
    tags = tags.joinToString(" "),
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    width = width,
    height = height,
    viewedAt = viewedAt,
)

fun FavoriteEntity.toPost() = Post(
    id = id,
    score = score,
    rating = rating,
    tags = tags.split(' ').filter { it.isNotBlank() },
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    width = width,
    height = height,
)

fun HistoryEntity.toPost() = Post(
    id = id,
    score = score,
    rating = rating,
    tags = tags.split(' ').filter { it.isNotBlank() },
    previewUrl = previewUrl,
    sampleUrl = sampleUrl,
    fileUrl = fileUrl,
    width = width,
    height = height,
)
