package dev.walter.carplay.ui

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.walter.carplay.data.AppPreferences
import dev.walter.carplay.service.CarModeService
import dev.walter.carplay.ui.theme.*
import kotlinx.coroutines.launch
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

    var showAddDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BgDark)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Header
        Text(
            "WALTER CAR PLAY",
            color = TextPrimary,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
        )

        // Service toggle card
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

        // Apps section
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
                        onClick = { showAddDialog = true },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, tint = Orange)
                        Spacer(Modifier.width(8.dp))
                        Text("App hinzufügen", color = Orange)
                    }
                }
            }
        }

        // Delay
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

        // Optionen
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

        // Test button
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

        // System
        SectionLabel("SYSTEM")
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

    if (showAddDialog) {
        AddAppDialog(
            onConfirm = { pkg ->
                if (pkg.isNotBlank()) scope.launch { prefs.setAppList(apps + pkg.trim()) }
                showAddDialog = false
            },
            onDismiss = { showAddDialog = false }
        )
    }
}

@Composable
private fun AppRow(pkg: String, onRemove: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(CardHighlight)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(appName(pkg), color = TextPrimary, fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
private fun AddAppDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var input by remember { mutableStateOf("") }
    AlertDialog(
        containerColor = Card,
        onDismissRequest = onDismiss,
        title = { Text("App hinzufügen", color = TextPrimary) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    "Package-Name eingeben, z.B.:\ncom.spotify.music\nde.blitzerpro",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
                OutlinedTextField(
                    value = input,
                    onValueChange = { input = it },
                    placeholder = { Text("com.example.app", color = TextSecondary) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Orange,
                        unfocusedBorderColor = CardHighlight,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary,
                    )
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(input) }) { Text("Hinzufügen", color = Orange) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Abbrechen", color = TextSecondary) }
        }
    )
}

private fun appName(pkg: String) = when (pkg) {
    "com.google.android.apps.youtube.music" -> "YouTube Music"
    "com.sygic.aura" -> "Sygic / BlitzerPro"
    "de.blitzerpro" -> "BlitzerPro"
    "com.spotify.music" -> "Spotify"
    "com.waze" -> "Waze"
    "com.google.android.apps.maps" -> "Google Maps"
    else -> pkg.substringAfterLast(".")
        .replaceFirstChar { it.uppercase() }
}
