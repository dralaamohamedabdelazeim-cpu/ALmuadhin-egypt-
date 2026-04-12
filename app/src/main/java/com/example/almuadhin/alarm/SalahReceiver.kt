Enterpackage com.example.almuadhin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.almuadhin.R

class SalahReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val mp = MediaPlayer.create(context, R.raw.nozaker_salt_ala_habib)
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
