package app.rule34.client.ui.library

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.paging.compose.collectAsLazyPagingItems
import app.rule34.client.AppContainer
import app.rule34.client.ui.common.PostGrid

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    container: AppContainer,
    onPost: (source: String, index: Int) -> Unit,
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    val favorites = container.feed.favorites.collectAsLazyPagingItems()
    val history = container.feed.history.collectAsLazyPagingItems()
    Column(Modifier.fillMaxSize()) {
        PrimaryTabRow(selectedTabIndex = tab) {
            Tab(selected = tab == 0, onClick = { tab = 0 }, text = { Text("Избранное") })
            Tab(selected = tab == 1, onClick = { tab = 1 }, text = { Text("История") })
        }
        if (tab == 0) {
            PostGrid(items = favorites, onPost = { onPost("favorites", it) }, modifier = Modifier.weight(1f))
        } else {
            PostGrid(items = history, onPost = { onPost("history", it) }, modifier = Modifier.weight(1f))
        }
    }
}
