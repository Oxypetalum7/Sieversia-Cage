@file:SuppressLint("RestrictedApi") // ADR-0010: 作成側の RestrictTo はこのモジュールに閉じ込める

package xyz.gekkao.sieversiacage.core.compiler

import android.annotation.SuppressLint
import androidx.compose.remote.core.operations.Utils
import androidx.compose.remote.core.operations.layout.managers.BoxLayout
import androidx.compose.remote.core.operations.layout.managers.CollapsiblePriority
import androidx.compose.remote.core.operations.layout.managers.ColumnLayout
import androidx.compose.remote.core.operations.layout.managers.CoreText
import androidx.compose.remote.core.operations.layout.managers.RowLayout
import androidx.compose.remote.creation.Rc.FloatExpression.ADD
import androidx.compose.remote.creation.Rc.FloatExpression.CLAMP
import androidx.compose.remote.creation.Rc.FloatExpression.COS
import androidx.compose.remote.creation.Rc.FloatExpression.DIV
import androidx.compose.remote.creation.Rc.FloatExpression.MIN
import androidx.compose.remote.creation.Rc.FloatExpression.MUL
import androidx.compose.remote.creation.Rc.FloatExpression.SIN
import androidx.compose.remote.creation.Rc.FloatExpression.SUB
import androidx.compose.remote.creation.Rc.Time.CONTINUOUS_SEC
import androidx.compose.remote.creation.RemoteComposeWriter
import androidx.compose.remote.creation.RemotePath
import androidx.compose.remote.creation.actions.HostAction
import androidx.compose.remote.creation.actions.ValueFloatExpressionChange
import androidx.compose.remote.creation.modifiers.RecordingModifier
import androidx.compose.remote.creation.profile.Profile
import androidx.compose.remote.creation.profile.RcPlatformProfiles
import xyz.gekkao.sieversiacage.core.sim.GrowthStage
import xyz.gekkao.sieversiacage.core.sim.PlantState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

/** ドキュメントの基準サイズ（dp）。再生側はこれをプレイヤーの大きさに使う。 */
const val PLANT_DOCUMENT_SIZE = 320
private const val PAINT_FILL = 0
private const val PAINT_STROKE = 1

private const val SKY = 0xFFE8F1F5.toInt()

/** 湿った土。乾くにつれて [SOIL_DRY] に近づく（見た目は M3 で作り込む）。 */
private const val SOIL = 0xFF6D5A4B.toInt()
private const val SOIL_DRY = 0xFFB09A80.toInt()
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

// M2-2: ウィジェットの骨組みの寸法（dp）と文字の大きさ（sp）。見た目は M3 で作り込む
private const val PLANT_MIN_WIDTH_DP = 160f
private const val PLANT_MIN_HEIGHT_DP = 160f
private const val STATUS_WIDTH_DP = 150f
private const val MEMO_HEIGHT_DP = 96f
private const val PANEL_PADDING_DP = 12f
private const val TITLE_SP = 20f
private const val BODY_SP = 14f
private const val FONT_WEIGHT_NORMAL = 400f
private const val TEXT_MAX_LINES = 3
private const val TEXT_COLOR = 0xFF3B3A36.toInt()
private const val MEMO_BACKGROUND = 0xFFF4F1E8.toInt()

/** availableLength で、しきい値の前後を 0 か 1 にはっきり分けるための係数。 */
private const val STEP_SHARPNESS = 1000f

/** 折りたたみの優先度。小さいほど先に消える。 */
private const val PRIORITY_KEEP = 2f
private const val PRIORITY_DROP_FIRST = 1f

/**
 * 低レベル API（ADR-0009）でドキュメントを作る。スパイク①③で植物の Canvas を、M2-2 でウィジェットの三段リフローの骨組みを足した。
 *
 * @param density 端末の画面密度。ウィジェットの寸法（dp）を px に直すのに使う。Canvas とレイアウトの座標は実機のピクセル
 * @param spToPx 文字の大きさ（sp）を px に直す。端末の「フォントサイズ」の設定も反映する。
 *   V6 は文字の大きさに式を使えないので、作るときに決める。設定が変わったら作り直す必要がある
 */
