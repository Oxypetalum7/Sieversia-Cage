package xyz.gekkao.sieversiacage.feature.showcase

import androidx.compose.runtime.Immutable
import xyz.gekkao.sieversiacage.core.player.PlantDocument

/**
 * ショーケースの画面の状態。
 *
 * @property ageDays 表示している日（植えてからの経過日数）
 * @property todayAgeDays 今日は植えてから何日目か。読み込むまでは null
 * @property document 表示するドキュメント。今日はアルバムのファイルから読み、それ以外の日はプレビューを描く
 * @property documentSizeDp ドキュメントの基準サイズ（dp）
 * @property allowedShaders プレイヤーに許可させるシェーダー
 * @property showReminderPrompt 「土が乾いたら知らせますか？」の案内を出すか（ADR-0021）
 */
@Immutable
data class ShowcaseUiState(
    val ageDays: Int,
    val todayAgeDays: Int?,
    val document: PlantDocument?,
    val documentSizeDp: Int,
    val allowedShaders: Set<String>,
    val showReminderPrompt: Boolean = false,
) {
    /** 今日以外の日は「毎日水をやった」前提のプレビュー（ADR-0021）。アルバムには書かない。 */
    val isPreview: Boolean get() = todayAgeDays != null && ageDays != todayAgeDays
}

sealed interface ShowcaseIntent {
    data object NextDay : ShowcaseIntent

    data object PreviousDay : ShowcaseIntent

    /** プレビューをやめて今日の株に戻る。 */
    data object BackToToday : ShowcaseIntent

    /** 表示中のドキュメントの中身を logcat に出す（デバッグ用）。 */
    data object DumpDocument : ShowcaseIntent

    /** 案内で「知らせる」を選んだ。 */
    data object AcceptReminder : ShowcaseIntent

    /** 案内で「今はいい」を選んだ。 */
    data object DeclineReminder : ShowcaseIntent

    /** 通知の許可を求めた結果。 */
    data class ReportReminderPermission(
        val granted: Boolean,
    ) : ShowcaseIntent

    /** ドキュメントがホストアクションを送った（水やりのタップなど。ADR-0014）。 */
    data class ReceiveHostAction(
        val actionId: Int,
        val metadata: String?,
    ) : ShowcaseIntent
}

sealed interface ShowcaseEffect {
    /** 通知の許可を求める（画面が許可のダイアログを開く。ADR-0021）。 */
    data object RequestNotificationPermission : ShowcaseEffect
}
