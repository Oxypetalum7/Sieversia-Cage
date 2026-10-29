package xyz.gekkao.sieversiacage.feature.showcase

import androidx.compose.runtime.Immutable
import xyz.gekkao.sieversiacage.core.player.PlantDocument

/**
 * ショーケースの画面の状態。
 *
 * @property ageDays 表示する経過日数
 * @property document アルバムに記録してから読み直したドキュメント。読み終わるまでは前の日のものか null
 * @property documentSizeDp ドキュメントの基準サイズ（dp）
 * @property allowedShaders プレイヤーに許可させるシェーダー
 */
@Immutable
data class ShowcaseUiState(
    val ageDays: Int,
    val document: PlantDocument?,
    val documentSizeDp: Int,
    val allowedShaders: Set<String>,
)

sealed interface ShowcaseIntent {
    data object NextDay : ShowcaseIntent

    data object PreviousDay : ShowcaseIntent

    /** 表示中のドキュメントの中身を logcat に出す（デバッグ用）。 */
    data object DumpDocument : ShowcaseIntent

    /** ドキュメントがホストアクションを送った（水やりのタップなど。ADR-0014）。 */
    data class ReceiveHostAction(
        val actionId: Int,
        val metadata: String?,
    ) : ShowcaseIntent
}
