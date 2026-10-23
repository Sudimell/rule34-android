package app.rule34.client.ui.setup

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import app.rule34.client.AppContainer
import app.rule34.client.data.CredentialsRepository
import app.rule34.client.data.Rule34Api
import app.rule34.client.ui.userMessage
import kotlinx.coroutines.launch

@Composable
fun ApiSetupScreen(container: AppContainer) {
    val viewModel = viewModel<SetupViewModel>(factory = SetupViewModel.factory(container))
    val context = LocalContext.current
    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Column(Modifier.widthIn(max = 560.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Ключ API", style = MaterialTheme.typography.headlineMedium)
            Text(
                "С августа 2025 API rule34 просит личный ключ. Войдите в аккаунт, откройте настройки " +
                    "и блок API Access Credentials. Один ключ на аккаунт: не публикуйте его. " +
                    "Лимит — около 60 запросов в минуту.",
                style = MaterialTheme.typography.bodyLarge,
            )
            TextButton(onClick = {
                context.startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("https://rule34.xxx/index.php?page=account&s=options"),
                    ),
                )
            }) { Text("Настройки аккаунта") }
            TextButton(onClick = {
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://api.rule34.xxx/")))
            }) { Text("Условия API") }
            OutlinedTextField(
                value = viewModel.userId,
                onValueChange = { viewModel.userId = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("User id") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            )
            OutlinedTextField(
                value = viewModel.apiKey,
                onValueChange = { viewModel.apiKey = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                label = { Text("API key") },
                visualTransformation = PasswordVisualTransformation(),
            )
            Button(onClick = viewModel::check, enabled = !viewModel.busy) {
                if (viewModel.busy) {
                    CircularProgressIndicator()
                } else {
                    Text("Проверить")
                }
            }
            viewModel.message?.let { text ->
                Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

class SetupViewModel(
    private val api: Rule34Api,
    private val credentials: CredentialsRepository,
) : ViewModel() {
    var userId by mutableStateOf(credentials.credentials.value?.userId.orEmpty())
    var apiKey by mutableStateOf(credentials.credentials.value?.apiKey.orEmpty())
    var busy by mutableStateOf(false)
    var message by mutableStateOf<String?>(null)

    fun check() {
        val user = userId.trim()
        val key = apiKey.trim()
        if (user.isEmpty() || key.isEmpty()) {
            message = "Нужны оба поля."
            return
        }
        viewModelScope.launch {
            busy = true
            message = null
            val result = runCatching { api.posts(user, key, tags = "", pid = 0, limit = 1) }
            busy = false
            result.fold(
                onSuccess = { posts ->
                    credentials.save(user, key)
                    message = if (posts.isEmpty()) "Ключ принят, лента пустая." else "Ключ работает."
                },
                onFailure = { error -> message = error.userMessage() },
            )
        }
    }

    companion object {
        fun factory(container: AppContainer): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    @Suppress("UNCHECKED_CAST")
                    return SetupViewModel(container.api, container.credentials) as T
                }
            }
    }
}