class WriterPlantDocumentCompiler(
    private val target: DocumentTarget = DocumentTarget.IN_APP,
    private val density: Float = 1f,
    private val spToPx: (Float) -> Float = { it * density },
) : PlantDocumentCompiler {
    private val profile: Profile =
        when (target) {
            DocumentTarget.IN_APP -> RcPlatformProfiles.ANDROIDX
            DocumentTarget.WIDGET_V6 -> RcPlatformProfiles.WIDGETS_V6
            DocumentTarget.WIDGET_V7 -> RcPlatformProfiles.WIDGETS_V7
        }

    // WIDGETS_V7 は DATA_SHADER を書けない（書き込み時に例外）。V6 は書けるので、②で OS 側に外されるかを確かめるため残す
    private val useShader = target != DocumentTarget.WIDGET_V7

    // obtain(w, h, contentDescription, profile) は説明文を捨てる（プロファイルのファクトリーが null を渡す）ため渡さない
    override fun compile(
        state: PlantState,
        display: DisplayInfo,
    ): ByteArray {
        val w =
            RemoteComposeWriter.obtain(PLANT_DOCUMENT_SIZE, PLANT_DOCUMENT_SIZE, profile)
        w.root {
            when (target) {
                DocumentTarget.IN_APP -> drawPlantCanvas(w, state, display, RecordingModifier().fillMaxSize())
                DocumentTarget.WIDGET_V6, DocumentTarget.WIDGET_V7 -> widgetLayout(w, state, display)
            }
        }
        return w.encodeToByteArray()
    }

    /**
     * M2-2: ウィジェットの三段リフロー（発注書: 2x2 = 鉢のクローズアップ / 4x2 = 株全体＋状態 / 4x4 = 全景＋成長メモ）。
     * ドキュメントの中だけで組み変わり、アプリには戻らない。
     *
     * 植物の Canvas を全面に敷き、その上に折りたたみ可能な横並び（状態の欄）と縦並び（成長メモ）を重ねる。
     * 折りたたみの判断は大きさの決まった部品だけで行う（weight を付けた部品は測る時点で大きさが決まらず、判断が崩れた）。
     * 植物の最小の大きさは中身のない箱で場所取りし、場所が足りなければ優先度の低い欄が消える。
     * Canvas は同じしきい値を式で判断し、欄が出ているときはその分を避けて植物を描く。
     */
    private fun widgetLayout(
        w: RemoteComposeWriter,
        state: PlantState,
        display: DisplayInfo,
    ) {
        val reserve =
            PanelReserve(
                plantMinWidth = dp(PLANT_MIN_WIDTH_DP),
                statusWidth = dp(STATUS_WIDTH_DP),
                plantMinHeight = dp(PLANT_MIN_HEIGHT_DP),
                memoHeight = dp(MEMO_HEIGHT_DP),
            )
        w.box(RecordingModifier().fillMaxSize(), BoxLayout.START, BoxLayout.TOP) {
            drawPlantCanvas(w, state, display, RecordingModifier().fillMaxSize(), reserve)

            // 横: 植物の最小幅 + 状態の欄が入らなければ、状態の欄が消える。並びは右に寄せ、状態の欄を右端に置く
            w.collapsibleRow(RecordingModifier().fillMaxSize(), RowLayout.END, RowLayout.TOP) {
                spacer(w, RecordingModifier().width(reserve.plantMinWidth).height(1f))
                val status =
                    RecordingModifier()
                        .width(reserve.statusWidth)
                        .padding(dp(PANEL_PADDING_DP))
                        .collapsiblePriority(CollapsiblePriority.HORIZONTAL, PRIORITY_DROP_FIRST)
                w.column(status, ColumnLayout.START, ColumnLayout.TOP) {
                    text(w, "Day ${state.ageDays}", TITLE_SP)
                    text(w, stageLabel(state.stage), BODY_SP)
                    // ADR-0021: その日の水やりは株に効くのは翌日からなので、記録されたことをここで見せる
                    text(w, if (display.wateredToday) "今日の水やり: 済み" else "今日の水やり: まだ", BODY_SP)
                }
            }

            // 縦: 植物の最小の高さ + 成長メモが入らなければ、成長メモが消える。並びは下に寄せ、成長メモを下端に置く
            w.collapsibleColumn(RecordingModifier().fillMaxSize(), ColumnLayout.START, ColumnLayout.BOTTOM) {
                spacer(w, RecordingModifier().width(1f).height(reserve.plantMinHeight))
                val memo =
                    RecordingModifier()
                        .fillMaxWidth()
                        .height(reserve.memoHeight)
                        .background(MEMO_BACKGROUND)
                        .padding(dp(PANEL_PADDING_DP))
                        .collapsiblePriority(CollapsiblePriority.VERTICAL, PRIORITY_DROP_FIRST)
                w.box(memo, BoxLayout.START, BoxLayout.TOP) {
                    text(w, "成長メモ（仮）: 花が開きはじめた。明日は花びらがもう少し広がりそう。", BODY_SP)
                }
            }
        }
    }

    /** 場所取りだけをする中身のない箱。折りたたみの優先度は最も高い（消えない）。 */
    private fun spacer(
        w: RemoteComposeWriter,
        modifier: RecordingModifier,
    ) {
        w.box(
            modifier.collapsiblePriority(CollapsiblePriority.HORIZONTAL, PRIORITY_KEEP),
            BoxLayout.START,
            BoxLayout.TOP,
        ) {}
    }

    private fun text(
        w: RemoteComposeWriter,
        value: String,
        sizeSp: Float,
    ) {
        w.textComponent(
            RecordingModifier(),
            w.addText(value),
            TEXT_COLOR,
            spToPx(sizeSp),
            0,
            FONT_WEIGHT_NORMAL,
            null,
            CoreText.TEXT_ALIGN_START,
            CoreText.OVERFLOW_ELLIPSIS,
            TEXT_MAX_LINES,
        ) {}
    }

    private fun dp(value: Float): Float = value * density

    /** 植物を描く Canvas。タップすると跳ねて、ホストにも水やりを知らせる。 */
    private fun drawPlantCanvas(
        w: RemoteComposeWriter,
        state: PlantState,
        display: DisplayInfo,
        modifier: RecordingModifier,
        reserve: PanelReserve? = null,
    ) {
        // 水やり: タップした瞬間の CONTINUOUS_SEC を lastTap に書き込む（式はタップ時にプレイヤーが評価する）
        val lastTap = w.addNamedFloat("lastTap", NEVER_TAPPED)
        val now = w.floatExpression(CONTINUOUS_SEC)
        val water = ValueFloatExpressionChange(Utils.idFromNan(lastTap), Utils.idFromNan(now))
        // ADR-0014: 跳ねる反応はドキュメントの中で返し、同じタップでホストにも知らせて記録は後追いにする
        val notifyHost = HostAction(WATER_ACTION_ID)

        w.startCanvas(modifier.background(SKY).onClick(water, notifyHost))
        val width = w.addComponentWidthValue()
        val height = w.addComponentHeightValue()
        // 植物を描く範囲。ウィジェットでは、状態の欄と成長メモが出ているときにその分を避ける
        val plantWidth = reserve?.let { availableLength(w, width, it.plantMinWidth, it.statusWidth) } ?: width
        val plantHeight = reserve?.let { availableLength(w, height, it.plantMinHeight, it.memoHeight) } ?: height
        val centerX = w.floatExpression(plantWidth, 2f, DIV)
        val ground = w.floatExpression(plantHeight, GROUND_RATIO, MUL)
        val flowerY = w.floatExpression(plantHeight, FLOWER_RATIO, MUL)

        drawSky(w, width, height, useShader)
        // ADR-0021: 土の見た目は、その日の水やりをすぐ反映する（Sim の湿り気は昨日までの記録で決まる）
        val moisture = if (display.wateredToday) 1f else state.soilMoisture
        w.rcPaint
            .setColor(soilColor(moisture))
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
        // 花の形は基準サイズ（320）の座標で書いてある。Canvas の座標は実機のピクセルなので、描く範囲の短い辺に合わせて拡大する
        val unit = w.floatExpression(plantWidth, plantHeight, MIN, PLANT_DOCUMENT_SIZE.toFloat(), DIV)
        w.scale(unit, unit)
        drawFlowerHead(w, state.stage)
        w.restore()

        w.endCanvas()
    }
}

