package com.example.almuadhin.alarm

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.IBinder
import android.telephony.TelephonyManager
import com.example.almuadhin.R
import com.example.almuadhin.data.ZekrData
import com.example.almuadhin.data.ZekrPrefs

class ZekrService : Service() {

    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private const val CHANNEL_ID = "zekr_channel"
        private const val NOTIF_ID = 4001
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ctx = applicationContext
        val tm = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val am = getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (tm.callState != TelephonyManager.CALL_STATE_IDLE ||
            am.mode == AudioManager.MODE_IN_COMMUNICATION) {
            ZekrScheduler.schedule(ctx, 5)
            stopSelf()
            return START_NOT_STICKY
        }

        if (AzanMediaPlayer.player?.isPlaying == true) {
            ZekrScheduler.schedule(ctx, 5)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIF_ID, buildNotification())
        playZekr()
        return START_NOT_STICKY
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "الأذكار الصوتية",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "تشغيل الأذكار الصوتية"
                setSound(null, null)
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(): Notification {
        val builder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
        }
        return builder
            .setContentTitle("الأذكار")
            .setContentText("جارٍ تشغيل الذكر...")
            .setSmallIcon(R.drawable.icon)
            .build()
    }

    private fun playZekr() {
        val ctx = applicationContext
        val playbackMode = ZekrPrefs.getPlaybackMode(ctx)
        val volumeFraction = ZekrPrefs.getVolume(ctx)

        val resId = if (playbackMode == 1) {
            val repeatIndex = ZekrPrefs.getRepeatIndex(ctx)
            if (repeatIndex < ZekrData.zekrList.size)
                ZekrData.zekrList[repeatIndex].resId
            else
                ZekrData.zekrList[0].resId
        } else {
            val index = ZekrPrefs.nextZekrIndex(ctx)
            ZekrData.zekrList[index].resId
        }

        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(ctx, resId)?.apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build()
                )
                setVolume(volumeFraction, volumeFraction)
                setOnCompletionListener {
                    it.release()
                    mediaPlayer = null
                    val interval = ZekrPrefs.getIntervalInMinutes(ctx).toLong()
                    if (ZekrPrefs.isEnabled(ctx)) {
                        ZekrScheduler.schedule(ctx, interval)
                    }
                    stopSelf()
                }
                start()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            stopSelf()
        }
    }

    override fun onDestroy() {
        mediaPlayer?.release()
        mediaPlayer = null
        super.onDestroy()
    }
}
