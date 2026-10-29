package xyz.gekkao.sieversiacage.core.data

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import xyz.gekkao.sieversiacage.core.data.db.GrowthDao
import xyz.gekkao.sieversiacage.core.data.db.PlantEntity
import xyz.gekkao.sieversiacage.core.data.db.THE_PLANT_ID
import xyz.gekkao.sieversiacage.core.data.db.WateringLogEntity
import xyz.gekkao.sieversiacage.core.sim.DayInput
import xyz.gekkao.sieversiacage.core.sim.PlantState
import xyz.gekkao.sieversiacage.core.sim.simulate
import java.time.Clock
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Sim の入力（植えた日と水やりの記録）の窓口と、そこから求めた株の状態（ADR-0020）。
 *
 * 日付は端末の現地時刻で区切る。その日の水やりが効くのは翌日の成長から。
 */
@Singleton
class GrowthRepository
    @Inject
    constructor(
        private val dao: GrowthDao,
        private val clock: Clock,
    ) {
        // ウィジェットとアプリ内のタップが同時に来ても、回数を取りこぼさない
        private val mutex = Mutex()

        /** 植えた日。まだ植えていなければ、今日植えたことにする。 */
        suspend fun plantedOn(): LocalDate =
            mutex.withLock {
                dao.insertPlantIfAbsent(PlantEntity(THE_PLANT_ID, today().toString()))
                LocalDate.parse(checkNotNull(dao.plant()).plantedOn)
            }

        /** 今日の水やりを 1 回記録する（ウィジェットとアプリ内のタップ）。 */
        suspend fun recordWatering() {
            mutex.withLock {
                val date = today().toString()
                dao.insertWateringIfAbsent(WateringLogEntity(date, taps = 0))
                dao.incrementWatering(date)
            }
        }

        /** [date] の株の状態。植えた日から前日までの水やりの記録で Sim を回す。 */
        suspend fun stateOn(date: LocalDate = today()): PlantState {
            val plantedOn = plantedOn()
            require(!date.isBefore(plantedOn)) { "date must not be before planting: $date < $plantedOn" }
            val taps = dao.wateringBetween(plantedOn.toString(), date.toString()).associate { it.date to it.taps }
            // LocalDate.datesUntil は Android では API 34 からなので、日数を数えて 1 日ずつ足す（minSdk 29）
            val days = ChronoUnit.DAYS.between(plantedOn, date)
            val inputs =
                (0 until days).map { offset ->
                    val day = plantedOn.plusDays(offset)
                    DayInput(day, taps[day.toString()] ?: 0)
                }
            return simulate(inputs)
        }

        private fun today(): LocalDate = LocalDate.now(clock)
    }
