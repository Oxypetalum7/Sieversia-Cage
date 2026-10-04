@file:SuppressLint("RestrictedApi") // ADR-0010: 作成側の RestrictTo はこのモジュールに閉じ込める

package xyz.gekkao.sieversiacage.core.compiler

import android.annotation.SuppressLint
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.creation.Rc.FloatExpression.ADD
import androidx.compose.remote.creation.Rc.FloatExpression.CLAMP
import androidx.compose.remote.creation.Rc.FloatExpression.COS
import androidx.compose.remote.creation.Rc.FloatExpression.DIV
import androidx.compose.remote.creation.Rc.FloatExpression.MUL
import androidx.compose.remote.creation.Rc.FloatExpression.SIN
import androidx.compose.remote.creation.Rc.FloatExpression.SUB
import androidx.compose.remote.creation.Rc.Time.CONTINUOUS_SEC
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import xyz.gekkao.sieversiacage.core.sim.GrowthStage
import xyz.gekkao.sieversiacage.core.sim.PlantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/** ドキュメントの基準サイズ（dp）。再生側はこれをプレイヤーの大きさに使う。 */
const val PLANT_DOCUMENT_SIZE = 320
private const val PAINT_FILL = 0
private const val PAINT_STROKE = 1

private const val SKY = 0xFFE8F1F5.toInt()
private const val SOIL = 0xFF6D5A4B.toInt()
private const val STEM = 0xFF4E7D3A.toInt()
private const val PETAL = 0xFFFFFDF5.toInt()
private const val PETAL_EDGE = 0xFFD8D2C0.toInt()
private const val STAMEN = 0xFFE8C547.toInt()
private const val FLUFF = 0xFFC9B9A6.toInt()

private const val GROUND_RATIO = 0.8f
private const val FLOWER_RATIO = 0.35f
private const val STEM_WIDTH = 6f
private const val SPROUT_RADIUS = 4f
private const val LEAF_RADIUS = 8f
private const val STAMEN_RADIUS = 5f
private const val SEED_RADIUS = 4f
private const val FLUFF_WIDTH = 1.5f
private const val HALF = 0.5f
private const val TWO_PI = (2 * PI).toFloat()

// CONTINUOUS_SEC は毎正時に 3600 → 0 へ戻る。周期で 3600 秒が割り切れれば継ぎ目が出ない
private const val HOUR_SEC = 3600f

/** 揺れ: 1時間に 1146 往復（≒ 角速度 2 rad/s）。①の sin(t * 2) は 3600 秒で割り切れず、正時に跳んでいた。 */
private const val SWAY_SPEED = TWO_PI * 1146 / HOUR_SEC
private const val SWAY_DEGREES = 5f

/** 綿毛の回転: 毎秒 90 度。1時間で 900 回転ちょうど。 */
private const val FLUFF_DEGREES_PER_SEC = 90f
private const val FLUFF_COUNT = 12
private const val FLUFF_LENGTH = 26f

/** 開花の補間（スパイク用）: 8 秒周期で開いて閉じる。1時間に 450 周期。 */
private const val BLOOM_PERIOD_SEC = 8f

/** 水やりタップで跳ねる時間と、そのときの伸び率。 */
private const val BOUNCE_SEC = 0.6f
private const val BOUNCE_STRETCH = 0.15f

/** タップ時刻の初期値。t - lastTap が常に BOUNCE_SEC を超えるよう、十分に過去にしておく。 */
private const val NEVER_TAPPED = -10_000f

