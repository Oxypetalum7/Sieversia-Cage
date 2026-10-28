package xyz.gekkao.sieversiacage.feature.album

import androidx.compose.runtime.Immutable
import xyz.gekkao.sieversiacage.core.player.PlantDocument

/**
 * アルバムの画面の状態。
 *
 * @property days 記録した日（経過日数の昇順）
 * @property selectedDay 選んでいる日。記録がまだなければ null
 * @property document 選んでいる日のドキュメント。読み終わるまでは前の日のものか null
 * @property isUnreadable 作り直しても再生できなかった
 * @property documentSizeDp ドキュメントの基準サイズ（dp）
 * @property allowedShaders プレイヤーに許可させるシェーダー
 */
@Immutable
data class AlbumUiState(
    val days: List<Int>,
    val selectedDay: Int?,
    val document: PlantDocument?,
    val isUnreadable: Boolean,
    val documentSizeDp: Int,
    val allowedShaders: Set<String>,
)

sealed interface AlbumIntent {
    data class SelectDay(
        val day: Int,
    ) : AlbumIntent

    /** プレイヤーがドキュメントを読めなかった（版の違う `.rc` に未知の命令があるときなど）。 */
    data class ReportLoadFailure(
        val day: Int,
        val error: Throwable,
    ) : AlbumIntent
}
