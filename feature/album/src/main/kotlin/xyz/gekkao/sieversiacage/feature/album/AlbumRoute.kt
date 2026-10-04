package xyz.gekkao.sieversiacage.feature.album

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** 成長アルバム画面の遷移先。 */
@Serializable
data object AlbumRoute

fun NavGraphBuilder.albumScreen() {
    composable<AlbumRoute> { AlbumScreen() }
}
