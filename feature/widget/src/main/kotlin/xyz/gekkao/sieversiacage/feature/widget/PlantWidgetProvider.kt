package xyz.gekkao.sieversiacage.feature.widget

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/** ホーム画面のウィジェット。置いたときやフォントサイズを変えたときなどに、システムから更新を頼まれる。 */
@AndroidEntryPoint
class PlantWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var updater: PlantWidgetUpdater

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        updater.update(appWidgetManager, appWidgetIds)
    }
}
