package app.rule34.client.ui.age

import android.app.Activity
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

@Composable
fun AgeGateScreen(onConfirm: () -> Unit) {
    val activity = LocalContext.current as? Activity
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.Start,
    ) {
        Column(Modifier.widthIn(max = 480.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Text("Только 18+", style = MaterialTheme.typography.headlineLarge)
            Text(
                "Здесь взрослые рисунки и видео с rule34.xxx. " +
                    "Дальше можно идти только если вам уже есть 18 лет.",
                style = MaterialTheme.typography.bodyLarge,
            )
            Button(onClick = onConfirm) { Text("Мне есть 18") }
            TextButton(onClick = { activity?.finish() }) { Text("Выйти") }
        }
    }
}
