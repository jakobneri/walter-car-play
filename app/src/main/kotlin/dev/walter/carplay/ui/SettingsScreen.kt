package dev.walter.carplay.ui

import android.content.Intent
import android.os.Build
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import dev.walter.carplay.data.AppPreferences
import dev.walter.carplay.service.CarModeService
import dev.walter.carplay.ui.theme.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt

@Composable
fun SettingsScreen(onRequestBatteryOptimization: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { AppPreferences(ctx) }
    val scope = rememberCoroutineScope()

    val serviceEnabled by prefs.serviceEnabled.collectAsState(true)
    val apps by prefs.appList.collectAsState(listOf("com.google.android.apps.youtube.music", "com.sygic.aura"))
    val delay by prefs.delaySeconds.collectAsState(2)
    val keepScreen by prefs.keepScreenOn.collectAsState(false)
    val setVol by prefs.setVolume.collectAsState(false)
    val volLevel by prefs.volumeLevel.collectAsState(80)

    var showAppPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            "WALTER CAR PLAY",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(14.dp),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Service", color = TextPrimary, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
                    val statusColor by animateColorAsState(if (serviceEnabled) Green else TextSecondary, label = "")
                    Text(
                        if (serviceEnabled) "● Aktiv – wartet auf Kabel" else "○ Deaktiviert",
                        color = statusColor,
                        fontSize = 13.sp
                    )
                }
                Switch(
                    checked = serviceEnabled,
                    onCheckedChange = {
                        scope.launch { prefs.setServiceEnabled(it) }
                        if (it) CarModeService.start(ctx) else CarModeService.stop(ctx)
                    },
                    colors = SwitchDefaults.colors(checkedThumbColor = Orange, checkedTrackColor = OrangeDim)
                )
            }
        }

        SectionLabel("APPS BEIM VERBINDEN")
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                apps.forEachIndexed { index, pkg ->
                    AppRow(
                        pkg = pkg,
                        onRemove = {
                            scope.launch { prefs.setAppList(apps.toMutableList().also { it.removeAt(index) }) }
                        }
                    )
                }
                if (apps.size < 5) {
                    TextButton(
                        onClick = { showAppPicker = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Orange)
                        Spacer(Modifier.width(8.dp))
                        Text("App auswählen", color = Orange)
                    }
                }
            }
        }

        SectionLabel("VERZÖGERUNG NACH KABELANSCHLUSS")
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Wartezeit", color = TextPrimary, fontSize = 15.sp)
                    Text("${delay}s", color = Orange, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
                Slider(
                    value = delay.toFloat(),
                    onValueChange = { scope.launch { prefs.setDelaySeconds(it.roundToInt()) } },
                    valueRange = 0f..10f,
                    steps = 9,
                    colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        SectionLabel("OPTIONEN")
        Card(
            colors = CardDefaults.cardColors(containerColor = Card),
            shape = RoundedCornerShape(14.dp),
        ) {
            Column(modifier = Modifier.padding(8.dp)) {
                OptionRow("Bildschirm einschalten", "Schaltet den Bildschirm beim Anstecken an", keepScreen) {
                    scope.launch { prefs.setKeepScreenOn(it) }
                }
                HorizontalDivider(color = CardHighlight, modifier = Modifier.padding(horizontal = 12.dp))
                OptionRow("Lautstärke setzen", "Musik-Lautstärke beim Verbinden anpassen", setVol) {
                    scope.launch { prefs.setSetVolume(it) }
                }
                if (setVol) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Lautstärke", color = TextSecondary, fontSize = 14.sp)
                        Text("${volLevel}%", color = Orange, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                    Slider(
                        value = volLevel.toFloat(),
                        onValueChange = { scope.launch { prefs.setVolumeLevel(it.roundToInt()) } },
                        valueRange = 0f..100f,
                        steps = 9,
                        colors = SliderDefaults.colors(thumbColor = Orange, activeTrackColor = Orange),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .padding(bottom = 8.dp)
                    )
                }
            }
        }

        Button(
            onClick = { CarModeService.testLaunch(ctx) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Orange),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Default.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("JETZT TESTEN", fontWeight = FontWeight.Bold, fontSize = 15.sp, letterSpacing = 1.sp)
        }

        SectionLabel("SYSTEM")

        // Android 14+: fullScreenIntent needs explicit permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            val nm = remember { ctx.getSystemService(android.app.NotificationManager::class.java) }
            val canFullScreen by produceState(nm.canUseFullScreenIntent()) {
                // recheck whenever screen is resumed
                value = nm.canUseFullScreenIntent()
            }
            if (!canFullScreen) {
                OutlinedButton(
                    onClick = {
                        ctx.startActivity(
                            android.content.Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
                                data = android.net.Uri.parse("package:${ctx.packageName}")
                            }
                        )
                    },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Orange),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Vollbild-Berechtigung erteilen", fontSize = 14.sp)
                }
                Text(
                    "Damit sich die Apps automatisch öffnen (ohne Tippen), muss die Vollbild-Benachrichtigung erlaubt sein.",
                    color = TextSecondary,
                    fontSize = 12.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(4.dp))
            }
        }

        OutlinedButton(
            onClick = onRequestBatteryOptimization,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("Batterie-Optimierung deaktivieren", fontSize = 14.sp)
        }
        Text(
            "Damit der Service bei langen Fahrten aktiv bleibt, sollte die Batterie-Optimierung für diese App deaktiviert sein.",
            color = TextSecondary,
            fontSize = 12.sp,
            lineHeight = 18.sp
        )

        Spacer(Modifier.height(24.dp))
    }

    if (showAppPicker) {
        InstalledAppsDialog(
            currentApps = apps,
            onConfirm = { pkg ->
                scope.launch { prefs.setAppList(apps + pkg) }
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }
}

@Composable
private fun AppRow(pkg: String, onRemove: () -> Unit) {
    val ctx = LocalContext.current
    val name = remember(pkg) {
        try {
            val ai = ctx.packageManager.getApplicationInfo(pkg, 0)
            ctx.packageManager.getApplicationLabel(ai).toString()
        } catch (_: Exception) {
            pkg.substringAfterLast(".").replaceFirstChar { it.uppercase() }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardHighlight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
            Text(pkg, color = TextSecondary, fontSize = 11.sp)
        }
        IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
            Icon(Icons.Default.Close, contentDescription = "Entfernen", tint = TextSecondary, modifier = Modifier.size(18.dp))
        }
    }
}

