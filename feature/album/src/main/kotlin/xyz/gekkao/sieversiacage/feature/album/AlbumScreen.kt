package xyz.gekkao.sieversiacage.feature.album

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView

/** アルバムの画面。ViewModel とつなぐだけ。 */
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    AlbumContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/** アルバムの中身。状態と操作だけを受け取る。 */
@Composable
fun AlbumContent(
    uiState: AlbumUiState,
    onIntent: (AlbumIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        val day = uiState.selectedDay
        val document = uiState.document
        if (day != null && document != null) {
            PlantDocumentView(
                document = document,
                widthDp = uiState.documentSizeDp,
                heightDp = uiState.documentSizeDp,
                allowedShaders = uiState.allowedShaders,
                onLoadFail = { error -> onIntent(AlbumIntent.ReportLoadFailure(day, error)) },
            )
            Text(text = "Day $day: ファイルから読込 / ${document.size} bytes")
        }
        if (day != null && uiState.isUnreadable) {
            Text(text = "Day $day は表示できませんでした")
        }
        Text(text = "アルバム（${uiState.days.size} 日分）")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            uiState.days.forEach { savedDay ->
                FilterChip(
                    selected = day == savedDay,
                    onClick = { onIntent(AlbumIntent.SelectDay(savedDay)) },
                    label = { Text("Day $savedDay") },
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun AlbumContentPreview() {
    AlbumContent(
        uiState =
            AlbumUiState(
                days = listOf(0, 1, 2, 3),
                selectedDay = 2,
                document = null,
                isUnreadable = true,
                documentSizeDp = 320,
                allowedShaders = emptySet(),
            ),
        onIntent = {},
    )
}
