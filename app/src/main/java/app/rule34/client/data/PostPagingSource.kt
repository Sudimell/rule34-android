package app.rule34.client.data

import androidx.paging.PagingSource
import androidx.paging.PagingState

class PostPagingSource(
    private val api: Rule34Api,
    private val credentials: CredentialsRepository,
    private val tags: String,
) : PagingSource<Int, Post>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Post> {
        val page = params.key ?: 0
        val creds = credentials.credentials.value
            ?: return LoadResult.Error(Rule34Exception.MissingAuthentication())
        return try {
            val posts = api.posts(
                userId = creds.userId,
                apiKey = creds.apiKey,
                tags = tags,
                pid = page,
                limit = PAGE_SIZE,
            )
            LoadResult.Page(
                data = posts,
                prevKey = if (page == 0) null else page - 1,
                nextKey = if (posts.size < PAGE_SIZE) null else page + 1,
            )
        } catch (error: Exception) {
            LoadResult.Error(error)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Post>): Int? {
        val anchor = state.anchorPosition ?: return null
        val page = state.closestPageToPosition(anchor) ?: return null
        return page.prevKey?.plus(1) ?: page.nextKey?.minus(1)
    }

    companion object {
        const val PAGE_SIZE = 50
    }
}
