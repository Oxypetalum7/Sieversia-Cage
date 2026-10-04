package xyz.gekkao.sieversiacage

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
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
import dagger.hilt.android.AndroidEntryPoint
import xyz.gekkao.sieversiacage.core.data.AlbumStore
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView
import xyz.gekkao.sieversiacage.core.player.dumpDocument
import javax.inject.Inject

private const val TAG = "Spike4"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var documents: PlantDocumentRepository

    @Inject lateinit var store: AlbumStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    SpikeScreen(
                        documents = documents,
                        store = store,
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                    )
                }
            }
        }
    }
}

/**
 * スパイク①④: Sim → Compiler → ファイル保存 → 読込 → プレイヤー を一画面で通す。
 * 再生するのは常に「ファイルから読み直したバイト列」。
 */
@Composable
private fun SpikeScreen(
    documents: PlantDocumentRepository,
    store: AlbumStore,
    modifier: Modifier = Modifier,
) {
    var ageDays by rememberSaveable { mutableIntStateOf(0) }
    // null = 今日（ageDays）を生成して保存する。数値 = アルバムからその日を再生する
    var albumDay by rememberSaveable { mutableStateOf<Int?>(null) }
    var savedDays by remember { mutableStateOf(store.savedDays()) }
    // 同じ日を上書きしても savedDays は変わらないため、保存のたびに進めて読み直させる
    var saveCount by remember { mutableIntStateOf(0) }

    LaunchedEffect(ageDays) {
        val generated = documents.inAppDocument(ageDays)
        val file = store.save(ageDays, generated)
        val reloaded = store.load(ageDays)
        Log.i(TAG, "saved ${file.name} ${generated.size}B, roundTrip=${reloaded?.contentEquals(generated)}")
        savedDays = store.savedDays()
        saveCount++
    }

    val playingDay = albumDay ?: ageDays
    val bytes =
        remember(playingDay, savedDays, saveCount) {
            store.load(playingDay).takeIf { playingDay in savedDays }
        }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        if (bytes != null) {
            PlantDocumentView(
                bytes = bytes,
                widthDp = documents.documentSizeDp,
                heightDp = documents.documentSizeDp,
                allowedShaders = documents.allowedShaders,
            )
            val source = if (albumDay == null) "今日（生成→保存→読込）" else "アルバム（ファイルから読込）"
            Text(text = "Day $playingDay: $source / ${bytes.size} bytes")
            TextButton(onClick = { Log.i(TAG, "dump day-$playingDay.rc\n" + dumpDocument(bytes)) }) {
                Text("中身を logcat にダンプ")
            }
        }
        Row {
            TextButton(onClick = {
                albumDay = null
                if (ageDays > 0) ageDays--
            }) { Text("前の日") }
            TextButton(onClick = {
                albumDay = null
                ageDays++
            }) { Text("次の日") }
        }
        Text(text = "アルバム（${savedDays.size} 日分）")
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            savedDays.forEach { day ->
                FilterChip(
                    selected = albumDay == day,
                    onClick = { albumDay = if (albumDay == day) null else day },
                    label = { Text("Day $day") },
                )
            }
        }
    }
}
