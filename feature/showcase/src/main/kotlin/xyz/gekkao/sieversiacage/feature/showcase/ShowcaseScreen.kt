package xyz.gekkao.sieversiacage.feature.showcase

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
    val permissionLauncher =
        rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            viewModel.onIntent(ShowcaseIntent.ReportReminderPermission(granted))
        }
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ShowcaseEffect.RequestNotificationPermission -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        viewModel.onIntent(ShowcaseIntent.ReportReminderPermission(granted = true))
                    }
                }
            }
        }
    }
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
            val label =
                if (uiState.isPreview) {
                    "Day ${uiState.ageDays} のプレビュー（毎日水やり）"
                } else {
                    "Day ${uiState.ageDays}（今日）: アルバムから読込"
                }
            Text(text = "$label / ${document.size} bytes")
            TextButton(onClick = { onIntent(ShowcaseIntent.DumpDocument) }) {
                Text("中身を logcat にダンプ")
            }
        }
        Row {
            TextButton(onClick = { onIntent(ShowcaseIntent.PreviousDay) }) { Text("前の日") }
            TextButton(onClick = { onIntent(ShowcaseIntent.NextDay) }) { Text("次の日") }
        }
        if (uiState.isPreview) {
            TextButton(onClick = { onIntent(ShowcaseIntent.BackToToday) }) { Text("今日に戻る") }
        }
        if (uiState.showReminderPrompt) {
            // ADR-0021: 初めて水をやったあとに、何のための通知かが伝わる形で尋ねる（見た目は M3 で作り込む）
            Text(text = "土が乾いたら、夕方にお知らせしますか？")
            Row {
                TextButton(onClick = { onIntent(ShowcaseIntent.AcceptReminder) }) { Text("知らせる") }
                TextButton(onClick = { onIntent(ShowcaseIntent.DeclineReminder) }) { Text("今はいい") }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun ShowcaseContentPreview() {
    ShowcaseContent(
        uiState =
            ShowcaseUiState(
                ageDays = 5,
                todayAgeDays = 3,
                document = null,
                documentSizeDp = 320,
                allowedShaders = emptySet(),
            ),
        onIntent = {},
    )
}
