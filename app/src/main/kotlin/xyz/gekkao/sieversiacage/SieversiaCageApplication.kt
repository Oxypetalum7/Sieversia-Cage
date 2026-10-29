package xyz.gekkao.sieversiacage

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import xyz.gekkao.sieversiacage.core.data.GrowthTicker
import xyz.gekkao.sieversiacage.core.data.work.GrowthWorkScheduler
import javax.inject.Inject

private const val TAG = "SieversiaCage"

/**
 * Hilt の起点（ADR-0015）。起動時に次をする。
 *
 * - 版の古いアルバムの日を作り直す（ADR-0011）
 * - 抜けている日を進め、毎日の成長 tick の予約がなければ入れる（ADR-0021）
 */
@HiltAndroidApp
class SieversiaCageApplication :
    Application(),
    Configuration.Provider {
    @Inject
    lateinit var album: AlbumRepository

    @Inject
    lateinit var ticker: GrowthTicker

    @Inject
    lateinit var scheduler: GrowthWorkScheduler

    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // ADR-0021: Worker に Hilt で依存を渡す
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(workerFactory).build()

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) Timber.plant(Timber.DebugTree())
        scheduler.ensureScheduled()
        scope.launch {
            val regenerated = album.regenerateStale()
            Timber.tag(TAG).i("album: regenerated $regenerated day(s)")
            ticker.catchUp()
        }
    }
}
