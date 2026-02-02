package com.example.almuadhin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.almuadhin.R
import com.example.almuadhin.data.AdhanSound

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val title = intent.getStringExtra(EXTRA_TITLE) ?: context.getString(R.string.notif_prayer_title)
        val body = intent.getStringExtra(EXTRA_BODY) ?: context.getString(R.string.notif_prayer_body)
        val adhanSoundName = intent.getStringExtra(EXTRA_ADHAN_SOUND) ?: AdhanSound.MAKKAH.name
        
        val adhanSound = try {
            AdhanSound.valueOf(adhanSoundName)
        } catch (e: Exception) {
            AdhanSound.MAKKAH
        }

        NotificationHelper.ensureChannels(context, adhanSound)

        val openIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)
        val pi = android.app.PendingIntent.getActivity(
            context,
            0,
            openIntent,
            android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
        )
        
        val soundUri = NotificationHelper.getAdhanSoundUri(context, adhanSound)

        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_PRAYER)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setContentIntent(pi)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setSound(soundUri)
            .build()

        NotificationManagerCompat.from(context).notify(intent.getIntExtra(EXTRA_ID, 1001), notif)
    }

    companion object {
        const val EXTRA_ID = "extra_id"
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_BODY = "extra_body"
        const val EXTRA_ADHAN_SOUND = "extra_adhan_sound"
    }
}
