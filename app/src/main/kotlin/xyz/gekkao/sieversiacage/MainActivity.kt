package xyz.gekkao.sieversiacage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SIZE
import xyz.gekkao.sieversiacage.core.compiler.WriterPlantDocumentCompiler
import xyz.gekkao.sieversiacage.core.sim.simulate

private val compiler = WriterPlantDocumentCompiler()

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    SpikeScreen(modifier = Modifier.fillMaxSize().padding(innerPadding))
                }
            }
        }
    }
}

/** スパイク①: Sim → Compiler → ドキュメント → プレイヤー を一画面で通す。 */
@Composable
private fun SpikeScreen(modifier: Modifier = Modifier) {
    var ageDays by rememberSaveable { mutableIntStateOf(0) }
    val state = simulate(ageDays)
    val bytes = remember(state) { compiler.compile(state) }

    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
    ) {
        PlantDocumentView(bytes = bytes, widthDp = PLANT_DOCUMENT_SIZE, heightDp = PLANT_DOCUMENT_SIZE)
        Text(text = "Day ${state.ageDays}: ${state.stage} / ${bytes.size} bytes")
        Row {
            TextButton(onClick = { if (ageDays > 0) ageDays-- }) { Text("前の日") }
            TextButton(onClick = { ageDays++ }) { Text("次の日") }
        }
    }
}
