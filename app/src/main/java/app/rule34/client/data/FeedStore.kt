package app.rule34.client.data

import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import app.rule34.client.data.local.AppDatabase
import app.rule34.client.data.local.BlacklistTagEntity
import app.rule34.client.data.local.toFavorite
import app.rule34.client.data.local.toHistory
import app.rule34.client.data.local.toPost
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.ConcurrentHashMap

class FeedStore(
    private val api: Rule34Api,
    private val credentials: CredentialsRepository,
    private val database: AppDatabase,
    private val scope: CoroutineScope,
) {
    private val searches = ConcurrentHashMap<String, Flow<PagingData<Post>>>()

    val favorites: Flow<PagingData<Post>> = pager { database.favorites().paging() }
        .map { pagingData -> pagingData.map { it.toPost() } }
        .cachedIn(scope)

    val history: Flow<PagingData<Post>> = pager { database.history().paging() }
        .map { pagingData -> pagingData.map { it.toPost() } }
        .cachedIn(scope)

    fun search(query: String): Flow<PagingData<Post>> {
        searches[query]?.let { return it }
        if (searches.size >= 4) {
            searches.keys.firstOrNull()?.let(searches::remove)
        }
        val flow = pager { PostPagingSource(api, credentials, query) }.cachedIn(scope)
        searches[query] = flow
        return flow
    }

    fun blacklist() = database.blacklist().observe()

    fun observeFavorite(id: Long) = database.favorites().observeFavorite(id)

    suspend fun toggleFavorite(post: Post): Boolean {
        val dao = database.favorites()
        return if (dao.isFavorite(post.id)) {
            dao.delete(post.id)
            false
        } else {
            dao.upsert(post.toFavorite(System.currentTimeMillis()))
            true
        }
    }

    suspend fun remember(post: Post) {
        database.history().upsert(post.toHistory(System.currentTimeMillis()))
        database.history().trim(HISTORY_LIMIT)
    }

    suspend fun clearHistory() {
        database.history().clear()
    }

    suspend fun addBlacklist(tag: String) {
        val normalized = QueryBuilder.normalizeTag(tag)
        if (normalized.isEmpty()) return
        database.blacklist().insert(BlacklistTagEntity(normalized, System.currentTimeMillis()))
    }

    suspend fun removeBlacklist(tag: String) {
        database.blacklist().delete(tag)
    }

    private fun <T : Any> pager(factory: () -> androidx.paging.PagingSource<Int, T>): Flow<PagingData<T>> =
        Pager(
            config = PagingConfig(
                pageSize = PostPagingSource.PAGE_SIZE,
                initialLoadSize = PostPagingSource.PAGE_SIZE,
                prefetchDistance = 15,
                enablePlaceholders = false,
            ),
            pagingSourceFactory = factory,
        ).flow

    companion object {
        const val HISTORY_LIMIT = 500
    }
}