/**
 * ウィジェットで、植物のために残しておく大きさと、その横・下に出す欄の大きさ（px）。
 * 折りたたみ可能なレイアウトと Canvas の式で、同じしきい値を使う。
 */
private class PanelReserve(
    val plantMinWidth: Float,
    val statusWidth: Float,
    val plantMinHeight: Float,
    val memoHeight: Float,
)

/**
 * 欄が出ているときは total - panel、出ていないときは total。
 * 欄が出る条件（total >= plantMin + panel）を、shown = clamp((total - plantMin - panel) * 大きな数 + 1, 0, 1) で 0 か 1 にする。
 * CLAMP は「値, 最大, 最小」の順に積む。
 */
private fun availableLength(
    w: RemoteComposeWriter,
    total: Float,
    plantMin: Float,
    panel: Float,
): Float {
    val shown = w.floatExpression(total, plantMin + panel, SUB, STEP_SHARPNESS, MUL, 1f, ADD, 1f, 0f, CLAMP)
    return w.floatExpression(total, shown, panel, MUL, SUB)
}

/** 土の湿り気（0〜1）から土の色を決める。1 で [SOIL]、0 で [SOIL_DRY]。 */
private fun soilColor(moisture: Float): Int {
    val m = moisture.coerceIn(0f, 1f)

    fun channel(shift: Int): Int {
        val wet = (SOIL shr shift) and CHANNEL_MASK
        val dry = (SOIL_DRY shr shift) and CHANNEL_MASK
        return (dry + (wet - dry) * m).roundToInt() shl shift
    }
    return OPAQUE or channel(RED_SHIFT) or channel(GREEN_SHIFT) or channel(BLUE_SHIFT)
}

