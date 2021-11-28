package com.app.caloriestomealspedometer

import android.app.*
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.media.RingtoneManager
import android.os.Process
import android.preference.PreferenceManager
import android.util.Log
import java.text.SimpleDateFormat
import java.util.*

class AlarmNotification : BroadcastReceiver() {
    // データを受信した
    override fun onReceive(context: Context, intent: Intent) {
        Log.d("AlarmBroadcastReceiver", "onReceive() pid=" + Process.myPid())
        val requestCode = intent.getIntExtra("RequestCode", 0)
        val resultIntent = Intent(context, MainActivity::class.java)
        resultIntent.flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        val pendingIntent: PendingIntent? = TaskStackBuilder.create(context).run {
            // Add the intent, which inflates the back stack
            addNextIntentWithParentStack(resultIntent)
            // Get the PendingIntent containing the entire back stack
            getPendingIntent(requestCode, PendingIntent.FLAG_UPDATE_CURRENT)
        }
        val channelId = "default"
        // app name
        val title = context.getString(R.string.app_name)
        val pref = PreferenceManager.getDefaultSharedPreferences(context)
        val foodName = pref.getString("CHECK_NAME","")
        val foodUnit = pref.getString("CHECK_UNIT","")

        //メッセージのセット
        val message = if (foodName == ""){
            "今日も一日お疲れ様でした。アプリを開いて今日の消費カロリーを確認しましょう！"
        } else {
            "今日も一日お疲れ様でした。アプリを開いて今日の消費カロリーを確認しましょう！" +
                    "\n本日の消費カロリー：\n${foodName} ?${foodUnit}分"
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val defaultSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        // Notification　Channel 設定
        val channel = NotificationChannel(channelId, title, NotificationManager.IMPORTANCE_DEFAULT)
        channel.description = message
        channel.enableVibration(true)
        channel.canShowBadge()
        channel.enableLights(true)
        channel.lightColor = Color.BLUE
        // the channel appears on the lockscreen
        channel.lockscreenVisibility = Notification.VISIBILITY_PRIVATE
        channel.setSound(defaultSoundUri, null)
        channel.setShowBadge(true)
        notificationManager.createNotificationChannel(channel)
        val notification : Notification = Notification.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_name)
            .setColor(Color.parseColor("#ED7D31"))
            .setContentText(message)
            .setAutoCancel(true)
            .setStyle(Notification.BigTextStyle().bigText(message))
            .setContentIntent(pendingIntent)
            .setWhen(System.currentTimeMillis())
            .build()

        // 通知
        notificationManager.notify(R.string.app_name, notification)
    }
}
