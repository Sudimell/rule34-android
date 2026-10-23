package app.rule34.client.ui.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.paging.LoadState
import androidx.paging.compose.LazyPagingItems
import app.rule34.client.data.Post
import app.rule34.client.data.ratingMark
import app.rule34.client.ui.userMessage
import coil3.compose.AsyncImage

@Composable
fun PostGrid(
    items: LazyPagingItems<Post>,
    onPost: (Int) -> Unit,
    modifier: Modifier = Modifier,
    contentPadding: PaddingValues = PaddingValues(12.dp),
) {
    val refresh = items.loadState.refresh
    if (refresh is LoadState.Loading && items.itemCount == 0) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }
    if (refresh is LoadState.Error && items.itemCount == 0) {
        Box(modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            androidx.compose.foundation.layout.Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(refresh.error.userMessage(), style = MaterialTheme.typography.bodyLarge)
                TextButton(onClick = items::retry) { Text("Ещё раз") }
            }
        }
        return
    }
    if (refresh is LoadState.NotLoading && items.itemCount == 0) {
        Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Ничего не нашлось", style = MaterialTheme.typography.bodyLarge)
        }
        return
    }

    BoxWithConstraints(modifier) {
        val columns = when {
            maxWidth < 600.dp -> 2
            maxWidth < 840.dp -> 3
            else -> 5
        }
        LazyVerticalGrid(
            columns = GridCells.Fixed(columns),
            contentPadding = contentPadding,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(count = items.itemCount, key = { index -> items.peek(index)?.id ?: index }) { index ->
                val post = items[index] ?: return@items
                PostCard(post = post, onClick = { onPost(index) })
            }
            when (val append = items.loadState.append) {
                is LoadState.Loading -> item(span = { GridItemSpan(maxLineSpan) }) {
                    Box(Modifier.padding(16.dp).fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is LoadState.Error -> item(span = { GridItemSpan(maxLineSpan) }) {
                    TextButton(onClick = items::retry, modifier = Modifier.padding(8.dp)) {
                        Text("Не загрузилось. Ещё раз")
                    }
                }
                is LoadState.NotLoading -> Unit
            }
        }
    }
}

@Composable
private fun PostCard(post: Post, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier.fillMaxSize(),
    ) {
        Box {
            AsyncImage(
                model = post.previewUrl.ifBlank { post.displayUrl },
                contentDescription = "Пост ${post.id}",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .aspectRatio(1f),
            )
            Row(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Badge(text = post.score.toString(), primary = true)
                Badge(text = ratingMark(post.rating), primary = false)
            }
        }
    }
}

@Composable
private fun Badge(text: String, primary: Boolean) {
    val container = if (primary) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val content = if (primary) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(color = container, contentColor = content, shape = MaterialTheme.shapes.small) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
        )
    }
}
