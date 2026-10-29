package xyz.gekkao.sieversiacage.core.data

import android.content.Context
import android.util.TypedValue
import dagger.hilt.android.qualifiers.ApplicationContext
import xyz.gekkao.sieversiacage.core.compiler.DocumentTarget
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SHADERS
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SIZE
import xyz.gekkao.sieversiacage.core.compiler.WATER_ACTION_ID
import xyz.gekkao.sieversiacage.core.compiler.WriterPlantDocumentCompiler
import xyz.gekkao.sieversiacage.core.sim.PlantState
import xyz.gekkao.sieversiacage.core.sim.simulateWateredDaily
import java.time.Clock
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ドキュメント生成の窓口（ADR-0016）。feature はコンパイラの型（DocumentTarget など）に触れず、ここを通して作る。
 */
@Singleton
class PlantDocumentRepository
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) {
        private val inAppCompiler = WriterPlantDocumentCompiler(DocumentTarget.IN_APP)

        /** ドキュメントの基準サイズ（dp）。再生側はこれをプレイヤーの大きさに使う。 */
        val documentSizeDp: Int = PLANT_DOCUMENT_SIZE

        /** 水やりのタップでドキュメントがホストに送るアクションの ID（ADR-0014）。 */
        val waterActionId: Int = WATER_ACTION_ID

        /** アプリ内のプレイヤーに許可させるシェーダー。コンパイラが書き込むものと完全一致する。 */
        val allowedShaders: Set<String> = PLANT_DOCUMENT_SHADERS

        /** アプリ内（ショーケース・アルバム）で再生するドキュメント。 */
        fun inAppDocument(ageDays: Int): ByteArray = inAppCompiler.compile(preview(ageDays))

        /** ホーム画面のウィジェットに載せるドキュメント。 */
        fun widgetDocument(ageDays: Int): ByteArray {
            // ADR-0012: ウィジェットは WIDGETS_V6 で書く。寸法と文字の大きさは、そのときの画面密度とフォントサイズの設定で px に直す（M2-2）
            val metrics = context.resources.displayMetrics
            val spToPx = { sp: Float -> TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, sp, metrics) }
            return WriterPlantDocumentCompiler(
                DocumentTarget.WIDGET_V6,
                metrics.density,
                spToPx,
            ).compile(preview(ageDays))
        }

        // 水やりの記録から Sim を回すのは毎日の成長 tick（M2-5）から。それまでは「今日まで毎日水をやった」前提で描く
        private fun preview(ageDays: Int): PlantState {
            val today = LocalDate.now(clock)
            return simulateWateredDaily(today.minusDays(ageDays.toLong()), ageDays)
        }
    }
