package com.example.almuadhin.ui.screens

import android.Manifest
import android.content.Intent
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.almuadhin.alarm.PrayerAlarmScheduler
import com.example.almuadhin.data.AdhanSound
import com.example.almuadhin.data.CalculationMethod
import com.example.almuadhin.data.LocationMode
import com.example.almuadhin.noor.config.ConfigManager
import com.example.almuadhin.noor.data.ApiProvider
import com.example.almuadhin.ui.viewmodel.SettingsViewModel
import dagger.hilt.android.EntryPointAccessors

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    contentPadding: PaddingValues,
    vm: SettingsViewModel = hiltViewModel()
) {
    val ctx = LocalContext.current
    val s by vm.ui.collectAsState()

    val appCtx = ctx.applicationContext
    val entryPoint = remember {
        EntryPointAccessors.fromApplication(appCtx, SchedulerEntryPoint::class.java)
    }
    val scheduler = entryPoint.scheduler()

    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var playingSound by remember { mutableStateOf<AdhanSound?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            mediaPlayer?.release()
        }
    }

    val locationLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* handled by system */ }

    val notifLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* handled by system */ }

    fun requestLocation() {
        locationLauncher.launch(arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ))
    }

    fun requestNotifications() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    fun openExactAlarmSettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
                data = Uri.parse("package:${ctx.packageName}")
            }
            ctx.startActivity(intent)
        }
    }

    fun toggleAdhanPreview(sound: AdhanSound) {
        if (playingSound == sound) {
            // Stop current
            mediaPlayer?.stop()
            mediaPlayer?.release()
            mediaPlayer = null
            playingSound = null
        } else {
            // Play new
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer.create(ctx, sound.resId).apply {
                setOnCompletionListener { 
                    release()
                    playingSound = null
                    mediaPlayer = null 
                }
                start()
            }
            playingSound = sound
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .background(MaterialTheme.colorScheme.background)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "الإعدادات",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    
                    // App Icon
                    androidx.compose.foundation.Image(
                        painter = androidx.compose.ui.res.painterResource(id = com.example.almuadhin.R.drawable.icon),
                        contentDescription = "App Icon",
                        modifier = Modifier
                            .size(64.dp)
                            .clip(RoundedCornerShape(16.dp))
                    )
                }
            }

            // Location Settings
            item {
                SettingsCard(
                    title = "الموقع",
                    icon = Icons.Default.LocationOn
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        FilterChip(
                            selected = s.locationMode == LocationMode.AUTO,
                            onClick = { vm.setLocationMode(LocationMode.AUTO); requestLocation() },
                            label = { Text("تلقائي (GPS)") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = s.locationMode == LocationMode.MANUAL,
                            onClick = { vm.setLocationMode(LocationMode.MANUAL) },
                            label = { Text("يدوي") },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = Color.White
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (s.locationMode == LocationMode.MANUAL) {
                        Spacer(Modifier.height(12.dp))
                        OutlinedTextField(
                            value = s.manualCity,
                            onValueChange = { vm.setManual(it, s.manualCountry) },
                            label = { Text("المدينة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Spacer(Modifier.height(8.dp))
                        OutlinedTextField(
                            value = s.manualCountry,
                            onValueChange = { vm.setManual(s.manualCity, it) },
                            label = { Text("الدولة") },
                            modifier = Modifier.fillMaxWidth(),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    } else {
                        Spacer(Modifier.height(8.dp))
                        TextButton(onClick = { requestLocation() }) {
                            Icon(Icons.Default.MyLocation, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("طلب صلاحية الموقع")
                        }
                    }
                }
            }

            // Calculation Method
            item {
                SettingsCard(
                    title = "طريقة الحساب",
                    icon = Icons.Default.Calculate
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    ExposedDropdownMenuBox(
                        expanded = expanded,
                        onExpandedChange = { expanded = !expanded }
                    ) {
                        OutlinedTextField(
                            value = s.calculationMethod.labelAr,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("اختيار الطريقة") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                            modifier = Modifier
                                .menuAnchor(type = MenuAnchorType.PrimaryNotEditable, enabled = true)
                                .fillMaxWidth(),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                focusedLabelColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        ExposedDropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            CalculationMethod.entries.forEach { m ->
                                DropdownMenuItem(
                                    text = { Text(m.labelAr) },
                                    onClick = {
                                        vm.setMethod(m)
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // Adhan Sound Settings
            item {
                SettingsCard(
                    title = "صوت الأذان",
                    icon = Icons.Default.Notifications
                ) {
                    Text(
                        "اختر صوت الأذان المفضل",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))

                    AdhanSound.entries.forEach { sound ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { vm.setAdhanSound(sound) }
                                .background(
                                    if (s.adhanSound == sound) MaterialTheme.colorScheme.outline
                                    else Color.Transparent
                                )
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                RadioButton(
                                    selected = s.adhanSound == sound,
                                    onClick = { vm.setAdhanSound(sound) },
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                                Column {
                                    Text(
                                        sound.labelAr,
                                        fontWeight = if (s.adhanSound == sound) FontWeight.Bold else FontWeight.Normal
                                    )
                                    if (sound.isFull) {
                                        Text(
                                            "أذان كامل",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }
                            }

                            // Play/Stop Button
                            IconButton(
                                onClick = { toggleAdhanPreview(sound) }
                            ) {
                                Icon(
                                    imageVector = if (playingSound == sound) Icons.Default.StopCircle else Icons.Default.PlayCircle,
                                    contentDescription = if (playingSound == sound) "إيقاف" else "تشغيل",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(
                        modifier = Modifier.padding(vertical = 12.dp),
                        color = MaterialTheme.colorScheme.outlineVariant
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تشغيل الأذان الكامل")
                            Text(
                                "عند تفعيل هذا الخيار، سيتم تشغيل الأذان كاملاً",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = s.playFullAdhan,
                            onCheckedChange = { vm.setPlayFullAdhan(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }
                }
            }

            // Notifications
            item {
                SettingsCard(
                    title = "الإشعارات",
                    icon = Icons.Default.Notifications
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("تفعيل الإشعارات")
                        Switch(
                            checked = s.notificationsEnabled,
                            onCheckedChange = { enabled ->
                                vm.setNotifications(enabled)
                                if (enabled) requestNotifications()
                            },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (s.notificationsEnabled) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !scheduler.canScheduleExactAlarms()) {
                            Spacer(Modifier.height(12.dp))
                            ElevatedCard(
                                colors = CardDefaults.elevatedCardColors(
                                    containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        "⚠️ ملاحظة",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                    Text(
                                        "جهازك يحتاج تفعيل (Exact Alarms) لإرسال تنبيهات الأذان بدقة",
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                    Spacer(Modifier.height(8.dp))
                                    Button(
                                        onClick = { openExactAlarmSettings() },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = MaterialTheme.colorScheme.primary
                                        )
                                    ) {
                                        Text("تفعيل الآن")
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Ads Settings
            item {
                SettingsCard(
                    title = "الإعلانات",
                    icon = Icons.Default.Block
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("إزالة الإعلانات")
                        Switch(
                            checked = s.adsRemoved,
                            onCheckedChange = { vm.setAdsRemoved(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                    }

                    if (!s.adsRemoved) {
                        Spacer(Modifier.height(12.dp))
                        Text(
                            "تخفيف ظهور الإعلان (بالدقائق)",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Slider(
                            value = s.adCooldownMinutes.toFloat(),
                            onValueChange = { vm.setAdCooldown(it.toInt()) },
                            valueRange = 0f..60f,
                            steps = 5,
                            colors = SliderDefaults.colors(
                                thumbColor = MaterialTheme.colorScheme.primary,
                                activeTrackColor = MaterialTheme.colorScheme.primary
                            )
                        )
                        Text(
                            "القيمة الحالية: ${s.adCooldownMinutes} دقيقة",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Noor AI Settings
         //   item {
        //        NoorSettingsCard()
        //    }

            // App Info
            item {
                SettingsCard(
                    title = "حول التطبيق",
                    icon = Icons.Default.Info
                ) {
                    Text(
                        "المؤذن - تطبيق محمد عبد العظيم الطويل مواقيت الصلاة",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "الإصدار 1.0.0",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                     spacer(8.dp)
                    Text(
                        "🕌 مواقيت الصلاة الدقيقة\n🧭 اتجاه القبلة\n📅 التقويم الهجري\n📿 الأذكار\n🌙 عداد رمضان",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    Spacer(Modifier.height(16.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Spacer(Modifier.height(12.dp))

                    Text(
                         "المطور: علاء محمد عبد العظيم ",
                         style = MaterialTheme.typography.titleSmall,
                         fontWeight = FontWeight.Bold,
                         color = MaterialTheme.colorScheme.primary
                    )

                    
            }

            item {
                Spacer(Modifier.height(80.dp))
            }
        }
    }
}

@Composable
private fun SettingsCard(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit
) {
    ElevatedCard(
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.padding(bottom = 12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            content()
        }
    }
}

// Hilt EntryPoint to access scheduler from composable
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface SchedulerEntryPoint {
    fun scheduler(): PrayerAlarmScheduler
}

@Composable
fun spacer(height: Int) {
    Spacer(Modifier.height(height.dp))
}

@Composable
fun spacer(height: androidx.compose.ui.unit.Dp) {
    Spacer(Modifier.height(height))
}

@OptIn(ExperimentalMaterial3Api::class)
                        
