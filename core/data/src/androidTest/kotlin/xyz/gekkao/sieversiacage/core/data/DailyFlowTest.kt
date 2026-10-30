package xyz.gekkao.sieversiacage.core.data

import android.Manifest
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.room3.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import androidx.work.Configuration
import androidx.work.ListenableWorker
import androidx.work.WorkManager
import androidx.work.WorkerFactory
import androidx.work.WorkerParameters
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.TestListenableWorkerBuilder
import androidx.work.testing.WorkManagerTestInitHelper
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import xyz.gekkao.sieversiacage.core.data.db.SieversiaDatabase
import xyz.gekkao.sieversiacage.core.data.work.DAILY_TICK_WORK
import xyz.gekkao.sieversiacage.core.data.work.DailyGrowthWorker
import xyz.gekkao.sieversiacage.core.data.work.DrySoilReminderWorker
import xyz.gekkao.sieversiacage.core.data.work.GrowthWorkScheduler
import xyz.gekkao.sieversiacage.core.data.work.REMINDER_WORK
import xyz.gekkao.sieversiacage.core.sim.simulateWateredDaily
import java.io.File
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

private val ZONE: ZoneId = ZoneId.of("Asia/Tokyo")
private val PLANTED_ON: LocalDate = LocalDate.of(2026, 10, 29)
private const val TOLERANCE = 1e-4f

/**
 * M2-6（ADR-0019）: 毎日の流れを、端から端まで通して確かめる。
 * tick → Sim → アルバム → ウィジェット、水やり → Sim、夕方の通知を、偽の時計で日付を進めながら本物の部品で動かす。
 *
 * - DB は本番と同じ同梱の SQLite（メモリの中）。Compiler も本物で、アルバムとウィジェットのドキュメントを実際に作る
 * - Worker は WorkManager を通さずに直接動かし、翌日の分の予約（遅らせる時間）を WorkManager のテスト用の実装で確かめる
 * - 通知は本当に出し、出ているかを NotificationManager で確かめる
 *
 * minSdk 29 では DEX のメソッド名に空白を使えないので、テスト名は空白なしで書く。
 */
@RunWith(AndroidJUnit4::class)
class DailyFlowTest {
    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val clock = MutableClock(PLANTED_ON.atTime(12, 0).atZone(ZONE))
    private val notifications = context.getSystemService(NotificationManager::class.java)

    private lateinit var db: SieversiaDatabase
    private lateinit var workManager: WorkManager
    private lateinit var growth: GrowthRepository
    private lateinit var album: AlbumRepository
    private lateinit var widgets: RecordingWidgetRefresher
    private lateinit var ticker: GrowthTicker
    private lateinit var notifier: ReminderNotifier
    private lateinit var scheduler: GrowthWorkScheduler

    private val workerFactory =
        object : WorkerFactory() {
            override fun createWorker(
                appContext: Context,
                workerClassName: String,
                workerParameters: WorkerParameters,
            ): ListenableWorker? =
                when (workerClassName) {
                    DailyGrowthWorker::class.java.name -> {
                        DailyGrowthWorker(appContext, workerParameters, ticker, scheduler)
                    }

                    DrySoilReminderWorker::class.java.name -> {
                        DrySoilReminderWorker(appContext, workerParameters, notifier, scheduler)
                    }

                    else -> {
                        null
                    }
                }
        }

