package xyz.gekkao.sieversiacage.feature.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.widget.RemoteViews
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import xyz.gekkao.sieversiacage.core.data.PlantDocumentRepository
import xyz.gekkao.sieversiacage.core.data.WidgetRefresher
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "Widget"
private const val SPIKE_DAY = 10

/**
 * ホーム画面のウィジェットにドキュメントを載せる（スパイク②・M2-1・M2-2）。
 * 公開 SDK の API（RemoteViews.DrawInstructions、API 36〜）だけを使う。
 * 毎日の成長 tick や水やりの記録のあとにも呼ばれる（ADR-0021 の [WidgetRefresher]）。
 */
@Singleton
class PlantWidgetUpdater
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val documents: PlantDocumentRepository,
    ) : WidgetRefresher {
        override suspend fun refresh() {
            val manager = AppWidgetManager.getInstance(context)
            val ids = manager.getAppWidgetIds(ComponentName(context, PlantWidgetProvider::class.java))
            if (ids.isNotEmpty()) update(manager, ids)
        }

        fun update(
            manager: AppWidgetManager,
            ids: IntArray,
        ) {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) return // initialLayout の代わりの表示のまま
            val bytes = documents.widgetDocument(SPIKE_DAY)
            Timber
                .tag(
                    TAG,
                ).i("widget ${bytes.size}B, player level=${RemoteViews.DrawInstructions.getSupportedVersion()}")
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
            manager.updateAppWidget(ids, views)
        }
    }
