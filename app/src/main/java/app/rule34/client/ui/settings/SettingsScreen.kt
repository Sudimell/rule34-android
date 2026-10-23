package app.rule34.client.ui.settings

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rule34.client.AppContainer
import app.rule34.client.data.CredentialsRepository
import app.rule34.client.data.FeedStore
import app.rule34.client.data.Rule34Api
import app.rule34.client.data.SettingsRepository
import app.rule34.client.data.ThemeMode
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, FlowPreview::class)
@Composable
fun SettingsScreen(container: AppContainer) {
    val viewModel = viewModel<SettingsViewModel>(factory = SettingsViewModel.factory(container))
    val settings by viewModel.settings.collectAsStateWithLifecycle(app.rule34.client.data.UserSettings())
    val blacklist by viewModel.blacklist.collectAsStateWithLifecycle(emptyList())
    val suggestions by viewModel.suggestions.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var confirmClear by remember { mutableStateOf(false) }
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Тема", style = MaterialTheme.typography.titleMedium)
        SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ThemeMode.entries.forEachIndexed { index, mode ->
                    SegmentedButton(
                        selected = settings.theme == mode,
                        onClick = { viewModel.setTheme(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index, ThemeMode.entries.size),
                    ) {
                        Text(
                            when (mode) {
                                ThemeMode.SYSTEM -> "Система"
                                ThemeMode.LIGHT -> "Светлая"
                                ThemeMode.DARK -> "Тёмная"
                            },
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Динамический цвет", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "С Android 12 берётся из обоев. Ниже — запасная схема.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Switch(
                    checked = settings.dynamicColor,
                    onCheckedChange = viewModel::setDynamicColor,
                )
            }

        Text("Чёрный список", style = MaterialTheme.typography.titleMedium)
        Text(
            "Эти теги добавляются к поиску как исключение. Если тег выбран вручную, исключение не ставится.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = viewModel.draft,
            onValueChange = viewModel::onDraft,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            label = { Text("Тег") },
            trailingIcon = {
                TextButton(
                    onClick = { viewModel.addDraft() },
                    enabled = viewModel.draft.isNotBlank(),
                ) { Text("Добавить") }
            },
        )
        suggestions.forEach { suggestion ->
            ListItem(
                headlineContent = { Text(suggestion.label) },
                modifier = Modifier.clickable { viewModel.addBlacklist(suggestion.value) },
            )
        }
        blacklist.forEach { tag ->
            ListItem(
                headlineContent = { Text(tag) },
                trailingContent = {
                    IconButton(onClick = { viewModel.removeBlacklist(tag) }) {
                        Icon(Icons.Default.Close, contentDescription = "Убрать $tag")
                    }
                },
            )
        }

        Text("История", style = MaterialTheme.typography.titleMedium)
        TextButton(onClick = { confirmClear = true }) { Text("Очистить историю") }

        Text("Ключ API", style = MaterialTheme.typography.titleMedium)
        Text(
            "Ключ сохранён на устройстве.",
            style = MaterialTheme.typography.bodyMedium,
        )
        TextButton(onClick = viewModel::forgetCredentials) { Text("Забыть ключ") }
        TextButton(onClick = { context.open("https://api.rule34.xxx/") }) {
            Text("Условия API")
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("Очистить историю?") },
            text = { Text("Список просмотренных постов будет удалён с устройства.") },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.clearHistory()
                    confirmClear = false
                }) { Text("Очистить") }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) { Text("Отмена") }
            },
        )
    }
}

private fun android.content.Context.open(url: String) {
    runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
}

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val credentials: CredentialsRepository,
    private val feed: FeedStore,
    private val api: Rule34Api,
) : ViewModel() {
    val settings = settingsRepository.state
    val blacklist = feed.blacklist()
    var draft by mutableStateOf("")
        private set

    fun onDraft(value: String) {
        draft = value
    }

    val suggestions = snapshotFlow { draft }
        .map { it.trim() }
        .debounce(300)
        .flatMapLatest { query ->
            flow {
                if (query.length < 2) emit(emptyList())
                else emit(runCatching { api.autocomplete(query) }.getOrDefault(emptyList()))
            }
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { settingsRepository.setTheme(mode) }
    }

    fun setDynamicColor(enabled: Boolean) {
        viewModelScope.launch { settingsRepository.setDynamicColor(enabled) }
    }

    fun addDraft() {
        addBlacklist(draft)
    }

    fun addBlacklist(tag: String) {
        viewModelScope.launch {
            feed.addBlacklist(tag)
            draft = ""
        }
    }

    fun removeBlacklist(tag: String) {
        viewModelScope.launch { feed.removeBlacklist(tag) }
    }

    fun clearHistory() {
        viewModelScope.launch { feed.clearHistory() }
    }

    fun forgetCredentials() {
        credentials.clear()
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SettingsViewModel(
                        container.settings,
                        container.credentials,
                        container.feed,
                        container.api,
                    ) as T
                }
            }
    }
}