private const val CHANNEL_MASK = 0xFF
private const val RED_SHIFT = 16
private const val GREEN_SHIFT = 8
private const val BLUE_SHIFT = 0
private const val OPAQUE = 0xFF shl 24

private fun stageLabel(stage: GrowthStage): String =
    when (stage) {
        GrowthStage.SPROUT -> "芽生え"
        GrowthStage.LEAF -> "葉がひらいた"
        GrowthStage.BUD -> "つぼみ"
        GrowthStage.BLOOM -> "開花"
        GrowthStage.SEED_HEAD -> "綿毛"
    }

/**
 * スパイク⑤: 空を AGSL で描く。上が青く下が明るいグラデーションに、斜めの細い光の帯が右へ流れる。
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
        float band = pow(0.5 + 0.5 * sin(uv.x * 9.42 - uv.y * 3.0 - iPhase), 6.0);
        c += half3(0.25) * half(band * (1.0 - uv.y));
        return half4(c, 1.0);
    }
    """.trimIndent()

/**
 * このコンパイラが書き込むシェーダーの一覧。プレイヤーは既定ですべてのシェーダーを拒否するため、
 * 再生側はこの一覧と完全一致するものだけを許可する（ファイルを差し替えられても、知らないシェーダーは動かさない）。
 */
val PLANT_DOCUMENT_SHADERS: Set<String> = setOf(SKY_SHADER)

/** 光の帯: 1時間に 360 周（10 秒で 1 周）。3600 秒で割り切れる。 */
private const val SKY_SPEED = TWO_PI * 360 / HOUR_SEC

private fun drawSky(
    w: RemoteComposeWriter,
    width: Float,
    height: Float,
    useShader: Boolean,
) {
    if (!useShader) {
        w.rcPaint
            .setColor(SKY)
            .setStyle(PAINT_FILL)
            .commit()
        w.drawRect(0f, 0f, width, height)
        return
    }
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
