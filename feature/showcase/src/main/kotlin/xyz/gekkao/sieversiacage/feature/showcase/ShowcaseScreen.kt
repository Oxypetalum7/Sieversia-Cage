package xyz.gekkao.sieversiacage.feature.showcase

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView
import xyz.gekkao.sieversiacage.core.player.dumpDocument

private const val TAG = "Showcase"

/**
 * 今日の株を生成 → 保存 → 読込 → 再生する（スパイク①④のまま）。
 * 再生するのは常に「ファイルから読み直したバイト列」。
 */
@Composable
fun ShowcaseScreen(
    modifier: Modifier = Modifier,
    viewModel: ShowcaseViewModel = hiltViewModel(),
) {
    val documents = viewModel.documents
    val store = viewModel.store
    var ageDays by rememberSaveable { mutableIntStateOf(0) }
    var bytes by remember { mutableStateOf<ByteArray?>(null) }

    LaunchedEffect(ageDays) {
        val generated = documents.inAppDocument(ageDays)
        val file = store.save(ageDays, generated)
        val reloaded = store.load(ageDays)
        Log.i(TAG, "saved ${file.name} ${generated.size}B, roundTrip=${reloaded?.contentEquals(generated)}")
        bytes = reloaded
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
            )
            Text(text = "Day $ageDays: 生成→保存→読込 / ${current.size} bytes")
            TextButton(onClick = { Log.i(TAG, "dump day-$ageDays.rc\n" + dumpDocument(current)) }) {
                Text("中身を logcat にダンプ")
            }
        }
        Row {
            TextButton(onClick = { if (ageDays > 0) ageDays-- }) { Text("前の日") }
            TextButton(onClick = { ageDays++ }) { Text("次の日") }
        }
    }
}
