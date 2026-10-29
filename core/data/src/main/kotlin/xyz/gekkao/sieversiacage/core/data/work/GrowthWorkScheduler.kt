package xyz.gekkao.sieversiacage.core.data.work

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import java.time.Duration
import java.time.LocalTime
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val DAILY_TICK_WORK = "daily-growth-tick"

/** 毎日の成長 tick を動かす時刻（端末の現地時刻）。日付が変わってすぐ（ADR-0021）。 */
private val DAILY_TICK_TIME: LocalTime = LocalTime.of(0, 5)

/** 毎日の成長 tick の予約（ADR-0021）。次の 0 時 5 分に一回きりの仕事を入れ、終わったら翌日の分を入れ直す。 */
@Singleton
class GrowthWorkScheduler
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val clock: Clock,
    ) {
        /** 予約がなければ入れる（アプリの起動時）。すでにあれば何もしない。 */
        fun ensureScheduled() = enqueue(ExistingWorkPolicy.KEEP)

        /** 翌日の分を入れる（tick の終わり）。動いている自分の仕事を止めないよう、後ろにつなぐ。 */
        fun scheduleNext() = enqueue(ExistingWorkPolicy.APPEND_OR_REPLACE)

        private fun enqueue(policy: ExistingWorkPolicy) {
            val delay = delayUntilNext(ZonedDateTime.now(clock), DAILY_TICK_TIME)
            val request =
                OneTimeWorkRequestBuilder<DailyGrowthWorker>()
                    .setInitialDelay(delay.toMillis(), TimeUnit.MILLISECONDS)
                    .build()
            WorkManager.getInstance(context).enqueueUniqueWork(DAILY_TICK_WORK, policy, request)
        }
    }

/** [now] から、次に [at] になるまでの時間。ちょうど [at] なら翌日の [at] まで。 */
internal fun delayUntilNext(
    now: ZonedDateTime,
    at: LocalTime,
): Duration {
    val todayAt = now.with(at)
    val next = if (todayAt.isAfter(now)) todayAt else todayAt.plusDays(1)
    return Duration.between(now, next)
}
