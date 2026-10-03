package xyz.gekkao.sieversiacage

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import xyz.gekkao.sieversiacage.core.sim.simulate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme {
                Scaffold { innerPadding ->
                    Box(
                        modifier = Modifier.fillMaxSize().padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        SimStageLabel(ageDays = 0)
                    }
                }
            }
        }
    }
}

@Composable
private fun SimStageLabel(
    ageDays: Int,
    modifier: Modifier = Modifier,
) {
    Text(text = "Day $ageDays: ${simulate(ageDays).stage}", modifier = modifier)
}

@Preview
@Composable
private fun SimStageLabelPreview() {
    MaterialTheme { SimStageLabel(ageDays = 10) }
}
