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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView

private const val TAG = "Album"

/**
 * 記録した日を一覧し、選んだ日を再生する。
 * プレイヤーが読めなかった日は、保存した Sim の出力から 1 回だけ作り直す（ADR-0011）。
 */
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val documents = viewModel.documents
    val album = viewModel.album
    val scope = rememberCoroutineScope()
    // 画面に入るたびに読み直す。ショーケースで増えた日もここで見える
    val savedDays by album.days().collectAsStateWithLifecycle(emptyList())
    var selectedDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }
    // 作り直しても読めないときに繰り返さないよう、作り直した日を覚えておく
    val regeneratedDays = remember { mutableSetOf<Int>() }

    LaunchedEffect(savedDays) {
        if (selectedDay == null) selectedDay = savedDays.lastOrNull()
    }
    LaunchedEffect(selectedDay) {
        bytes = selectedDay?.let { album.document(it) }
    }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        bytes?.let { current ->
            PlantDocumentView(
                bytes = current,
                widthDp = documents.documentSizeDp,
                heightDp = documents.documentSizeDp,
                allowedShaders = documents.allowedShaders,
                onLoadFail = { error ->
                    val day = selectedDay ?: return@PlantDocumentView
                    Timber.tag(TAG).w(error, "day-$day を読めなかった")
                    if (regeneratedDays.add(day)) {
                        scope.launch { bytes = album.regenerate(day) }
                    }
                },
            )
            Text(text = "Day $selectedDay: ファイルから読込 / ${current.size} bytes")
        }
        Text(text = "アルバム（${savedDays.size} 日分）")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            savedDays.forEach { day ->
                FilterChip(
                    selected = selectedDay == day,
                    onClick = { selectedDay = day },
                    label = { Text("Day $day") },
                )
            }
        }
    }
}
