package xyz.gekkao.sieversiacage.feature.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.GrowthRepository
import javax.inject.Inject

private const val TAG = "Watering"

// onReceive が返ったあとも記録を続けるためのスコープ。goAsync の間だけプロセスが生かされる
private val receiverScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/**
 * ウィジェットの水やりのタップを受け取り、水やりの記録に足す（ADR-0014 の「記録は後追い」、ADR-0020）。
 * 跳ねる反応はドキュメントの中で返っているので、ここでは記録だけをする。
 */
@AndroidEntryPoint
class WateringReceiver : BroadcastReceiver() {
    @Inject lateinit var growth: GrowthRepository

    override fun onReceive(
        context: Context,
        intent: Intent,
    ) {
        val pending = goAsync()
        receiverScope.launch {
            try {
                growth.recordWatering()
                Timber.tag(TAG).i("widget tap recorded")
            } finally {
                pending.finish()
            }
        }
    }
}
