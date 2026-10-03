@file:SuppressLint("RestrictedApi") // ADR-0010: 作成側の RestrictTo はこのモジュールに閉じ込める

package xyz.gekkao.sieversiacage.core.compiler

import android.annotation.SuppressLint
import androidx.compose.remote.creation.Rc.FloatExpression.DIV
import androidx.compose.remote.creation.Rc.FloatExpression.MUL
import androidx.compose.remote.creation.Rc.FloatExpression.SIN
import androidx.compose.remote.creation.Rc.Time.CONTINUOUS_SEC
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import xyz.gekkao.sieversiacage.core.sim.GrowthStage
import xyz.gekkao.sieversiacage.core.sim.PlantState

/** ドキュメントの基準サイズ（dp）。再生側はこれをプレイヤーの大きさに使う。 */
const val PLANT_DOCUMENT_SIZE = 320
private const val PAINT_FILL = 0
private const val PAINT_STROKE = 1

private const val SKY = 0xFFE8F1F5.toInt()
private const val SOIL = 0xFF6D5A4B.toInt()
private const val STEM = 0xFF4E7D3A.toInt()
private const val PETAL = 0xFFFFFDF5.toInt()

private const val GROUND_RATIO = 0.8f
private const val FLOWER_RATIO = 0.35f
private const val SWAY_SPEED = 2f
private const val SWAY_DEGREES = 5f
private const val STEM_WIDTH = 6f

/** スパイク①: 低レベル API（ADR-0009）で最小のドキュメントを作る。 */
class WriterPlantDocumentCompiler(
    private val profile: Profile = RcPlatformProfiles.ANDROIDX,
) : PlantDocumentCompiler {
    override fun compile(state: PlantState): ByteArray {
        val w =
            RemoteComposeWriter.obtain(
                PLANT_DOCUMENT_SIZE,
                PLANT_DOCUMENT_SIZE,
                "Sieversia Cage day ${state.ageDays}",
                profile,
            )
        w.root {
            w.startCanvas(RecordingModifier().fillMaxSize().background(SKY))
            val width = w.addComponentWidthValue()
            val height = w.addComponentHeightValue()
            val centerX = w.floatExpression(width, 2f, DIV)
            val ground = w.floatExpression(height, GROUND_RATIO, MUL)
            val flowerY = w.floatExpression(height, FLOWER_RATIO, MUL)

            w.rcPaint
                .setColor(SOIL)
                .setStyle(PAINT_FILL)
                .commit()
            w.drawRect(0f, ground, width, height)

            // sway = sin(t * 2) * 5 度。根元を支点に揺らす
            val sway = w.floatExpression(CONTINUOUS_SEC, SWAY_SPEED, MUL, SIN, SWAY_DEGREES, MUL)
            w.save()
            w.rotate(sway, centerX, ground)
            w.rcPaint
                .setColor(STEM)
                .setStyle(PAINT_STROKE)
                .setStrokeWidth(STEM_WIDTH)
                .commit()
            w.drawLine(centerX, ground, centerX, flowerY)
            w.rcPaint
                .setColor(PETAL)
                .setStyle(PAINT_FILL)
                .commit()
            w.drawCircle(centerX, flowerY, flowerRadius(state.stage))
            w.restore()

            w.endCanvas()
        }
        return w.encodeToByteArray()
    }
}

private fun flowerRadius(stage: GrowthStage): Float =
    when (stage) {
        GrowthStage.SPROUT -> 4f
        GrowthStage.LEAF -> 8f
        GrowthStage.BUD -> 14f
        GrowthStage.BLOOM -> 28f
        GrowthStage.SEED_HEAD -> 22f
    }
