package xyz.gekkao.sieversiacage.core.data

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

private const val CHANNEL_ID = "watering"
private const val NOTIFICATION_ID = 1
private const val PREFS = "reminder"
private const val KEY_PERMISSION_ASKED = "permission_asked"

/** 水やりの通知（ADR-0021）。通知の許可と、許可を尋ねたかどうかも扱う。 */
@Singleton
class ReminderNotifier
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val growth: GrowthRepository,
    ) {
        private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        /**
         * ショーケースに「土が乾いたら知らせますか？」を出すか。
         * Android 13 以降で、まだ許可されておらず、まだ尋ねておらず、一度でも水をやったことがあるとき。
         */
        suspend fun shouldAskPermission(): Boolean =
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                !hasPermission() &&
                !prefs.getBoolean(KEY_PERMISSION_ASKED, false) &&
                growth.hasEverWatered()

        /** 許可を尋ねた（許可・拒否・「今はいい」のどれでも）。もう案内は出さない。 */
        fun markPermissionAsked() = prefs.edit { putBoolean(KEY_PERMISSION_ASKED, true) }

        /** 条件に合えば、水やりを知らせる。 */
        suspend fun remindIfDry() {
            if (!shouldRemind(growth.stateOn(), growth.wateredToday())) return
            post()
        }

        private fun post() {
            // 許可の確認は notify と同じ関数の中で行う（lint の MissingPermission が追えるように）
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            ) {
                return
            }
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) return
            createChannel()
            // 開く画面が見つからないとき（ランチャーのない APK など）は、押しても何もしない通知にする
            val contentIntent =
                context.packageManager.getLaunchIntentForPackage(context.packageName)?.let { launch ->
                    PendingIntent.getActivity(
                        context,
                        0,
                        launch,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
                    )
                }
            val notification =
                NotificationCompat
                    .Builder(context, CHANNEL_ID)
                    .setSmallIcon(R.drawable.ic_reminder_water)
                    .setContentTitle(context.getString(R.string.reminder_title))
                    .setContentText(context.getString(R.string.reminder_text))
                    .setContentIntent(contentIntent)
                    .setAutoCancel(true)
                    .build()
            NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
        }

        private fun hasPermission(): Boolean =
            Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED

        private fun createChannel() {
            val channel =
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.reminder_channel_name),
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply { description = context.getString(R.string.reminder_channel_description) }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }
