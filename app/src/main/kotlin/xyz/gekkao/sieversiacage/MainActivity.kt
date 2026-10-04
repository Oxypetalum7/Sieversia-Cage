package xyz.gekkao.sieversiacage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import dagger.hilt.android.AndroidEntryPoint
import xyz.gekkao.sieversiacage.feature.album.AlbumRoute
import xyz.gekkao.sieversiacage.feature.album.albumScreen
import xyz.gekkao.sieversiacage.feature.showcase.ShowcaseRoute
import xyz.gekkao.sieversiacage.feature.showcase.showcaseScreen
import kotlin.reflect.KClass

/** ナビゲーションバーの項目。:app は画面遷移だけを持つ（ADR-0016）。 */
private enum class TopLevelDestination(
    val route: Any,
    val routeClass: KClass<*>,
    val iconRes: Int,
    val labelRes: Int,
) {
    SHOWCASE(ShowcaseRoute, ShowcaseRoute::class, R.drawable.ic_nav_showcase, R.string.nav_showcase),
    ALBUM(AlbumRoute, AlbumRoute::class, R.drawable.ic_nav_album, R.string.nav_album),
}

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                SieversiaCageApp()
            }
        }
    }
}

@Composable
private fun SieversiaCageApp(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    Scaffold(
        modifier = modifier,
        bottomBar = { TopLevelNavigationBar(navController) },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = ShowcaseRoute,
            modifier = Modifier.fillMaxSize().padding(innerPadding),
        ) {
            showcaseScreen()
            albumScreen()
        }
    }
}

@Composable
private fun TopLevelNavigationBar(
    navController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val currentDestination = navController.currentBackStackEntryAsState().value?.destination
    NavigationBar(modifier = modifier) {
        TopLevelDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = currentDestination?.hasRoute(destination.routeClass) == true,
                onClick = {
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(painterResource(destination.iconRes), contentDescription = null) },
                label = { Text(stringResource(destination.labelRes)) },
            )
        }
    }
}
