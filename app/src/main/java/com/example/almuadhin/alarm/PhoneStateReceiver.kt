package com.example.almuadhin.alarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.telephony.TelephonyManager
import com.example.almuadhin.data.ZekrPrefs

class PhoneStateReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
        // لما المكالمة تخلص
        if (state == TelephonyManager.EXTRA_STATE_IDLE) {
            if (ZekrPrefs.isEnabled(context)) {
                val interval = ZekrPrefs.getIntervalInMinutes(context).toLong()
                ZekrScheduler.schedule(context, interval)
            }
        }
    }
}
