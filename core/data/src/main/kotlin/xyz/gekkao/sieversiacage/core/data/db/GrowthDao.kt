package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.OnConflictStrategy
import androidx.room3.Query

/** Sim の入力（植えた日と水やりの記録）。 */
@Dao
interface GrowthDao {
    @Query("SELECT * FROM plant WHERE id = $THE_PLANT_ID")
    suspend fun plant(): PlantEntity?

    /** すでに植えてあれば何もしない。 */
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlantIfAbsent(plant: PlantEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWateringIfAbsent(log: WateringLogEntity)

    @Query("UPDATE watering_log SET taps = taps + 1 WHERE date = :date")
    suspend fun incrementWatering(date: String)

    /** [from] 以上 [until] 未満の日の記録（日付の昇順）。日付は `yyyy-MM-dd` なので文字列の比較で足りる。 */
    @Query("SELECT * FROM watering_log WHERE date >= :from AND date < :until ORDER BY date")
    suspend fun wateringBetween(
        from: String,
        until: String,
    ): List<WateringLogEntity>
}
