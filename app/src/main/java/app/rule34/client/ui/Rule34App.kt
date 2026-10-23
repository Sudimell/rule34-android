package app.rule34.client.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import app.rule34.client.AppContainer
import app.rule34.client.ui.age.AgeGateScreen
import app.rule34.client.ui.library.LibraryScreen
import app.rule34.client.ui.post.PostScreen
import app.rule34.client.ui.search.SearchScreen
import app.rule34.client.ui.settings.SettingsScreen
import app.rule34.client.ui.setup.ApiSetupScreen
import app.rule34.client.ui.theme.Rule34Theme
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import androidx.compose.runtime.rememberCoroutineScope

@Serializable
data object SearchRoute

@Serializable
data object LibraryRoute

@Serializable
data object SettingsRoute

@Serializable
data class PostRoute(val source: String, val query: String, val startIndex: Int)

@Composable
fun Rule34App(container: AppContainer) {
    val settings by container.settings.state.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val current = settings
    Rule34Theme(
        themeMode = current?.theme ?: app.rule34.client.data.ThemeMode.SYSTEM,
        dynamicColor = current?.dynamicColor ?: true,
    ) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            when {
                current == null -> Box(Modifier.fillMaxSize())
                !current.ageConfirmed -> Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    AgeGateScreen(onConfirm = { scope.launch { container.settings.confirmAge() } })
                }
                !current.hasCredentials -> Box(Modifier.windowInsetsPadding(WindowInsets.safeDrawing)) {
                    ApiSetupScreen(container)
                }
                else -> MainNav(container)
            }
        }
    }
}

@Composable
private fun MainNav(container: AppContainer) {
    val nav = rememberNavController()
    NavHost(
        navController = nav,
        startDestination = SearchRoute,
        modifier = Modifier.windowInsetsPadding(WindowInsets.safeDrawing),
    ) {
        composable<SearchRoute> {
            AppScaffold(selected = 0, onSelect = { dest -> nav.switchTopLevel(dest) }) {
                SearchScreen(
                    container = container,
                    onPost = { query, index ->
                        nav.navigate(PostRoute("search", query, index))
                    },
                )
            }
        }
        composable<LibraryRoute> {
            AppScaffold(selected = 1, onSelect = { dest -> nav.switchTopLevel(dest) }) {
                LibraryScreen(
                    container = container,
                    onPost = { source, index -> nav.navigate(PostRoute(source, "", index)) },
                )
            }
        }
        composable<SettingsRoute> {
            AppScaffold(selected = 2, onSelect = { dest -> nav.switchTopLevel(dest) }) {
                SettingsScreen(container)
            }
        }
        composable<PostRoute> { entry ->
            val route = entry.toRoute<PostRoute>()
            PostScreen(
                container = container,
                source = route.source,
                query = route.query,
                startIndex = route.startIndex,
                onBack = { nav.popBackStack() },
                onTag = { tag ->
                    container.searchRelay.push(tag)
                    nav.switchTopLevel(0)
                },
            )
        }
    }
}

private fun androidx.navigation.NavHostController.switchTopLevel(index: Int) {
    val route = when (index) {
        1 -> LibraryRoute
        2 -> SettingsRoute
        else -> SearchRoute
    }
    navigate(route) {
        popUpTo(SearchRoute) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

@Composable
private fun AppScaffold(
    selected: Int,
    onSelect: (Int) -> Unit,
    content: @Composable () -> Unit,
) {
    NavigationSuiteScaffold(
        navigationSuiteItems = {
            item(
                selected = selected == 0,
                onClick = { onSelect(0) },
                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                label = { Text("Поиск") },
            )
            item(
                selected = selected == 1,
                onClick = { onSelect(1) },
                icon = { Icon(Icons.Default.Favorite, contentDescription = null) },
                label = { Text("Библиотека") },
            )
            item(
                selected = selected == 2,
                onClick = { onSelect(2) },
                icon = { Icon(Icons.Default.Settings, contentDescription = null) },
                label = { Text("Настройки") },
            )
        },
    ) {
        Box(Modifier.padding()) { content() }
    }
}
