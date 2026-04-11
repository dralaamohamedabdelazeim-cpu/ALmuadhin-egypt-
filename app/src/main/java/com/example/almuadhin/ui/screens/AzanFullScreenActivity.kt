package com.example.almuadhin.ui.screens

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.almuadhin.R
import com.example.almuadhin.alarm.AzanMediaPlayer

class AzanFullScreenActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_azan_fullscreen)

        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephony.callState != TelephonyManager.CALL_STATE_IDLE) {
            finish()
            return
        }

        val prayerName = intent.getStringExtra("prayer_name") ?: ""
        findViewById<TextView>(R.id.tvPrayerName).text = prayerName

        findViewById<Button>(R.id.btnDismiss).setOnClickListener {
            AzanMediaPlayer.player?.stop()
            AzanMediaPlayer.player?.release()
            AzanMediaPlayer.player = null
            finish()
        }
    }
}
