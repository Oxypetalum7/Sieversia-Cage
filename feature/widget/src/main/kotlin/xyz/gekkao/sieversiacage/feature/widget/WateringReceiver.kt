package xyz.gekkao.sieversiacage.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.GrowthRepository
import xyz.gekkao.sieversiacage.core.data.WidgetRefresher
import javax.inject.Inject

private const val TAG = "Watering"

/**
 * タップからウィジェットを作り直すまでの待ち時間。ドキュメントを送り直すと跳ねる動き（約 0.6 秒）が初期状態に戻って
 * 途中で切れるので、動きが終わるのを待つ。
 */
private const val REFRESH_DELAY_MS = 1_000L

// onReceive が返ったあとも記録を続けるためのスコープ。goAsync の間だけプロセスが生かされる
private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * ウィジェットの水やりのタップを受け取り、水やりの記録に足す（ADR-0014 の「記録は後追い」、ADR-0020）。
 * 跳ねる反応はドキュメントの中で返っているので、ここでは記録して、ウィジェットに「今日の水やり: 済み」を出す（ADR-0021）。
 */
@AndroidEntryPoint
class WateringReceiver : BroadcastReceiver() {
    @Inject lateinit var growth: GrowthRepository

    @Inject lateinit var widgets: WidgetRefresher

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                growth.recordWatering()
                Timber.tag(TAG).i("widget tap recorded")
                // 連打したときは最後のタップのあとに 1 回だけ作り直す。前の予約が次のタップの跳ねる動きを切らないように
                scheduleRefresh(widgets).join()
            } finally {
                pending.finish()
            }
        }
    }
}

private var refreshJob: Job? = null

@Synchronized
private fun scheduleRefresh(widgets: WidgetRefresher): Job {
    refreshJob?.cancel()
    return receiverScope
        .launch {
            delay(REFRESH_DELAY_MS)
            widgets.refresh()
        }.also { refreshJob = it }
}
