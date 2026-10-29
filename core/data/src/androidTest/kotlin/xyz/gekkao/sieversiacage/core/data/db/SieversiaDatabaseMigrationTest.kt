package xyz.gekkao.sieversiacage.core.data.db

import androidx.room3.Room
import androidx.room3.testing.MigrationTestHelper
import androidx.room3.useReaderConnection
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.sqlite.execSQL
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

private const val TEST_DB = "migration-test"

/**
 * ADR-0018: スキーマのマイグレーションは、本番と同じ同梱の SQLite で実機で確かめる。
 * minSdk 29 では DEX のメソッド名に空白を使えないので、テスト名は空白なしで書く。
 */
@RunWith(AndroidJUnit4::class)
class SieversiaDatabaseMigrationTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @get:Rule
    val helper =
        MigrationTestHelper(
            instrumentation = instrumentation,
            databaseClass = SieversiaDatabase::class,
            driver = BundledSQLiteDriver(),
            file = instrumentation.targetContext.getDatabasePath(TEST_DB),
        )

    // 前のテストの DB が残っていると、createDatabase が「新しく作るはずがマイグレーションになった」と落ちる
    @Before
    fun deleteDatabase() {
        instrumentation.targetContext.deleteDatabase(TEST_DB)
    }

    @Test
    fun `版1の行は版2でSimの出力の列が既定値で埋まる`() =
        runTest {
            helper.createDatabase(1).use { connection ->
                connection.execSQL(
                    "INSERT INTO album_entry " +
                        "(age_days, stage, remote_compose_version, compiler_version, target, created_at) " +
                        "VALUES (3, 'LEAF', '1.0.0-alpha20', 1, 'IN_APP', 0)",
                )
            }

            helper.runMigrationsAndValidate(2).use { connection ->
                connection
                    .prepare(
                        "SELECT stage, cycle, stage_progress, growth, branch_count, flower_count, " +
                            "soil_moisture, vigor FROM album_entry WHERE age_days = 3",
                    ).use {
                        it.step()
                        assertEquals("LEAF", it.getText(0))
                        assertEquals(0L, it.getLong(1))
                        assertEquals(0.0, it.getDouble(2), 0.0)
                        assertEquals(0.0, it.getDouble(3), 0.0)
                        assertEquals(1L, it.getLong(4))
                        assertEquals(1L, it.getLong(5))
                        assertEquals(1.0, it.getDouble(6), 0.0)
                        assertEquals(1.0, it.getDouble(7), 0.0)
                    }
            }
        }

    @Test
    fun `版2の行は版3でも残り水やりの記録と植えた日の表が増える`() =
        runTest {
            helper.createDatabase(2).use { connection ->
                connection.execSQL(
                    "INSERT INTO album_entry " +
                        "(age_days, stage, cycle, stage_progress, growth, branch_count, flower_count, " +
                        "soil_moisture, vigor, remote_compose_version, compiler_version, target, created_at) " +
                        "VALUES (5, 'LEAF', 0, 0.25, 5.0, 1, 1, 1.0, 1.0, '1.0.0-alpha20', 3, 'IN_APP', 0)",
                )
            }

            helper.runMigrationsAndValidate(3).use { connection ->
                connection.prepare("SELECT growth FROM album_entry WHERE age_days = 5").use {
                    it.step()
                    assertEquals(5.0, it.getDouble(0), 0.0)
                }
                connection.execSQL("INSERT INTO watering_log (date, taps) VALUES ('2026-05-01', 1)")
                connection.execSQL("INSERT INTO plant (id, planted_on) VALUES (1, '2026-05-01')")
            }
        }

    @Test
    fun `版1から最新の版まですべてのマイグレーションを通して開ける`() =
        runTest {
            helper.createDatabase(1).close()

            val db =
                Room
                    .databaseBuilder<SieversiaDatabase>(instrumentation.targetContext, TEST_DB)
                    .setDriver(BundledSQLiteDriver())
                    .build()
            // Room は開くときに、マイグレーション後のスキーマを検証する
            db.useReaderConnection { }
            assertEquals(emptyList<AlbumEntryEntity>(), db.albumEntryDao().all())
            db.close()
        }
}
