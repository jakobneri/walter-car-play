# Walter CarPlay – Android App Plan

## Ziel

Eine Android-App, die **automatisch YouTube Music und BlitzerPro startet**, sobald ein USB-C-zu-AUX-Kabel angeschlossen wird. Das Kabel lädt das Gerät nicht – es ist ein reiner Audio-Adapter mit eingebautem DAC-Chip.

---

## Technische Kernherausforderung: Kabelerkennung

Ein USB-C-zu-AUX-Adapter erscheint in Android als **USB-Audio-Gerät** (der DAC-Chip im Kabel). Da keine Ladung stattfindet, greift `ACTION_POWER_CONNECTED` nicht.

**Lösung:** `AudioManager.AudioDeviceCallback` – wird gefeuert, sobald sich die Audio-Routing-Geräte ändern.

```kotlin
audioManager.registerAudioDeviceCallback(object : AudioDeviceCallback() {
    override fun onAudioDevicesAdded(addedDevices: Array<AudioDeviceInfo>) {
        val usbAudio = addedDevices.any {
            it.type == AudioDeviceInfo.TYPE_USB_HEADSET ||
            it.type == AudioDeviceInfo.TYPE_USB_DEVICE
        }
        if (usbAudio) launchCarMode()
    }
    override fun onAudioDevicesRemoved(removedDevices: Array<AudioDeviceInfo>) {
        // optional: Car Mode beenden
    }
}, handler)
```

---

## App-Architektur

```
walter-car-play/
├── app/
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── kotlin/dev/walter/carplay/
│   │   │   ├── MainActivity.kt            ← Einstellungs-Screen
│   │   │   ├── service/
│   │   │   │   └── CarModeService.kt      ← Foreground Service (Kern)
│   │   │   ├── receiver/
│   │   │   │   └── BootReceiver.kt        ← Service nach Neustart starten
│   │   │   ├── ui/
│   │   │   │   ├── SettingsScreen.kt      ← Compose UI
│   │   │   │   └── DriveOverlay.kt        ← Optionaler Fahr-Overlay
│   │   │   └── data/
│   │   │       └── Preferences.kt         ← DataStore-Einstellungen
│   │   └── res/
│   │       └── ...
│   └── build.gradle.kts
└── PLAN.md
```

---

## Komponenten im Detail

### 1. `CarModeService` (Foreground Service)

Der zentrale Dienst, der dauerhaft im Hintergrund läuft.

**Aufgaben:**
- Registriert `AudioDeviceCallback` beim AudioManager
- Erkennt USB-Audio-Verbindung → startet YouTube Music & BlitzerPro
- Erkennt Trennung → optionaler Cleanup
- Läuft als Foreground Service mit dauerhafter Benachrichtigung

**Service-Typ im Manifest:**
```xml
<service
    android:name=".service.CarModeService"
    android:foregroundServiceType="connectedDevice"
    android:exported="false" />
```

**App-Start (Android 10+ Background Launch):**

Ab Android 10 dürfen Hintergrund-Services keine Activities mehr direkt starten. Lösung:
- Der Service startet eine **transparente `LauncherActivity`** über einen PendingIntent aus der Notification
- Oder: `SYSTEM_ALERT_WINDOW`-Permission für direkten Overlay-Start

Einfachste zuverlässige Methode: Notification mit `fullScreenIntent` + eigene transparente Activity, die die Ziel-Apps öffnet.

```kotlin
private fun launchApps() {
    val launchIntent = Intent(this, LauncherActivity::class.java).apply {
        flags = Intent.FLAG_ACTIVITY_NEW_TASK
        putExtra("apps", arrayOf(
            "com.google.android.apps.youtube.music",
            "com.sygic.aura"  // BlitzerPro Package-Name
        ))
    }
    startActivity(launchIntent)
}
```

---

### 2. `BootReceiver` (BroadcastReceiver)

Startet den `CarModeService` nach Geräteneustart automatisch.

