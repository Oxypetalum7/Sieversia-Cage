package xyz.gekkao.sieversiacage.core.data

import xyz.gekkao.sieversiacage.core.compiler.DocumentTarget
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SHADERS
import xyz.gekkao.sieversiacage.core.compiler.PLANT_DOCUMENT_SIZE
import xyz.gekkao.sieversiacage.core.compiler.WriterPlantDocumentCompiler
import xyz.gekkao.sieversiacage.core.sim.simulate
import javax.inject.Inject
import javax.inject.Singleton

/**
 * ドキュメント生成の窓口（ADR-0016）。feature はコンパイラの型（DocumentTarget など）に触れず、ここを通して作る。
 */
@Singleton
class PlantDocumentRepository
    @Inject
    constructor() {
        private val inAppCompiler = WriterPlantDocumentCompiler(DocumentTarget.IN_APP)

        // ADR-0012: ウィジェットは WIDGETS_V6 で書く
        private val widgetCompiler = WriterPlantDocumentCompiler(DocumentTarget.WIDGET_V6)

        /** ドキュメントの基準サイズ（dp）。再生側はこれをプレイヤーの大きさに使う。 */
        val documentSizeDp: Int = PLANT_DOCUMENT_SIZE

        /** アプリ内のプレイヤーに許可させるシェーダー。コンパイラが書き込むものと完全一致する。 */
        val allowedShaders: Set<String> = PLANT_DOCUMENT_SHADERS

        /** アプリ内（ショーケース・アルバム）で再生するドキュメント。 */
        fun inAppDocument(ageDays: Int): ByteArray = inAppCompiler.compile(simulate(ageDays))

        /** ホーム画面のウィジェットに載せるドキュメント。 */
        fun widgetDocument(ageDays: Int): ByteArray = widgetCompiler.compile(simulate(ageDays))
    }