    @Before
    fun setUp() {
        WorkManagerTestInitHelper.initializeTestWorkManager(
            context,
            Configuration.Builder().setExecutor(SynchronousExecutor()).build(),
        )
        workManager = WorkManager.getInstance(context)
        db =
            Room
                .inMemoryDatabaseBuilder<SieversiaDatabase>(context)
                .setDriver(BundledSQLiteDriver())
                .build()
        val albumDir = File(context.cacheDir, "daily-flow-album").apply { deleteRecursively() }
        growth = GrowthRepository(db.growthDao(), clock)
        album = AlbumRepository(db.albumEntryDao(), AlbumDocumentFiles(albumDir), InAppAlbumDocumentGenerator(), clock)
        widgets = RecordingWidgetRefresher(PlantDocumentRepository(context, growth))
        ticker = GrowthTicker(growth, album, widgets)
        notifier = ReminderNotifier(context, growth)
        scheduler = GrowthWorkScheduler(context, clock)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            InstrumentationRegistry
                .getInstrumentation()
                .uiAutomation
                .grantRuntimePermission(context.packageName, Manifest.permission.POST_NOTIFICATIONS)
        }
        notifications.cancelAll()
    }

    @After
    fun tearDown() {
        notifications.cancelAll()
        db.close()
    }

    @Test
    fun `水やりと乾きと夕方の通知が日をまたいで流れる`() =
        runTest {
            // Day 0: 植えて、水をやる。水をやった日は鳴らない
            runTick(day = 0, hour = 12, minute = 0)
            assertEquals(listOf(0), albumDays())
            assertEquals(1, widgets.documents.size)
            assertEquals(Duration.ofHours(12).plusMinutes(5), pendingDelay(DAILY_TICK_WORK))
            growth.recordWatering()
            assertTrue(growth.wateredToday())
            runReminder(day = 0)
            assertFalse(reminderShown())
            assertEquals(Duration.ofDays(1), pendingDelay(REMINDER_WORK))

            // Day 1: 0 時過ぎに日が進む。昨日の水やりで土は湿っている。今日は水をやらない
            runTick(day = 1)
            assertEquals(listOf(0, 1), albumDays())
            assertEquals(2, widgets.documents.size)
            assertEquals(Duration.ofDays(1), pendingDelay(DAILY_TICK_WORK))
            assertMoisture(day = 1, expected = 1f)
            assertFalse(growth.wateredToday())
            runReminder(day = 1)
            assertFalse(reminderShown())

            // Day 2: 1 日乾く。まだ鳴らない
            runTick(day = 2)
            assertMoisture(day = 2, expected = 0.65f)
            runReminder(day = 2)
            assertFalse(reminderShown())

            // Day 3: 2 日続けて水をやらなかったので、夕方に鳴る。鳴ったあと水をやる
            runTick(day = 3)
            assertMoisture(day = 3, expected = 0.3f)
            runReminder(day = 3)
            assertTrue(reminderShown())
            notifications.cancelAll()
            clock.set(day = 3, hour = 19, minute = 0)
            growth.recordWatering()

            // Day 4: 土は湿り、元気さが戻り始める。乾いた日の分だけ、毎日水をやった株より育ちが遅い
            runTick(day = 4)
            assertMoisture(day = 4, expected = 1f)
            val day3 = entry(3)
            val day4 = entry(4)
            assertTrue("vigor ${day4.vigor} > ${day3.vigor}", day4.vigor > day3.vigor)
            val wateredDaily = simulateWateredDaily(PLANTED_ON, 4)
            assertTrue("growth ${day4.growth} < ${wateredDaily.growth}", day4.growth < wateredDaily.growth)
            runReminder(day = 4)
            assertFalse(reminderShown())

            // ウィジェットには毎回、作り直したドキュメントが届いている
            assertEquals(5, widgets.documents.size)
            assertTrue(widgets.documents.all { it.isNotEmpty() })
        }

    @Test
    fun `tickが抜けた日はあとでまとめて埋まる`() =
        runTest {
            runTick(day = 0, hour = 12, minute = 0)
            growth.recordWatering()

            // 端末の電源が切れていたなどで、Day 1〜3 の tick が動かず、Day 4 の朝にアプリを開いた
            clock.set(day = 4, hour = 8, minute = 0)
            assertEquals(4, ticker.catchUp())

            assertEquals(listOf(0, 1, 2, 3, 4), albumDays())
            for (day in 1..4) {
                val expected = growth.stateOn(PLANTED_ON.plusDays(day.toLong()))
                val actual = entry(day)
                assertEquals(expected.stage.name, actual.stage)
                assertEquals(expected.growth, actual.growth, TOLERANCE)
                assertEquals(expected.soilMoisture, actual.soilMoisture, TOLERANCE)
            }
            // まとめて埋めても、ウィジェットの作り直しは 1 回
            assertEquals(2, widgets.documents.size)
        }

    @Test
    fun `初めて植えたときプレビューの記録が消える`() =
        runTest {
            album.record(simulateWateredDaily(PLANTED_ON, 10))

            runTick(day = 0, hour = 12, minute = 0)

            assertEquals(listOf(0), albumDays())
        }

    private suspend fun runTick(
        day: Int,
        hour: Int = 0,
        minute: Int = 5,
    ) {
        clock.set(day, hour, minute)
        runWorker<DailyGrowthWorker>(DAILY_TICK_WORK)
    }

    private suspend fun runReminder(day: Int) {
        clock.set(day, hour = 18, minute = 0)
        runWorker<DrySoilReminderWorker>(REMINDER_WORK)
    }

    private suspend inline fun <reified W : ListenableWorker> runWorker(name: String) {
        // 本番では、動いている自分のあとに翌日の分がつながる。ここでは WorkManager を通さないので、前の予約を消しておく
        workManager.cancelUniqueWork(name).result.get()
        val worker = TestListenableWorkerBuilder<W>(context).setWorkerFactory(workerFactory).build()
        assertEquals(ListenableWorker.Result.success(), worker.startWork().get())
    }

    /** 予約されている次の仕事を、今からどれだけ遅らせて動かすか。 */
    private fun pendingDelay(name: String): Duration {
        val pending = workManager.getWorkInfosForUniqueWork(name).get().filterNot { it.state.isFinished }
        assertEquals(1, pending.size)
        return Duration.ofMillis(pending.single().initialDelayMillis)
    }

    private suspend fun albumDays(): List<Int> = db.albumEntryDao().all().map { it.ageDays }

    private suspend fun entry(day: Int) = checkNotNull(db.albumEntryDao().find(day))

    private suspend fun assertMoisture(
        day: Int,
        expected: Float,
    ) = assertEquals(expected, entry(day).soilMoisture, TOLERANCE)

    private fun reminderShown(): Boolean = notifications.activeNotifications.isNotEmpty()
}

/** 日付と時刻を自由に進められる時計。 */
private class MutableClock(
    private var now: ZonedDateTime,
) : Clock() {
    fun set(
        day: Int,
        hour: Int,
        minute: Int,
    ) {
        now = PLANTED_ON.plusDays(day.toLong()).atTime(hour, minute).atZone(ZONE)
    }

    override fun getZone(): ZoneId = now.zone

    override fun withZone(zone: ZoneId): Clock = MutableClock(now.withZoneSameInstant(zone))

    override fun instant(): Instant = now.toInstant()
}

/** ウィジェットに送るはずのドキュメントを、本物と同じ方法で作って記録する。 */
private class RecordingWidgetRefresher(
    private val repository: PlantDocumentRepository,
) : WidgetRefresher {
    val documents = mutableListOf<ByteArray>()

    override suspend fun refresh() {
        documents += repository.todayWidgetDocument()
    }
}