```xml
<receiver android:name=".receiver.BootReceiver" android:exported="true">
    <intent-filter>
        <action android:name="android.intent.action.BOOT_COMPLETED" />
        <action android:name="android.intent.action.MY_PACKAGE_REPLACED" />
    </intent-filter>
</receiver>
```

---

### 3. `MainActivity` (Einstellungen)

Einfacher Settings-Screen (Jetpack Compose):

| Setting | Beschreibung |
|---|---|
| Service aktivieren/deaktivieren | Toggle |
| Apps beim Verbinden starten | Liste installierten Apps, auswählen |
| Verzögerung | Slider 0–10 Sekunden (nach Kabelverbindung) |
| App nach Verbindung trennen | Toggle für Auto-Stop |
| Batterie-Optimierung deaktivieren | Button → System-Dialog |

---

### 4. Optionaler Fahr-Overlay (`DriveOverlay`)

Ein minimalistisches Overlay (TYPE_APPLICATION_OVERLAY) mit:
- Großem Play/Pause-Button (YouTube Music MediaSession steuern)
- Aktueller Straßenwarnung von BlitzerPro (falls API vorhanden)
- Zurück-Button zum schließen

---

## Berechtigungen (`AndroidManifest.xml`)

```xml
<!-- Service im Vordergrund -->
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_CONNECTED_DEVICE" />

<!-- Autostart nach Neustart -->
<uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />

<!-- Installierte Apps abfragen -->
<uses-permission android:name="android.permission.QUERY_ALL_PACKAGES" />

<!-- Overlay für direkten App-Start aus Background -->
<uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />

<!-- Batterie-Optimierung umgehen -->
<uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />

<!-- Für Notifications (Android 13+) -->
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

---

## Ablauf beim Kabelanstecken

```
USB-C Kabel angesteckt
        │
        ▼
CarModeService.AudioDeviceCallback
  → TYPE_USB_HEADSET / TYPE_USB_DEVICE erkannt
        │
        ▼
[Optional: X Sekunden warten (Einstellung)]
        │
        ▼
LauncherActivity starten (transparent)
  → YouTube Music Intent feuern
  → BlitzerPro Intent feuern
        │
        ▼
Activity schließt sich selbst
```

---

## Tech Stack

| Komponente | Technologie |
|---|---|
| Sprache | Kotlin |
| UI | Jetpack Compose |
| Einstellungen | DataStore (Preferences) |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |
| Build | Gradle (Kotlin DSL) |

---

## Package-Namen der Ziel-Apps

| App | Package |
|---|---|
| YouTube Music | `com.google.android.apps.youtube.music` |
| BlitzerPro | `com.sygic.aura` *(zu verifizieren – ggf. `de.blitzerpro.app`)* |

> Die genauen Package-Namen können per `adb shell pm list packages | grep blitzer` auf dem Gerät geprüft werden.

---

## Implementierungsreihenfolge

1. **Gradle-Projekt aufsetzen** – Android App Module mit Kotlin + Compose
2. **`CarModeService`** implementieren – AudioDeviceCallback + App-Launch-Logik
3. **`BootReceiver`** implementieren – Autostart
4. **`AndroidManifest.xml`** komplett konfigurieren
5. **`MainActivity`** + Settings-Screen (Compose) bauen
6. **Testen** – auf Gerät mit USB-C-zu-AUX-Adapter
7. **Optional:** Fahr-Overlay

---

## Bekannte Einschränkungen

- **Android 12+ Exact Alarms / Background Launch:** Die transparente LauncherActivity ist der zuverlässigste Weg, da `startActivity()` aus einem Service ab Android 10 blockiert wird.
- **BlitzerPro Package-Name:** Muss auf dem Zielgerät verifiziert werden.
- **Batterie-Optimierung:** Muss deaktiviert werden, sonst killt Android den Foreground Service bei langen Fahrten.
- **USB-C Adapter-Typ:** Manche billigen Adapter erscheinen als `TYPE_WIRED_HEADSET` statt `TYPE_USB_HEADSET` – der Callback sollte beide Typen prüfen.
