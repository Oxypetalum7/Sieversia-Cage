package xyz.gekkao.sieversiacage.feature.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import xyz.gekkao.sieversiacage.core.data.GrowthTicker
import javax.inject.Inject

// onUpdate が返ったあとも続けるためのスコープ。goAsync の間だけプロセスが生かされる
private val providerScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

/** ホーム画面のウィジェット。置いたときやフォントサイズを変えたときなどに、システムから更新を頼まれる。 */
@AndroidEntryPoint
class PlantWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var updater: PlantWidgetUpdater

    @Inject lateinit var ticker: GrowthTicker

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        val pending = goAsync()
        providerScope.launch {
            try {
                // ADR-0021: tick が遅れていても、ここで抜けている日を進めてから今日の株を描く
                ticker.catchUp()
                updater.update(appWidgetManager, appWidgetIds)
            } finally {
                pending.finish()
            }
        }
    }
}
