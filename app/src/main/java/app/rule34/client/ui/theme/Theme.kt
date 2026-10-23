package app.rule34.client.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.rule34.client.data.ThemeMode
import com.materialkolor.DynamicMaterialTheme

private val Seed = Color(0xFF6750A4)

@Composable
fun Rule34Theme(
    themeMode: ThemeMode,
    dynamicColor: Boolean,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    if (dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context = LocalContext.current
        val scheme = if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        MaterialTheme(colorScheme = scheme, content = content)
    } else {
        DynamicMaterialTheme(
            seedColor = Seed,
            isDark = dark,
            content = content,
        )
    }
}
