package xyz.gekkao.sieversiacage.feature.showcase

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView

/** ショーケースの画面。ViewModel とつなぐだけ。 */
@Composable
fun ShowcaseScreen(
    modifier: Modifier = Modifier,
    viewModel: ShowcaseViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ShowcaseContent(
        uiState = uiState,
        onIntent = viewModel::onIntent,
        modifier = modifier,
    )
}

/** ショーケースの中身。状態と操作だけを受け取る。 */
@Composable
fun ShowcaseContent(
    uiState: ShowcaseUiState,
    onIntent: (ShowcaseIntent) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        uiState.document?.let { document ->
            PlantDocumentView(
                document = document,
                widthDp = uiState.documentSizeDp,
                heightDp = uiState.documentSizeDp,
                allowedShaders = uiState.allowedShaders,
                onAction = { actionId, metadata -> onIntent(ShowcaseIntent.ReceiveHostAction(actionId, metadata)) },
            )
            Text(text = "Day ${uiState.ageDays}: 記録→読込 / ${document.size} bytes")
            TextButton(onClick = { onIntent(ShowcaseIntent.DumpDocument) }) {
                Text("中身を logcat にダンプ")
            }
        }
        Row {
            TextButton(onClick = { onIntent(ShowcaseIntent.PreviousDay) }) { Text("前の日") }
            TextButton(onClick = { onIntent(ShowcaseIntent.NextDay) }) { Text("次の日") }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShowcaseContentPreview() {
    ShowcaseContent(
        uiState = ShowcaseUiState(ageDays = 3, document = null, documentSizeDp = 320, allowedShaders = emptySet()),
        onIntent = {},
    )
}
