package xyz.gekkao.sieversiacage.core.sim

/**
 * Sim が出力する株の状態（ADR-0020）。Compiler 層はこれだけを見てドキュメントを組み立てる。
 *
 * @property ageDays 植えてからの経過日数
 * @property cycle 周回数。最初の一生が 0 で、綿毛のあと葉の時期に戻るたびに 1 増える
 * @property stage 今の成長段階
 * @property stageProgress 段階の中での進み具合（0〜1）
 * @property growth 今の周の中で積み上げた成長ポイント
 * @property branchCount 枝の数。周を重ねるごとに増える
 * @property flowerCount 花の数（開花・綿毛の段階で描く数）。周を重ねるごとに増える
 * @property soilMoisture 土の湿り気（0〜1）。水をやった日に 1 になり、やらない日が続くと乾く
 * @property vigor 元気さ（0〜1）。土の湿り気を追いかけて変わり、成長の速さに効く
 */
data class PlantState(
    val ageDays: Int,
    val cycle: Int,
    val stage: GrowthStage,
    val stageProgress: Float,
    val growth: Float,
    val branchCount: Int,
    val flowerCount: Int,
    val soilMoisture: Float,
    val vigor: Float,
)
