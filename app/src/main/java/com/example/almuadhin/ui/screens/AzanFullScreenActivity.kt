package com.example.almuadhin.ui.screens

import android.content.Context
import android.os.Bundle
import android.telephony.TelephonyManager
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.example.almuadhin.alarm.AzanMediaPlayer
import com.example.almuadhin.databinding.ActivityAzanFullscreenBinding

class AzanFullScreenActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAzanFullscreenBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAzanFullscreenBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // تظهر في كل حتة
        window.addFlags(
            WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD or
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
            WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )

        val prayerName = intent.getStringExtra("prayer_name") ?: ""
        binding.tvPrayerName.text = prayerName

        // مش يشتغل أثناء المكالمات
        val telephony = getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        if (telephony.callState != TelephonyManager.CALL_STATE_IDLE) {
            finish()
            return
        }

        binding.btnDismiss.setOnClickListener {
            AzanMediaPlayer.player?.stop()
            AzanMediaPlayer.player?.release()
            AzanMediaPlayer.player = null
            finish()
        }
    }
}
