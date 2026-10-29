package xyz.gekkao.sieversiacage.core.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ADR-0018 / ADR-0020: Sim の入力の DAO を、本番と同じ同梱の SQLite で実機で確かめる。
 * minSdk 29 では DEX のメソッド名に空白を使えないので、テスト名は空白なしで書く。
 */
@RunWith(AndroidJUnit4::class)
class GrowthDaoTest {
    private lateinit var db: SieversiaDatabase
    private lateinit var dao: GrowthDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder<SieversiaDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
        dao = db.growthDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `植えた日は最初の1回だけ記録される`() =
        runTest {
            dao.insertPlantIfAbsent(PlantEntity(THE_PLANT_ID, "2026-05-01"))
            dao.insertPlantIfAbsent(PlantEntity(THE_PLANT_ID, "2026-06-01"))

            assertEquals("2026-05-01", dao.plant()?.plantedOn)
        }

    @Test
    fun `同じ日の水やりは回数が足される`() =
        runTest {
            repeat(3) {
                dao.insertWateringIfAbsent(WateringLogEntity("2026-05-01", 0))
                dao.incrementWatering("2026-05-01")
            }

            assertEquals(listOf(WateringLogEntity("2026-05-01", 3)), dao.wateringBetween("2026-05-01", "2026-05-02"))
        }

    @Test
    fun `期間の記録は開始日を含み終了日を含まない`() =
        runTest {
            listOf("2026-04-30", "2026-05-01", "2026-05-03", "2026-05-04").forEach {
                dao.insertWateringIfAbsent(WateringLogEntity(it, 1))
            }

            assertEquals(
                listOf("2026-05-01", "2026-05-03"),
                dao.wateringBetween("2026-05-01", "2026-05-04").map { it.date },
            )
        }
}
