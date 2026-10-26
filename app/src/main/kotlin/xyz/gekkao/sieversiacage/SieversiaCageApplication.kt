package xyz.gekkao.sieversiacage

import android.app.Application
import android.util.Log
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.gekkao.sieversiacage.core.data.AlbumRepository
import javax.inject.Inject

private const val TAG = "SieversiaCage"

/** Hilt の起点（ADR-0015）。起動時に、版の古いアルバムの日を作り直す（ADR-0011）。 */
@HiltAndroidApp
class SieversiaCageApplication : Application() {
    @Inject
    lateinit var album: AlbumRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()
        scope.launch {
            val regenerated = album.regenerateStale()
            Log.i(TAG, "album: regenerated $regenerated day(s)")
        }
    }
}