/** スパイク①③: 低レベル API（ADR-0009）でドキュメントを作る。③で式アニメーションとタップを足した。 */
class WriterPlantDocumentCompiler(
    private val profile: Profile = RcPlatformProfiles.ANDROIDX,
) : PlantDocumentCompiler {
    // obtain(w, h, contentDescription, profile) は説明文を捨てる（プロファイルのファクトリーが null を渡す）ため渡さない
    override fun compile(state: PlantState): ByteArray {
        val w =
            RemoteComposeWriter.obtain(PLANT_DOCUMENT_SIZE, PLANT_DOCUMENT_SIZE, profile)
        w.root {
            // 水やり: タップした瞬間の CONTINUOUS_SEC を lastTap に書き込む（式はタップ時にプレイヤーが評価する）
            val lastTap = w.addNamedFloat("lastTap", NEVER_TAPPED)
            val now = w.floatExpression(CONTINUOUS_SEC)
            val water = ValueFloatExpressionChange(Utils.idFromNan(lastTap), Utils.idFromNan(now))

            w.startCanvas(RecordingModifier().fillMaxSize().background(SKY).onClick(water))
            val width = w.addComponentWidthValue()
            val height = w.addComponentHeightValue()
            val centerX = w.floatExpression(width, 2f, DIV)
            val ground = w.floatExpression(height, GROUND_RATIO, MUL)
            val flowerY = w.floatExpression(height, FLOWER_RATIO, MUL)

            drawSky(w, width, height)
            w.rcPaint
                .setColor(SOIL)
                .setStyle(PAINT_FILL)
                .commit()
            w.drawRect(0f, ground, width, height)

            val sway = w.floatExpression(CONTINUOUS_SEC, SWAY_SPEED, MUL, SIN, SWAY_DEGREES, MUL)
            // progress = clamp((t - lastTap) / 0.6, 0, 1)、stretch = 1 + sin(progress * π) * 0.15
            // 正時をまたいだタップは t - lastTap が負になり、跳ねずに終わる（スパイクでは許容）
            val stretch =
                w.floatExpression(
                    CONTINUOUS_SEC,
                    lastTap,
                    SUB,
                    BOUNCE_SEC,
                    DIV,
                    1f,
                    0f,
                    CLAMP,
                    PI.toFloat(),
                    MUL,
                    SIN,
                    BOUNCE_STRETCH,
                    MUL,
                    1f,
                    ADD,
                )

            // 根元を支点に、揺らしてから縦に伸ばす
            w.save()
            w.rotate(sway, centerX, ground)
            w.scale(1f, stretch, centerX, ground)
            w.rcPaint
                .setColor(STEM)
                .setStyle(PAINT_STROKE)
                .setStrokeWidth(STEM_WIDTH)
                .commit()
            w.drawLine(centerX, ground, centerX, flowerY)
            w.translate(centerX, flowerY)
            // 花の形は基準サイズ（320）の座標で書いてある。Canvas の座標は実機のピクセルなので、幅に合わせて拡大する
            val unit = w.floatExpression(width, PLANT_DOCUMENT_SIZE.toFloat(), DIV)
            w.scale(unit, unit)
            drawFlowerHead(w, state.stage)
            w.restore()

            w.endCanvas()
        }
        return w.encodeToByteArray()
    }
}

/**
 * スパイク⑤: 空を AGSL で描く。上が青く下が明るいグラデーションに、斜めの光の帯がゆっくり流れる。
 * 位相は作成側の式で作って渡す（周期の調整をシェーダーに持ち込まない）。
 */
private val SKY_SHADER =
    """
    uniform float2 iResolution;
    uniform float iPhase;

    half4 main(float2 p) {
        float2 uv = p / iResolution;
        half3 top = half3(0.67, 0.81, 0.90);
        half3 bottom = half3(0.95, 0.93, 0.86);
        half3 c = mix(top, bottom, half(smoothstep(0.0, 0.8, uv.y)));
        float band = 0.5 + 0.5 * sin(uv.x * 9.42 - uv.y * 3.0 + iPhase);
        c += half3(0.06) * half(band * (1.0 - uv.y));
        return half4(c, 1.0);
    }
    """.trimIndent()

/**
 * このコンパイラが書き込むシェーダーの一覧。プレイヤーは既定ですべてのシェーダーを拒否するため、
 * 再生側はこの一覧と完全一致するものだけを許可する（ファイルを差し替えられても、知らないシェーダーは動かさない）。
 */
val PLANT_DOCUMENT_SHADERS: Set<String> = setOf(SKY_SHADER)

/** 光の帯: 1時間に 180 周（20 秒で 1 周）。3600 秒で割り切れる。 */
private const val SKY_SPEED = TWO_PI * 180 / HOUR_SEC

private fun drawSky(
    w: RemoteComposeWriter,
    width: Float,
    height: Float,
) {
    val phase = w.floatExpression(CONTINUOUS_SEC, SKY_SPEED, MUL)
    val shader =
        w
            .createShader(SKY_SHADER)
            .setFloatUniform("iResolution", width, height)
            .setFloatUniform("iPhase", phase)
            .commit()
    // シェーダーが使えない環境（API 33 未満）では、この色の単色になる
    w.rcPaint
        .setColor(SKY)
        .setStyle(PAINT_FILL)
        .setShader(shader)
        .commit()
    w.drawRect(0f, 0f, width, height)
    w.rcPaint.setShader(0).commit()
}

