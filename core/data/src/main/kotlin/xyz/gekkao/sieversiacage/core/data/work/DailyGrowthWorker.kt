package xyz.gekkao.sieversiacage.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import xyz.gekkao.sieversiacage.core.data.GrowthTicker

/** 毎日の成長 tick（ADR-0021）。中身は [GrowthTicker] にあり、ここは呼んで翌日の分を予約するだけ。 */
@HiltWorker
class DailyGrowthWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val ticker: GrowthTicker,
        private val scheduler: GrowthWorkScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            try {
                ticker.catchUp()
            } finally {
                // 失敗しても翌日の分は入れる。抜けた日は次の tick かアプリの起動時に埋まる
                scheduler.scheduleNext()
            }
            return Result.success()
        }
    }
