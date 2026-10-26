package xyz.gekkao.sieversiacage.core.data.db

import android.content.Context
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * ADR-0018: DAO の SQL は、本番と同じ同梱の SQLite で実機で確かめる。
 * minSdk 29 では DEX のメソッド名に空白を使えないので、テスト名は空白なしで書く。
 */
@RunWith(AndroidJUnit4::class)
class AlbumEntryDaoTest {
    private lateinit var db: SieversiaDatabase
    private lateinit var dao: AlbumEntryDao

    @Before
    fun createDb() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db =
            Room
                .inMemoryDatabaseBuilder<SieversiaDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
        dao = db.albumEntryDao()
    }

    @After
    fun closeDb() {
        db.close()
    }

    @Test
    fun `upsertは同じ日を上書きする`() =
        runTest {
            dao.upsert(entry(ageDays = 3, compilerVersion = 1))
            dao.upsert(entry(ageDays = 3, compilerVersion = 2))

            assertEquals(entry(ageDays = 3, compilerVersion = 2), dao.find(3))
            assertEquals(1, dao.all().size)
        }

    @Test
    fun `findはない日にnullを返す`() =
        runTest {
            assertNull(dao.find(5))
        }

    @Test
    fun `allは経過日数の順に返す`() =
        runTest {
            listOf(7, 0, 3).forEach { dao.upsert(entry(ageDays = it)) }

            assertEquals(listOf(0, 3, 7), dao.all().map { it.ageDays })
        }

    @Test
    fun `staleはCompilerの版が古い日とRemoteComposeの版が違う日を返す`() =
        runTest {
            dao.upsert(entry(ageDays = 0, compilerVersion = 1))
            dao.upsert(entry(ageDays = 1, compilerVersion = 2))
            dao.upsert(entry(ageDays = 2, compilerVersion = 2, remoteComposeVersion = "1.0.0-alpha19"))
            dao.upsert(entry(ageDays = 3, compilerVersion = 3))

            val stale = dao.stale(compilerVersion = 2, remoteComposeVersion = "1.0.0-alpha20")

            assertEquals(listOf(0, 2), stale.map { it.ageDays })
        }

    private fun entry(
        ageDays: Int,
        compilerVersion: Int = 1,
        remoteComposeVersion: String = "1.0.0-alpha20",
    ) = AlbumEntryEntity(
        ageDays = ageDays,
        stage = "SPROUT",
        remoteComposeVersion = remoteComposeVersion,
        compilerVersion = compilerVersion,
        target = "IN_APP",
        createdAt = 0L,
    )
}
