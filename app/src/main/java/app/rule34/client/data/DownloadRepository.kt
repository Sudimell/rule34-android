package app.rule34.client.data

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class DownloadRepository(
    private val context: Context,
    private val client: OkHttpClient,
) {
    suspend fun find(postId: Long): Uri? = withContext(Dispatchers.IO) {
        val name = "rule34-$postId%"
        sequenceOf(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
        ).firstNotNullOfOrNull { collection -> query(collection, name) }
    }

    suspend fun download(post: Post): Uri = withContext(Dispatchers.IO) {
        find(post.id)?.let { return@withContext it }
        val source = post.originalUrl.ifBlank { throw IOException("No file") }
        val extension = source.substringBefore('?').substringAfterLast('.', "jpg").lowercase()
        val video = extension == "mp4" || extension == "webm" || extension == "mkv"
        val mime = mimeFor(extension)
        val collection = collectionFor(video)
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "rule34-${post.id}.$extension")
            put(MediaStore.MediaColumns.MIME_TYPE, mime)
            if (Build.VERSION.SDK_INT >= 29) {
                put(
                    MediaStore.MediaColumns.RELATIVE_PATH,
                    if (video) "Movies/rule34-android" else "Pictures/rule34-android",
                )
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
        }
        val resolver = context.contentResolver
        val uri = resolver.insert(collection, values) ?: throw IOException("MediaStore insert failed")
        try {
            val request = Request.Builder().url(source).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw IOException("HTTP ${response.code}")
                val body = response.body
                resolver.openOutputStream(uri)?.use { output ->
                    body.byteStream().use { input -> input.copyTo(output) }
                } ?: throw IOException("No output stream")
            }
            if (Build.VERSION.SDK_INT >= 29) {
                values.clear()
                values.put(MediaStore.MediaColumns.IS_PENDING, 0)
                resolver.update(uri, values, null, null)
            }
            uri
        } catch (error: Exception) {
            resolver.delete(uri, null, null)
            throw error
        }
    }

    private fun query(collection: Uri, name: String): Uri? {
        val projection = arrayOf(MediaStore.MediaColumns._ID)
        context.contentResolver.query(
            collection,
            projection,
            "${MediaStore.MediaColumns.DISPLAY_NAME} LIKE ?",
            arrayOf(name),
            null,
        )?.use { cursor ->
            if (!cursor.moveToFirst()) return null
            val id = cursor.getLong(0)
            return Uri.withAppendedPath(collection, id.toString())
        }
        return null
    }

    private fun collectionFor(video: Boolean): Uri = when {
        Build.VERSION.SDK_INT >= 29 && video ->
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        Build.VERSION.SDK_INT >= 29 ->
            MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        video -> MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        else -> MediaStore.Images.Media.EXTERNAL_CONTENT_URI
    }

    private fun mimeFor(extension: String): String = when (extension) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        "mp4" -> "video/mp4"
        "webm" -> "video/webm"
        "mkv" -> "video/x-matroska"
        else -> "image/jpeg"
    }
}
