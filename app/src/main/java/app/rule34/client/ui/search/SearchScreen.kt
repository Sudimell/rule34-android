package app.rule34.client.ui.search

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.DockedSearchBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.paging.compose.collectAsLazyPagingItems
import app.rule34.client.AppContainer
import app.rule34.client.data.FeedStore
import app.rule34.client.data.QueryBuilder
import app.rule34.client.data.Rule34Api
import app.rule34.client.data.SearchRelay
import app.rule34.client.ui.common.PostGrid
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.PagingData
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class, FlowPreview::class)
@Composable
fun SearchScreen(
    container: AppContainer,
    onPost: (query: String, index: Int) -> Unit,
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<SearchViewModel>(
        factory = SearchViewModel.factory(container),
    )
    val chips by viewModel.chips.collectAsStateWithLifecycle()
    val raw by viewModel.raw.collectAsStateWithLifecycle()
    val draft by viewModel.draftState.collectAsStateWithLifecycle()
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val builtQuery by viewModel.builtQuery.collectAsStateWithLifecycle()
    val posts = viewModel.posts.collectAsLazyPagingItems()
    var expanded by rememberSaveable { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        DockedSearchBar(
            inputField = {
                SearchBarDefaults.InputField(
                    query = draft,
                    onQueryChange = {
                        viewModel.draftState.value = it
                        expanded = true
                    },
                    onSearch = {
                        viewModel.submitDraft()
                        expanded = false
                    },
                    expanded = expanded,
                    onExpandedChange = { expanded = it },
                    placeholder = { Text("Теги") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                )
            },
            expanded = expanded && suggestions.isNotEmpty(),
            onExpandedChange = { expanded = it },
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
        ) {
            suggestions.forEach { suggestion ->
                ListItem(
                    headlineContent = { Text(suggestion.label) },
                    modifier = Modifier.clickable {
                        viewModel.addChip(suggestion.value)
                        viewModel.draftState.value = ""
                        expanded = false
                    },
                )
            }
        }
        if (chips.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                chips.forEach { tag ->
                    InputChip(
                        selected = true,
                        onClick = { viewModel.removeChip(tag) },
                        label = { Text(tag) },
                        trailingIcon = {
                            Icon(Icons.Default.Close, contentDescription = "Убрать $tag")
                        },
                    )
                }
            }
        }
        OutlinedTextField(
            value = raw,
            onValueChange = viewModel::setRaw,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
            label = { Text("Операторы") },
            supportingText = { Text("rating:explicit, score:>10") },
        )
        if (builtQuery.isNotBlank()) {
            Text(
                text = builtQuery,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
        }
        PostGrid(
            items = posts,
            onPost = { index -> onPost(builtQuery, index) },
            modifier = Modifier.weight(1f),
        )
    }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SearchViewModel(
    private val api: Rule34Api,
    private val feed: FeedStore,
    private val relay: SearchRelay,
    private val savedState: SavedStateHandle,
) : ViewModel() {
    val chips = savedState.getStateFlow("chips", arrayListOf<String>())
    val raw = savedState.getStateFlow("raw", "")
    val draftState = MutableStateFlow("")

    init {
        viewModelScope.launch {
            relay.drain().forEach(::addChip)
            relay.updates.collect { relay.drain().forEach(::addChip) }
        }
    }

    val suggestions = draftState
        .map { it.trim() }
        .debounce(300)
        .flatMapLatest { token ->
            flow {
                if (token.length < 2 || token.any { it == ':' || it == '>' || it == '<' }) {
                    emit(emptyList())
                } else {
                    emit(runCatching { api.autocomplete(token) }.getOrDefault(emptyList()))
                }
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val builtQuery = combine(chips, raw, feed.blacklist()) { selected, operators, blacklist ->
        QueryBuilder.build(selected, operators, blacklist)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), "")

    val posts: Flow<PagingData<app.rule34.client.data.Post>> =
        builtQuery.flatMapLatest { query -> feed.search(query) }

    fun addChip(tag: String) {
        val normalized = QueryBuilder.normalizeTag(tag)
        if (normalized.isEmpty() || normalized.any { it == ':' || it == '>' || it == '<' }) return
        val next = ArrayList(chips.value)
        if (normalized !in next) next += normalized
        savedState["chips"] = next
    }

    fun removeChip(tag: String) {
        savedState["chips"] = ArrayList(chips.value.filterNot { it == tag })
    }

    fun setRaw(value: String) {
        savedState["raw"] = value
    }

    fun submitDraft() {
        val text = draftState.value.trim()
        if (text.isEmpty()) return
        text.split(Regex("\\s+")).forEach { token ->
            if (token.startsWith("-") || token.any { it == ':' || it == '>' || it == '<' }) {
                val next = listOf(raw.value, token).filter { it.isNotBlank() }.joinToString(" ")
                savedState["raw"] = next
            } else {
                addChip(token)
            }
        }
        draftState.value = ""
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>, extras: CreationExtras): T {
                    @Suppress("UNCHECKED_CAST")
                    return SearchViewModel(
                        api = container.api,
                        feed = container.feed,
                        relay = container.searchRelay,
                        savedState = extras.createSavedStateHandle(),
                    ) as T
                }
            }
    }
}
