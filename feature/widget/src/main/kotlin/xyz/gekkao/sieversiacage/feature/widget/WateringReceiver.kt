package xyz.gekkao.sieversiacage.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import timber.log.Timber

private const val TAG = "Watering"

/** ランチャーの内蔵プレイヤーが送るメタデータの extra（android-36 の RemoteViews.SetDrawInstructionAction）。 */
private const val EXTRA_METADATA = "remotecompose_metadata"

/**
 * ウィジェットの水やりのタップを受け取る（ADR-0014 の「記録は後追い」の入口）。
 * M2-1 の検証では、届いたことと届くまでの時間をログに出すだけ。記録は Sim の本実装（M2-4）で作る。
 */
class WateringReceiver : BroadcastReceiver() {
    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        Timber.tag(TAG).i(
            "widget tap received: elapsed=${SystemClock.elapsedRealtime()} " +
                "metadata=${intent.getStringExtra(EXTRA_METADATA)} extras=${intent.extras?.keySet()}",
        )
    }
}
