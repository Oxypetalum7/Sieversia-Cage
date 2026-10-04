package xyz.gekkao.sieversiacage

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.os.Build
import android.util.Log
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import javax.inject.Inject

private const val TAG = "Spike2"
private const val SPIKE_DAY = 10

/**
 * スパイク②: Day 10 のドキュメントを、OS 内蔵プレイヤーで再生するウィジェットとして載せる。
 * 公開 SDK の API（RemoteViews.DrawInstructions、API 36〜）だけを使う。
 */
@AndroidEntryPoint
class PlantWidgetProvider : AppWidgetProvider() {
    @Inject lateinit var documents: PlantDocumentRepository

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return // initialLayout の代わりの表示のまま
        val bytes = documents.widgetDocument(SPIKE_DAY)
        Log.i(TAG, "widget ${bytes.size}B, player level=${RemoteViews.DrawInstructions.getSupportedVersion()}")
        val views = RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(bytes)).build())
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}
