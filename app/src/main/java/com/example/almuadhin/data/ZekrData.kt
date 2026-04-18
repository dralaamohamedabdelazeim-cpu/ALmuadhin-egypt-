package com.example.almuadhin.data

import com.example.almuadhin.R

data class Zekr(val name: String, val resId: Int)

object ZekrData {
    val zekrList = listOf(
        Zekr("سبحان الله وبحمده",        R.raw.sobhanallah_wabehamdeh),
        Zekr("الحمد لله",                R.raw.alhamdo_lelah),
        Zekr("اللهم لك الحمد",           R.raw.allahom_lk_alhamd),
        Zekr("لا حول ولا قوة إلا بالله", R.raw.lahawla_wlaqowat),
        Zekr("ربنا اغفر لي",             R.raw.rbna_ighfer_li)
    )
}