/** 花の部分を、花の中心を原点とし、基準サイズ（320）を単位とする座標で描く。 */
private fun drawFlowerHead(
    w: RemoteComposeWriter,
    stage: GrowthStage,
) {
    when (stage) {
        GrowthStage.SPROUT -> {
            drawDisc(w, PETAL, SPROUT_RADIUS)
        }

        GrowthStage.LEAF -> {
            drawDisc(w, PETAL, LEAF_RADIUS)
        }

        GrowthStage.BUD -> {
            drawPetals(w, tween = 0f)
        }

        GrowthStage.BLOOM -> {
            // tween = (1 - cos(t * 2π / 8)) / 2 = cos(...) * -0.5 + 0.5。スパイクでは開閉を繰り返して補間が動くことだけ確かめる
            val tween =
                w.floatExpression(CONTINUOUS_SEC, TWO_PI / BLOOM_PERIOD_SEC, MUL, COS, -HALF, MUL, HALF, ADD)
            drawPetals(w, tween)
            drawDisc(w, STAMEN, STAMEN_RADIUS)
        }

        GrowthStage.SEED_HEAD -> {
            drawFluff(w)
        }
    }
}

private fun drawDisc(
    w: RemoteComposeWriter,
    color: Int,
    radius: Float,
) {
    w.rcPaint
        .setColor(color)
        .setStyle(PAINT_FILL)
        .commit()
    w.drawCircle(0f, 0f, radius)
}

/** つぼみ（tween = 0）と花（tween = 1）の間を drawTweenPath で補間する。 */
private fun drawPetals(
    w: RemoteComposeWriter,
    tween: Float,
) {
    val bud = w.addPathData(petalPath(BUD_SHAPE))
    val bloom = w.addPathData(petalPath(BLOOM_SHAPE))
    w.rcPaint
        .setColor(PETAL)
        .setStyle(PAINT_FILL)
        .commit()
    w.drawTweenPath(bud, bloom, tween, 0f, 1f)
    w.rcPaint
        .setColor(PETAL_EDGE)
        .setStyle(PAINT_STROKE)
        .setStrokeWidth(1f)
        .commit()
    w.drawTweenPath(bud, bloom, tween, 0f, 1f)
}

/** 綿毛: 放射状の線を、毎秒 90 度で回す。 */
private fun drawFluff(w: RemoteComposeWriter) {
    val spin = w.floatExpression(CONTINUOUS_SEC, FLUFF_DEGREES_PER_SEC, MUL)
    w.save()
    w.rotate(spin)
    w.rcPaint
        .setColor(FLUFF)
        .setStyle(PAINT_STROKE)
        .setStrokeWidth(FLUFF_WIDTH)
        .commit()
    repeat(FLUFF_COUNT) { i ->
        val angle = TWO_PI * i / FLUFF_COUNT
        w.drawLine(0f, 0f, cos(angle) * FLUFF_LENGTH, sin(angle) * FLUFF_LENGTH)
    }
    w.restore()
    drawDisc(w, STAMEN, SEED_RADIUS)
}

/** 5 枚の花びらの形。補間できるよう、つぼみと花でコマンドの並びを同じにする。 */
private class PetalShape(
    val centerDegrees: (Int) -> Float,
    val spreadDegrees: Float,
    val length: Float,
)

private const val PETAL_COUNT = 5
private val BUD_SHAPE = PetalShape(centerDegrees = { i -> -90f + (i - 2) * 6f }, spreadDegrees = 10f, length = 20f)
private val BLOOM_SHAPE = PetalShape(centerDegrees = { i -> -90f + i * 72f }, spreadDegrees = 28f, length = 30f)

/** 中心から出て中心に戻る三次ベジェを 5 回つなぐ。 */
private fun petalPath(shape: PetalShape): RemotePath =
    RemotePath().apply {
        moveTo(0f, 0f)
        repeat(PETAL_COUNT) { i ->
            val center = shape.centerDegrees(i)
            val (x1, y1) = polar(center - shape.spreadDegrees, shape.length)
            val (x2, y2) = polar(center + shape.spreadDegrees, shape.length)
            cubicTo(x1, y1, x2, y2, 0f, 0f)
        }
        close()
    }

private fun polar(
    degrees: Float,
    radius: Float,
): Pair<Float, Float> {
    val rad = Math.toRadians(degrees.toDouble())
    return (cos(rad) * radius).toFloat() to (sin(rad) * radius).toFloat()
}