@Composable
private fun OptionRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = TextPrimary, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(subtitle, color = TextSecondary, fontSize = 12.sp)
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Orange, checkedTrackColor = OrangeDim)
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        color = TextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(start = 4.dp, top = 4.dp)
    )
}

@Composable
private fun InstalledAppsDialog(
    currentApps: List<String>,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val ctx = LocalContext.current
    var search by remember { mutableStateOf("") }

    val installedApps by produceState<List<Pair<String, String>>>(initialValue = emptyList()) {
        value = withContext(Dispatchers.IO) {
            val pm = ctx.packageManager
            val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
                .map { ri ->
                    val pkg = ri.activityInfo.packageName
                    val label = try {
                        pm.getApplicationLabel(ri.activityInfo.applicationInfo).toString()
                    } catch (_: Exception) { pkg }
                    pkg to label
                }
                .distinctBy { it.first }
                .filter { it.first !in currentApps }
                .sortedBy { it.second.lowercase() }
        }
    }

    val filtered = if (search.isBlank()) installedApps
    else installedApps.filter {
        it.second.contains(search, ignoreCase = true) ||
                it.first.contains(search, ignoreCase = true)
    }

    AlertDialog(
        containerColor = Card,
        onDismissRequest = onDismiss,
        title = { Text("App auswählen", color = TextPrimary) },
        text = {
            Column {
                OutlinedTextField(
                    value = search,
                    onValueChange = { search = it },
                    placeholder = { Text("Suchen…", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = CardHighlight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    )
                )
                Spacer(Modifier.height(8.dp))
                if (installedApps.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Orange, modifier = Modifier.size(32.dp))
                    }
                } else {
                    LazyColumn(modifier = Modifier.height(320.dp)) {
                        items(filtered, key = { it.first }) { (pkg, name) ->
                            AppPickerRow(pkg = pkg, name = name, onClick = { onConfirm(pkg) })
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = TextSecondary) }
        }
    )
}

@Composable
private fun AppPickerRow(pkg: String, name: String, onClick: () -> Unit) {
    val ctx = LocalContext.current
    var icon by remember(pkg) { mutableStateOf<ImageBitmap?>(null) }
    LaunchedEffect(pkg) {
        icon = withContext(Dispatchers.IO) {
            try {
                ctx.packageManager.getApplicationIcon(pkg).toBitmap().asImageBitmap()
            } catch (_: Exception) { null }
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 4.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Image(
                bitmap = icon!!,
                contentDescription = null,
                modifier = Modifier.size(36.dp).clip(RoundedCornerShape(8.dp))
            )
        } else {
            Spacer(Modifier.size(36.dp))
        }
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(name, color = TextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(pkg, color = TextSecondary, fontSize = 11.sp, maxLines = 1)
        }
    }
}
