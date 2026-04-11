package com.example.almuadhin.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "muadhin_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val LOCATION_MODE = stringPreferencesKey("location_mode")
        val MANUAL_CITY = stringPreferencesKey("manual_city")
        val MANUAL_COUNTRY = stringPreferencesKey("manual_country")
        val CALC_METHOD = stringPreferencesKey("calc_method")
        val NOTIFICATIONS = booleanPreferencesKey("notifications_enabled")
        val ADS_REMOVED = booleanPreferencesKey("ads_removed")
        val AD_COOLDOWN_MIN = intPreferencesKey("ad_cooldown_min")
        val ADHAN_SOUND = stringPreferencesKey("adhan_sound")
        val PLAY_FULL_ADHAN = booleanPreferencesKey("play_full_adhan")
         val SILENT_FAJR = booleanPreferencesKey("silent_fajr")
        // Stored prayer times for rescheduling after reboot
        val LAST_DATE = stringPreferencesKey("last_prayer_date")
        val IMSAK = stringPreferencesKey("last_imsak")
        val FAJR = stringPreferencesKey("last_fajr")
        val SUNRISE = stringPreferencesKey("last_sunrise")
        val DHUHR = stringPreferencesKey("last_dhuhr")
        val ASR = stringPreferencesKey("last_asr")
        val MAGHRIB = stringPreferencesKey("last_maghrib")
        val ISHA = stringPreferencesKey("last_isha")
        val TIMEZONE = stringPreferencesKey("last_timezone")
    }

    val settingsFlow: Flow<UserSettings> = context.dataStore.data.map { prefs ->
        UserSettings(
            locationMode = prefs[Keys.LOCATION_MODE]
                ?.let { runCatching { LocationMode.valueOf(it) }.getOrNull() }
                ?: LocationMode.AUTO,
            manualCity = prefs[Keys.MANUAL_CITY] ?: "المنامة",
            manualCountry = prefs[Keys.MANUAL_COUNTRY] ?: "البحرين",
            calculationMethod = prefs[Keys.CALC_METHOD]
                ?.let { runCatching { CalculationMethod.valueOf(it) }.getOrNull() }
                ?: CalculationMethod.UMM_AL_QURA,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS] ?: true,
            adsRemoved = prefs[Keys.ADS_REMOVED] ?: false,
            adCooldownMinutes = prefs[Keys.AD_COOLDOWN_MIN] ?: 0,
            adhanSound = prefs[Keys.ADHAN_SOUND]
                ?.let { runCatching { AdhanSound.valueOf(it) }.getOrNull() }
                ?: AdhanSound.MAKKAH,
            playFullAdhan = prefs[Keys.PLAY_FULL_ADHAN] ?: false,
            silentFajr = prefs[Keys.SILENT_FAJR] ?: false,
        )
    }

    suspend fun updateSettings(update: (UserSettings) -> UserSettings) {
        val current = settingsFlow.first()
        val next = update(current)

        context.dataStore.edit { prefs ->
            prefs[Keys.LOCATION_MODE] = next.locationMode.name
            prefs[Keys.MANUAL_CITY] = next.manualCity
            prefs[Keys.MANUAL_COUNTRY] = next.manualCountry
            prefs[Keys.CALC_METHOD] = next.calculationMethod.name
            prefs[Keys.NOTIFICATIONS] = next.notificationsEnabled
            prefs[Keys.ADS_REMOVED] = next.adsRemoved
            prefs[Keys.AD_COOLDOWN_MIN] = next.adCooldownMinutes
            prefs[Keys.ADHAN_SOUND] = next.adhanSound.name
            prefs[Keys.PLAY_FULL_ADHAN] = next.playFullAdhan
            prefs[Keys.SILENT_FAJR] = next.silentFajr
        }
    }

    suspend fun setLocationMode(mode: LocationMode) =
        context.dataStore.edit { it[Keys.LOCATION_MODE] = mode.name }

    suspend fun setManualLocation(city: String, country: String) =
        context.dataStore.edit {
            it[Keys.MANUAL_CITY] = city
            it[Keys.MANUAL_COUNTRY] = country
        }

    suspend fun setCalculationMethod(method: CalculationMethod) =
        context.dataStore.edit { it[Keys.CALC_METHOD] = method.name }

    suspend fun setNotificationsEnabled(enabled: Boolean) =
        context.dataStore.edit { it[Keys.NOTIFICATIONS] = enabled }

    suspend fun setAdsRemoved(removed: Boolean) =
        context.dataStore.edit { it[Keys.ADS_REMOVED] = removed }

    suspend fun setAdCooldownMinutes(minutes: Int) =
        context.dataStore.edit { it[Keys.AD_COOLDOWN_MIN] = minutes.coerceAtLeast(0) }

    suspend fun setAdhanSound(sound: AdhanSound) =
        context.dataStore.edit { it[Keys.ADHAN_SOUND] = sound.name }

    suspend fun setPlayFullAdhan(playFull: Boolean) =
        context.dataStore.edit { it[Keys.PLAY_FULL_ADHAN] = playFull }

    suspend fun setSilentFajr(silent: Boolean) =
    context.dataStore.edit { it[Keys.SILENT_FAJR] = silent }

    suspend fun savePrayerDayForReschedule(dateIso: String, day: PrayerDay) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LAST_DATE] = dateIso
            prefs[Keys.IMSAK] = day.imsak
            prefs[Keys.FAJR] = day.fajr
            prefs[Keys.SUNRISE] = day.sunrise
            prefs[Keys.DHUHR] = day.dhuhr
            prefs[Keys.ASR] = day.asr
            prefs[Keys.MAGHRIB] = day.maghrib
            prefs[Keys.ISHA] = day.isha
            prefs[Keys.TIMEZONE] = day.timezone
        }
    }

    val storedPrayerDayFlow: Flow<Pair<String, PrayerDay>?> = context.dataStore.data.map { prefs ->
        val date = prefs[Keys.LAST_DATE] ?: return@map null
        val tz = prefs[Keys.TIMEZONE] ?: "UTC"
        val imsak = prefs[Keys.IMSAK] ?: return@map null
        val fajr = prefs[Keys.FAJR] ?: return@map null
        val sunrise = prefs[Keys.SUNRISE] ?: return@map null
        val dhuhr = prefs[Keys.DHUHR] ?: return@map null
        val asr = prefs[Keys.ASR] ?: return@map null
        val maghrib = prefs[Keys.MAGHRIB] ?: return@map null
        val isha = prefs[Keys.ISHA] ?: return@map null

        date to PrayerDay(
            imsak = imsak,
            fajr = fajr,
            sunrise = sunrise,
            dhuhr = dhuhr,
            asr = asr,
            maghrib = maghrib,
            isha = isha,
            timezone = tz,
            gregorianDate = "",
            hijriDate = "",
            hijriMonthNumber = 0
        )
    }
}
