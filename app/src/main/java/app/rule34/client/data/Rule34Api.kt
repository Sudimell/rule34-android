package app.rule34.client.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.http.GET
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

private const val USER_AGENT = "rule34-android/1.0 (unofficial)"

interface Rule34Service {
    @GET("index.php")
    suspend fun posts(
        @Query("page") page: String = "dapi",
        @Query("s") section: String = "post",
        @Query("q") query: String = "index",
        @Query("json") json: Int = 1,
        @Query("tags") tags: String,
        @Query("pid") pid: Int,
        @Query("limit") limit: Int,
        @Query("api_key") apiKey: String,
        @Query("user_id") userId: String,
    ): Response<ResponseBody>

    @GET("autocomplete.php")
    suspend fun autocomplete(@Query("q") query: String): Response<ResponseBody>
}

class Rule34Api(okHttp: OkHttpClient) {
    private val service = Retrofit.Builder()
        .baseUrl("https://api.rule34.xxx/")
        .client(okHttp)
        .build()
        .create(Rule34Service::class.java)

    suspend fun posts(
        userId: String,
        apiKey: String,
        tags: String,
        pid: Int,
        limit: Int,
    ): List<Post> = withContext(Dispatchers.IO) {
        val response = service.posts(
            tags = tags,
            pid = pid,
            limit = limit.coerceIn(1, 100),
            apiKey = apiKey,
            userId = userId,
        )
        PostJson.parsePosts(response.readBody())
    }

    suspend fun autocomplete(prefix: String): List<TagSuggestion> = withContext(Dispatchers.IO) {
        if (prefix.isBlank()) return@withContext emptyList()
        val response = service.autocomplete(prefix)
        if (!response.isSuccessful) return@withContext emptyList()
        PostJson.parseSuggestions(response.body()?.string().orEmpty())
    }

    companion object {
        fun httpClient(): OkHttpClient = OkHttpClient.Builder()
            .connectTimeout(20, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .callTimeout(40, TimeUnit.SECONDS)
            .addInterceptor { chain ->
                chain.proceed(
                    chain.request().newBuilder()
                        .header("User-Agent", USER_AGENT)
                        .build(),
                )
            }
            .build()
    }
}

private fun Response<ResponseBody>.readBody(): String {
    val payload = body()?.string() ?: errorBody()?.string().orEmpty()
    if (!isSuccessful && payload.isBlank()) {
        throw Rule34Exception.Api("HTTP ${code()}")
    }
    return payload
}
