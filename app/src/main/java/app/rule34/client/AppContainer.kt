package app.rule34.client

import android.content.Context
import app.rule34.client.data.CredentialsRepository
import app.rule34.client.data.DownloadRepository
import app.rule34.client.data.FeedStore
import app.rule34.client.data.Rule34Api
import app.rule34.client.data.SearchRelay
import app.rule34.client.data.SettingsRepository
import app.rule34.client.data.local.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

class AppContainer(context: Context) {
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val okHttp = Rule34Api.httpClient()
    val api = Rule34Api(okHttp)
    val credentials = CredentialsRepository(context)
    val settings = SettingsRepository(context, credentials)
    val database = AppDatabase.create(context)
    val feed = FeedStore(api, credentials, database, appScope)
    val downloads = DownloadRepository(context, okHttp)
    val searchRelay = SearchRelay()
}
