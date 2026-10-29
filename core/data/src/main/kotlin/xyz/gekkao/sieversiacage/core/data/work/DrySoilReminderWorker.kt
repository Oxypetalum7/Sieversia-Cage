package xyz.gekkao.sieversiacage.core.data.work

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import xyz.gekkao.sieversiacage.core.data.ReminderNotifier

/** 夕方の水やりの通知（ADR-0021）。条件の判断は [ReminderNotifier] にあり、ここは呼んで翌日の分を予約するだけ。 */
@HiltWorker
class DrySoilReminderWorker
    @AssistedInject
    constructor(
        @Assisted context: Context,
        @Assisted params: WorkerParameters,
        private val notifier: ReminderNotifier,
        private val scheduler: GrowthWorkScheduler,
    ) : CoroutineWorker(context, params) {
        override suspend fun doWork(): Result {
            try {
                notifier.remindIfDry()
            } finally {
                scheduler.scheduleNextReminder()
            }
            return Result.success()
        }
    }
