package app.rule34.client.ui.post

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.paging.compose.collectAsLazyPagingItems
import app.rule34.client.AppContainer
import app.rule34.client.data.DownloadRepository
import app.rule34.client.data.FeedStore
import app.rule34.client.data.MediaKind
import app.rule34.client.data.Post
import app.rule34.client.ui.userMessage
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import me.saket.telephoto.zoomable.coil3.ZoomableAsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostScreen(
    container: AppContainer,
    source: String,
    query: String,
    startIndex: Int,
    onBack: () -> Unit,
    onTag: (String) -> Unit,
) {
    val viewModel = viewModel<PostViewModel>(
        key = "$source|$query",
        factory = PostViewModel.factory(container, source, query),
    )
    val items = viewModel.posts.collectAsLazyPagingItems()
    val pagerState = rememberPagerState(initialPage = 0) {
        items.itemCount.coerceAtLeast(1)
    }
    val snackbar = remember { SnackbarHostState() }
    val context = LocalContext.current
    var jumped by rememberSaveable { mutableStateOf(false) }
    var pendingDownload by remember { mutableStateOf<Post?>(null) }
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        val post = pendingDownload
        pendingDownload = null
        if (granted && post != null) viewModel.download(post)
    }

    LaunchedEffect(items.itemCount, startIndex) {
        if (!jumped && startIndex > 0 && items.itemCount > startIndex) {
            pagerState.scrollToPage(startIndex)
            jumped = true
        }
    }
    LaunchedEffect(pagerState, items) {
        snapshotFlow { pagerState.currentPage }.collect { page ->
            if (page < items.itemCount) {
                items[page]?.let { post ->
                    viewModel.remember(post)
                    viewModel.lookupSaved(post)
                }
            }
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.messages.collect { snackbar.showSnackbar(it) }
    }

    val page = pagerState.currentPage
    val current = if (page < items.itemCount) items[page] else null
    val favorite by androidx.compose.runtime.produceState(initialValue = false, current?.id) {
        val id = current?.id
        if (id == null) {
            value = false
        } else {
            viewModel.favorite(id).collect { value = it }
        }
    }
    val savedUri = current?.let { viewModel.savedUris[it.id] }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = {
            TopAppBar(
                title = { Text(current?.let { "#${it.id}" } ?: "Пост") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                },
                actions = {
                    if (current != null) {
                        FilledIconButton(onClick = { viewModel.toggleFavorite(current) }) {
                            Icon(
                                imageVector = if (favorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                                contentDescription = if (favorite) "Убрать из избранного" else "В избранное",
                            )
                        }
                        IconButton(
                            onClick = {
                                if (needsStoragePermission(context)) {
                                    pendingDownload = current
                                    permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                                } else {
                                    viewModel.download(current)
                                }
                            },
                            enabled = !viewModel.downloading.contains(current.id),
                        ) {
                            if (viewModel.downloading.contains(current.id)) {
                                CircularProgressIndicator()
                            } else {
                                Icon(Icons.Filled.FileDownload, contentDescription = "Скачать")
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f).fillMaxWidth(),
            ) { index ->
                val post = if (index < items.itemCount) items[index] else null
                if (post == null) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                } else {
                    PostPage(post)
                }
            }
            if (current != null) {
                if (savedUri != null) {
                    TextButton(
                        onClick = { openUri(context, savedUri) },
                        modifier = Modifier.padding(horizontal = 12.dp),
                    ) { Text("Открыть файл") }
                }
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(current.tags, key = { it }) { tag ->
                        FilterChip(
                            selected = false,
                            onClick = { onTag(tag) },
                            label = { Text(tag) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PostPage(post: Post) {
    var original by rememberSaveable(post.id) { mutableStateOf(false) }
    val url = if (original) post.originalUrl else post.displayUrl
    Column(Modifier.fillMaxSize()) {
        Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
            when (post.kind) {
                MediaKind.VIDEO -> AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { context ->
                        VideoView(context).apply {
                            setVideoURI(Uri.parse(post.originalUrl))
                            setOnPreparedListener { player ->
                                player.isLooping = true
                                start()
                            }
                        }
                    },
                    onRelease = { it.stopPlayback() },
                )
                MediaKind.IMAGE, MediaKind.ANIMATION -> ZoomableAsyncImage(
                    modifier = Modifier.fillMaxSize(),
                    model = url,
                    contentDescription = "Пост ${post.id}",
                    contentScale = ContentScale.Fit,
                )
            }
        }
        if (post.kind != MediaKind.VIDEO && post.originalUrl != post.displayUrl) {
            FilterChip(
                selected = original,
                onClick = { original = !original },
                label = { Text(if (original) "Оригинал" else "Сэмпл") },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
            )
        }
    }
}

private fun needsStoragePermission(context: android.content.Context): Boolean =
    Build.VERSION.SDK_INT <= 28 &&
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
        ) != PackageManager.PERMISSION_GRANTED

private fun openUri(context: android.content.Context, uri: Uri) {
    val mime = context.contentResolver.getType(uri) ?: "*/*"
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(uri, mime)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    runCatching { context.startActivity(intent) }
}

class PostViewModel(
    private val source: String,
    private val query: String,
    private val feed: FeedStore,
    private val downloads: DownloadRepository,
) : ViewModel() {
    val posts = when (source) {
        "favorites" -> feed.favorites
        "history" -> feed.history
        else -> feed.search(query)
    }

    private val messagesFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messages = messagesFlow.asSharedFlow()
    val downloading = mutableStateMapOf<Long, Boolean>()
    val savedUris = mutableStateMapOf<Long, Uri>()

    fun favorite(id: Long) = feed.observeFavorite(id)

    fun toggleFavorite(post: Post) {
        viewModelScope.launch { feed.toggleFavorite(post) }
    }

    fun remember(post: Post) {
        viewModelScope.launch { feed.remember(post) }
    }

    fun download(post: Post) {
        if (downloading[post.id] == true) return
        viewModelScope.launch {
            downloading[post.id] = true
            runCatching { downloads.download(post) }
                .onSuccess { uri ->
                    savedUris[post.id] = uri
                    messagesFlow.emit("Сохранено")
                }
                .onFailure { error -> messagesFlow.emit(error.userMessage()) }
            downloading.remove(post.id)
        }
    }

    fun lookupSaved(post: Post) {
        viewModelScope.launch {
            if (savedUris.containsKey(post.id)) return@launch
            downloads.find(post.id)?.let { savedUris[post.id] = it }
        }
    }

    companion object {
        fun factory(container: AppContainer, source: String, query: String): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return PostViewModel(source, query, container.feed, container.downloads) as T
                }
            }
    }
}
