package com.example.almuadhin.data

import com.example.almuadhin.R

enum class LocationMode { AUTO, MANUAL }

enum class CalculationMethod(val apiId: Int, val label: String, val labelAr: String) {
    UMM_AL_QURA(4, "Umm Al-Qura", "أم القرى"),
    MWL(3, "MWL", "رابطة العالم الإسلامي"),
    ISNA(2, "ISNA", "أمريكا الشمالية"),
    EGYPT(5, "Egypt", "الهيئة المصرية")
}

enum class AdhanSound(val resId: Int, val labelAr: String, val isFull: Boolean = false) {
    MAKKAH(R.raw.adhan_makkah, "أذان مكة المكرمة"),
    MADINAH(R.raw.adhan_madinah, "أذان المدينة المنورة"),
    ALAQSA(R.raw.adhan_alaqsa, "أذان المسجد الأقصى"),
    MAKKAH_FULL(R.raw.adhan_makkah_full, "أذان مكة الكامل", isFull = true),
    ELHOSARY(R.raw.adhan_elhosary, "الشيخ الحصري"),
    ELNAKSHBANDY(R.raw.adhan_elnakshbandy, "الشيخ النقشبندي"),
    MOHAMED_REFAT(R.raw.adhan_mohamed_refat, "الشيخ محمد رفعت")
}

data class UserSettings(
    val locationMode: LocationMode = LocationMode.AUTO,
    val manualCity: String = "المنامة",
    val manualCountry: String = "البحرين",
    val calculationMethod: CalculationMethod = CalculationMethod.UMM_AL_QURA,
    val notificationsEnabled: Boolean = true,
    val adsRemoved: Boolean = false,
    val adCooldownMinutes: Int = 0,
    val adhanSound: AdhanSound = AdhanSound.MAKKAH,
    val playFullAdhan: Boolean = false
)

data class PrayerDay(
    val imsak: String,
    val fajr: String,
    val sunrise: String,
    val dhuhr: String,
    val asr: String,
    val maghrib: String,
    val isha: String,
    val timezone: String,
    val gregorianDate: String,
    val hijriDate: String,
    val hijriMonthNumber: Int
)
