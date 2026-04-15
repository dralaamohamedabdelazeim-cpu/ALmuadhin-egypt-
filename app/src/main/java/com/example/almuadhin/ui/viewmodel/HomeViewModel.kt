package com.example.almuadhin.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.almuadhin.alarm.PrayerAlarmScheduler
import com.example.almuadhin.data.LocationMode
import com.example.almuadhin.data.PrayerDay
import com.example.almuadhin.data.SettingsRepository
import com.example.almuadhin.data.UserSettings
import com.example.almuadhin.data.repo.LocationRepository
import com.example.almuadhin.data.repo.PrayerRepository
import com.example.almuadhin.util.TimeUtils
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

data class HomeUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val settings: UserSettings = UserSettings(),
    val day: PrayerDay? = null,
    val selectedDateDay: PrayerDay? = null, // Prayer times for selected date in calendar
    val nextPrayerName: String? = null,
    val nextPrayerTime: String? = null,
    val countdown: String? = null,
    val isOffline: Boolean = false,
    val lastUpdated: String? = null, 
   val cityName: String? = null,
  )

@HiltViewModel
class HomeViewModel @Inject constructor(
  @dagger.hilt.android.qualifiers.ApplicationContext private val context: android.content.Context,
    private val settingsRepo: SettingsRepository,
    private val prayerRepo: PrayerRepository,
    private val locationRepo: LocationRepository,
    private val scheduler: PrayerAlarmScheduler
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state

    private val zoneId = ZoneId.systemDefault()

    init {
        viewModelScope.launch {
            // First, try to load cached data immediately
            loadCachedPrayerTimes()
            
            settingsRepo.settingsFlow.collect { s ->
                _state.value = _state.value.copy(settings = s)
                refresh()
            }
        }
        startTicker()
    }

    /**
     * Load cached prayer times from Room database (offline support)
     */
    private suspend fun loadCachedPrayerTimes() {
        // First check Room database for today's data
        if (prayerRepo.hasCachedDataForToday()) {
            val date = TimeUtils.todayDdMmYyyy(zoneId)
            val s = settingsRepo.settingsFlow.first()
            try {
                val day = if (s.locationMode == LocationMode.AUTO) {
                    val loc = locationRepo.getLastKnownLocation()
                    if (loc != null) {
                       val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
                val address = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
               val city = address?.firstOrNull()?.locality
                ?: address?.firstOrNull()?.subAdminArea
                ?: address?.firstOrNull()?.adminArea
                _state.value = _state.value.copy(cityName = city)
                        prayerRepo.getPrayerDayByCoordinates(date, loc.latitude, loc.longitude, s.calculationMethod)
                    } else {
                        prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                } else {
                    prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                }
                
                _state.value = _state.value.copy(
                    day = day,
                    isLoading = false,
                    isOffline = false,
                    lastUpdated = "محفوظ مسبقاً",
                    error = null
                )
                computeNext(day)
            } catch (e: Exception) {
                // Fallback to legacy DataStore cache
                val cached = settingsRepo.storedPrayerDayFlow.first()
                if (cached != null) {
                    _state.value = _state.value.copy(
                        day = cached.second,
                        isLoading = false,
                        isOffline = true,
                        lastUpdated = "محفوظ مسبقاً",
                        error = null
                    )
                    computeNext(cached.second)
                }
            }
        } else {
            // Fallback to legacy DataStore cache
            val cached = settingsRepo.storedPrayerDayFlow.first()
            val todayIso = TimeUtils.todayIso(zoneId)
            
            if (cached != null && cached.first == todayIso) {
                _state.value = _state.value.copy(
                    day = cached.second,
                    isLoading = false,
                    isOffline = true,
                    lastUpdated = "محفوظ مسبقاً",
                    error = null
                )
                computeNext(cached.second)
            }
        }
    }

    fun refresh() {
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, error = null)
            val s = settingsRepo.settingsFlow.first()
            // Force update settings in state to ensure UI reflects changes immediately
            _state.value = _state.value.copy(settings = s)

            val date = TimeUtils.todayDdMmYyyy(zoneId)
            val day = runCatching {
           if (s.locationMode == LocationMode.AUTO) {
    val cachedLoc = settingsRepo.getLocationCache()
    if (cachedLoc != null) {
        prayerRepo.getPrayerDayByCoordinates(date, cachedLoc.first, cachedLoc.second, s.calculationMethod)
    } else {
        val loc = locationRepo.getLastKnownLocation()
        if (loc != null) {
            val geocoder = android.location.Geocoder(context, java.util.Locale("ar"))
            val address = geocoder.getFromLocation(loc.latitude, loc.longitude, 1)
            val city = address?.firstOrNull()?.locality
                ?: address?.firstOrNull()?.subAdminArea
                ?: address?.firstOrNull()?.adminArea ?: ""
            settingsRepo.saveLocationCache(loc.latitude, loc.longitude, city)
            _state.value = _state.value.copy(cityName = city)
            prayerRepo.getPrayerDayByCoordinates(date, loc.latitude, loc.longitude, s.calculationMethod)
        } else {
            prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
        }
    }
                } else {
                    prayerRepo.getPrayerDayByCity(date, s.manualCity, s.manualCountry, s.calculationMethod)
                }
            }.getOrElse { e ->
                // Failed to fetch from network, try to use cached data
                val cached = settingsRepo.storedPrayerDayFlow.first()
                if (cached != null) {
                    // Use cached data (even if it's from a different day)
                    _state.value = _state.value.copy(
                        isLoading = false,
                        day = cached.second,
                        isOffline = true,
                        lastUpdated = "آخر تحديث: ${cached.first}",
                        error = null
                    )
                    computeNext(cached.second)
                    
                    // Schedule alarms even with cached data
                    if (s.notificationsEnabled && scheduler.canScheduleExactAlarms()) {
                        scheduler.cancelAll()
                        scheduler.scheduleForToday(cached.second, s.adhanSound.name, zoneId)
                    }
                } else {
                    _state.value = _state.value.copy(
                        isLoading = false, 
                        error = "لا يوجد اتصال بالإنترنت ولا توجد بيانات محفوظة",
                        isOffline = true
                    )
                }
                return@launch
            }

            // Successfully fetched from network
            _state.value = _state.value.copy(
                isLoading = false, 
                day = day, 
                error = null,
                isOffline = false,
                lastUpdated = "تم التحديث الآن"
            )

            // Save for offline use and reboot reschedule
            runCatching { 
                settingsRepo.savePrayerDayForReschedule(TimeUtils.todayIso(zoneId), day) 
            }

            // Schedule alarms if enabled (and allowed)
            if (s.notificationsEnabled && scheduler.canScheduleExactAlarms()) {
                scheduler.cancelAll()
                scheduler.scheduleForToday(day, s.adhanSound.name, zoneId)
            }

            computeNext(day)
            
            // Fetch and cache 7 days in background for offline support
            launch {
                runCatching {
                    if (s.locationMode == LocationMode.AUTO) {
                        val loc = locationRepo.getLastKnownLocation()
                        if (loc != null) {
                            prayerRepo.fetchAndCacheWeek(loc.latitude, loc.longitude, s.calculationMethod)
                        } else {
                            prayerRepo.fetchAndCacheWeekByCity(s.manualCity, s.manualCountry, s.calculationMethod)
                        }
                    } else {
                        prayerRepo.fetchAndCacheWeekByCity(s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                }
            }
        }
    }

    /**
     * Fetch prayer times for a specific date (for calendar view)
     */
    fun fetchPrayerTimesForDate(date: LocalDate) {
        viewModelScope.launch {
            val s = settingsRepo.settingsFlow.first()
            val dateStr = date.format(java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy"))
            
            runCatching {
                val day = if (s.locationMode == LocationMode.AUTO) {
                    val loc = locationRepo.getLastKnownLocation()
                    if (loc != null) {
                        prayerRepo.getPrayerDayByCoordinates(dateStr, loc.latitude, loc.longitude, s.calculationMethod)
                    } else {
                        prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                    }
                } else {
                    prayerRepo.getPrayerDayByCity(dateStr, s.manualCity, s.manualCountry, s.calculationMethod)
                }
                _state.value = _state.value.copy(selectedDateDay = day)
            }.onFailure {
                // If we can't get data for the date, use today's data as fallback
                _state.value = _state.value.copy(selectedDateDay = _state.value.day)
            }
        }
    }

    private fun startTicker() {
        viewModelScope.launch {
            while (true) {
                val d = _state.value.day
                if (d != null) computeNext(d)
                delay(1000L)
            }
        }
    }

    private fun computeNext(day: PrayerDay) {
        val now = System.currentTimeMillis()
        val today = LocalDate.now(zoneId)

        val schedule = listOf(
            "الفجر" to day.fajr,
            "الظهر" to day.dhuhr,
            "العصر" to day.asr,
            "المغرب" to day.maghrib,
            "العشاء" to day.isha
        )

        var nextName: String? = null
        var nextTime: String? = null
        var nextMillis: Long? = null

        for ((name, time) in schedule) {
            val ms = TimeUtils.toMillis(today, time, zoneId)
            if (ms > now) {
                nextName = name
                nextTime = time
                nextMillis = ms
                break
            }
        }

        if (nextMillis == null) {
            // Next is tomorrow's Fajr
            nextName = "الفجر"
            nextTime = day.fajr
            nextMillis = TimeUtils.toMillis(today.plusDays(1), day.fajr, zoneId)
        }

        _state.value = _state.value.copy(
            nextPrayerName = nextName,
            nextPrayerTime = nextTime,
            countdown = TimeUtils.formatDuration(TimeUtils.millisUntil(nextMillis))
        )
    }
}
