package dev.walter.carplay.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import dev.walter.carplay.data.AppPreferences
import dev.walter.carplay.service.CarModeService
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onRequestBatteryOptimization: () -> Unit) {
    val ctx = LocalContext.current
    val prefs = remember { AppPreferences(ctx) }
    val scope = rememberCoroutineScope()

    val serviceEnabled by prefs.serviceEnabled.collectAsState(true)
    val launchYt by prefs.launchYoutubeMusic.collectAsState(true)
    val launchBlitzer by prefs.launchBlitzer.collectAsState(true)
    val blitzerPkg by prefs.blitzerPackage.collectAsState("com.sygic.aura")
    val delay by prefs.delaySeconds.collectAsState(2)

    var showPkgDialog by remember { mutableStateOf(false) }
    var pkgInput by remember(blitzerPkg) { mutableStateOf(blitzerPkg) }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Walter CarPlay") }) }
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(horizontal = 16.dp)
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Spacer(Modifier.height(8.dp))

            // Service Toggle
            SectionLabel("Überwachung")
            ToggleRow(
                title = "Service aktiv",
                subtitle = "Erkennt USB-Audio-Verbindung im Hintergrund",
                checked = serviceEnabled,
                onCheckedChange = {
                    scope.launch { prefs.setServiceEnabled(it) }
                    if (it) CarModeService.start(ctx) else CarModeService.stop(ctx)
                }
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Apps
            SectionLabel("Apps beim Verbinden starten")
            ToggleRow(
                title = "YouTube Music",
                subtitle = "com.google.android.apps.youtube.music",
                checked = launchYt,
                onCheckedChange = { scope.launch { prefs.setLaunchYoutubeMusic(it) } }
            )
            ToggleRow(
                title = "BlitzerPro",
                subtitle = blitzerPkg,
                checked = launchBlitzer,
                onCheckedChange = { scope.launch { prefs.setLaunchBlitzer(it) } }
            )
            OutlinedButton(
                onClick = { showPkgDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) { Text("BlitzerPro Package-Name ändern") }

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Delay
            SectionLabel("Verzögerung: ${delay}s")
            Text(
                "Wartezeit nach Kabelerkennung bevor Apps starten",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Slider(
                value = delay.toFloat(),
                onValueChange = { scope.launch { prefs.setDelaySeconds(it.roundToInt()) } },
                valueRange = 0f..10f,
                steps = 9,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))

            // Battery
            SectionLabel("Wichtig: Batterie-Optimierung")
            Text(
                "Damit der Service bei langen Fahrten aktiv bleibt, muss die " +
                "Batterie-Optimierung für diese App deaktiviert werden.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(Modifier.height(8.dp))
            Button(
                onClick = onRequestBatteryOptimization,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Batterie-Optimierung deaktivieren") }

            Spacer(Modifier.height(16.dp))
        }
    }

    if (showPkgDialog) {
        AlertDialog(
            onDismissRequest = { showPkgDialog = false },
            title = { Text("BlitzerPro Package-Name") },
            text = {
                Column {
                    Text(
                        "Prüfe den genauen Package-Namen auf deinem Gerät:\n" +
                        "Einstellungen → Apps → BlitzerPro → App-Info",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = pkgInput,
                        onValueChange = { pkgInput = it },
                        label = { Text("Package-Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { prefs.setBlitzerPackage(pkgInput.trim()) }
                    showPkgDialog = false
                }) { Text("Speichern") }
            },
            dismissButton = {
                TextButton(onClick = { showPkgDialog = false }) { Text("Abbrechen") }
            }
        )
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun ToggleRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
