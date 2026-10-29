package xyz.gekkao.sieversiacage.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
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
        Timber.tag(TAG).i("widget ${bytes.size}B, player level=${RemoteViews.DrawInstructions.getSupportedVersion()}")
        val views = RemoteViews(RemoteViews.DrawInstructions.Builder(listOf(bytes)).build())
        // ADR-0014: ドキュメントの水やりのホストアクションを、アプリの WateringReceiver に届ける。
        // 内蔵プレイヤーはメタデータを extra に足して送るので、PendingIntent は書き換え可能にする
        val watering =
            PendingIntent.getBroadcast(
                context,
                0,
                Intent(context, WateringReceiver::class.java),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE,
            )
        views.setOnClickResponse(documents.waterActionId, RemoteViews.RemoteResponse.fromPendingIntent(watering))
        appWidgetManager.updateAppWidget(appWidgetIds, views)
    }
}
