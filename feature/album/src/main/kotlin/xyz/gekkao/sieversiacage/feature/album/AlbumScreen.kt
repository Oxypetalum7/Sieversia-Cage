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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import xyz.gekkao.sieversiacage.core.player.PlantDocumentView

/** 保存済みの日を一覧し、選んだ日をファイルだけから再生する（スパイク④のまま）。 */
@Composable
fun AlbumScreen(
    modifier: Modifier = Modifier,
    viewModel: AlbumViewModel = hiltViewModel(),
) {
    val documents = viewModel.documents
    val store = viewModel.store
    // 画面に入るたびに読み直す。ショーケースで増えた日もここで見える
    val savedDays = remember { store.savedDays() }
    var selectedDay by rememberSaveable { mutableStateOf(savedDays.lastOrNull()) }
    val bytes = remember(selectedDay) { selectedDay?.let(store::load) }

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
            Text(text = "Day $selectedDay: ファイルから読込 / ${bytes.size} bytes")
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
