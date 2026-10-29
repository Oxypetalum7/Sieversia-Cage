package xyz.gekkao.sieversiacage.core.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "GrowthTicker"

/**
 * 毎日の成長 tick の中身（ADR-0021）。Worker・アプリの起動時・ショーケースから呼ぶ。
 *
 * 1. まだ植えていなければ今日植える。初めて植えたときは、それより前のアルバムの記録を消す
 * 2. アルバムに記録した最後の日の翌日から今日まで、1 日ずつ状態を求めて記録する（抜けた日もまとめて埋める）
 * 3. 新しく記録した日があれば、ウィジェットを作り直す
 */
@Singleton
class GrowthTicker
    @Inject
    constructor(
        private val growth: GrowthRepository,
        private val album: AlbumRepository,
        private val widgets: WidgetRefresher,
    ) {
        // Worker とアプリの起動時が同時に動いても、同じ日を二重に進めない
        private val mutex = Mutex()

        /** @return 新しく記録した日数 */
        suspend fun catchUp(): Int =
            mutex.withLock {
                val planting = growth.ensurePlanted()
                if (planting.isNew) album.clear()
                val today = growth.todayAgeDays()
                val from = (album.lastRecordedDay() ?: -1) + 1
                // 日ごとに植えた日から Sim を回し直す。日数が増えて遅くなったら、途中の状態を持ち回す（ADR-0020）
                for (ageDays in from..today) {
                    album.record(growth.stateOn(planting.plantedOn.plusDays(ageDays.toLong())))
                }
                val recorded = (today - from + 1).coerceAtLeast(0)
                if (recorded > 0 || planting.isNew) widgets.refresh()
                Timber.tag(TAG).i("caught up: recorded $recorded day(s), today is day $today")
                recorded
            }
    }
