package xyz.gekkao.sieversiacage.feature.showcase

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

/** ショーケース画面の遷移先。 */
@Serializable
data object ShowcaseRoute

fun NavGraphBuilder.showcaseScreen() {
    composable<ShowcaseRoute> { ShowcaseScreen() }
}
