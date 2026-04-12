package com.example.almuadhin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.almuadhin.R
import com.example.almuadhin.data.SettingsRepository
import com.example.almuadhin.data.SalahSound
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class SalahReceiver : BroadcastReceiver() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        val settings = runBlocking { settingsRepository.settingsFlow.first() }
        val soundResId = settings.salahSound.resId

        val mp = MediaPlayer.create(context, soundResId)
        mp?.start()
        mp?.setOnCompletionListener { it.release() }

        val notif = NotificationCompat.Builder(context, NotificationHelper.CHANNEL_AZKAR)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("الصلاة على النبي ﷺ")
            .setContentText("اللهم صل وسلم على نبينا محمد")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(9001, notif)
    }
}
