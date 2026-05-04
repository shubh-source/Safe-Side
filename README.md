# SafeSignal — Personal SOS App 💜

A private safety app for two people. When she presses the **power button 4 times rapidly**, your phone rings at full volume, shows a live map of her location, and auto-calls her — all within seconds.

---

## ⚡ Quick Setup (15 minutes total)

### Step 1 — Create a Free Firebase Project

1. Go to **[console.firebase.google.com](https://console.firebase.google.com)**
2. Click **"Add project"** → Name it `SafeSignal` → Continue
3. Disable Google Analytics (optional) → **Create project**
4. In the left sidebar → **Build → Realtime Database**
5. Click **"Create Database"** → Choose **"Start in test mode"** → Enable
6. Now go to **Project Settings** (gear icon top-left)
7. Under **"Your apps"** → click the Android icon `</>`
8. Enter package name: `com.safesignal` → Register
9. **Download `google-services.json`**
10. Place it at: `SafeSignal/app/google-services.json` (replace the placeholder)

---

### Step 2 — Open in Android Studio

1. Download [Android Studio](https://developer.android.com/studio) if not installed
2. Open Android Studio → **File → Open** → Select the `SafeSignal/` folder
3. Wait for Gradle sync to complete (~2–3 minutes first time)

---

### Step 3 — Build the APK

```
Build → Build Bundle(s) / APK(s) → Build APK(s)
```

The APK will be at:
```
SafeSignal/app/build/outputs/apk/debug/app-debug.apk
```

---

### Step 4 — Install on Both Phones

On each Android phone:
1. Enable **Settings → Security → Install unknown apps** (for your file manager)
2. Copy the APK via USB / WhatsApp / Google Drive
3. Tap the APK to install

---

### Step 5 — Setup on Her Phone (Protected)

1. Open SafeSignal
2. Tap **"💜 I need Protection"**
3. A **6-letter pair code** is auto-generated (e.g. `K7MX2P`)
4. Enter her own phone number
5. Enter **your** phone number as "Partner's Number"
6. Tap **Save & Activate**
7. **Share the pair code with you** (WhatsApp it)
8. Grant all permissions when asked:
   - Location (choose "Always")
   - Notifications
   - Display over other apps

---

### Step 6 — Setup on Your Phone (Guardian)

1. Open SafeSignal
2. Tap **"🛡️ I am the Guardian"**
3. Enter the pair code she shared
4. Enter your own phone number
5. Enter **her** phone number as "Partner's Number"
6. Tap **Save & Activate**
7. Grant: Call Phone permission, Notifications

---

### Step 7 — Test It

On her phone:
- Press power button **4 times quickly** (within ~2 seconds)
- Her screen will **blink white 4 times** (silent confirmation)

On your phone:
- 🚨 Full-screen red SOS alert appears
- 📢 Alarm rings at max volume
- 📞 Your phone auto-calls her after 3 seconds
- 📍 Her live GPS location appears — tap "Open Live Map"

---

## 🔧 How It Works

| What | How |
|------|-----|
| Power button detection | Android `ACTION_SCREEN_OFF` broadcast — counts 4 presses within 2.5 sec |
| Communication | Firebase Realtime Database (free, instant) |
| Location | GPS via FusedLocationProvider, updates every 30 sec |
| Location duration | Automatically stops after **24 hours** |
| Alarm | System alarm sound at max volume |
| Auto-call | `Intent.ACTION_CALL` on your phone → dials her number |
| Background | Foreground services (survive phone sleep) |
| After reboot | Services auto-restart via `BOOT_COMPLETED` receiver |

---

## 🔋 Battery Tips (Important!)

For reliable background operation:

**Her phone:**
- Settings → Apps → SafeSignal → Battery → **Unrestricted**
- Settings → Battery → **Don't optimize** SafeSignal

**Your phone (same steps)**

On some brands (Xiaomi/MIUI, Samsung, OnePlus):
- Enable **AutoStart** for SafeSignal in security settings
- Lock the app in recent apps so it doesn't get killed

---

## 📁 Project Structure

```
SafeSignal/
├── app/
│   ├── google-services.json          ← PUT YOUR FILE HERE
│   └── src/main/
│       ├── java/com/safesignal/
│       │   ├── MainActivity.kt           Role selection
│       │   ├── SetupActivity.kt          Pair code + phone numbers
│       │   ├── ProtectedHomeActivity.kt  Her home screen
│       │   ├── GuardianHomeActivity.kt   Your home screen
│       │   ├── SosAlertActivity.kt       Full-screen SOS on your phone
│       │   ├── SosBlinkActivity.kt       4 blinks on her phone
│       │   ├── BootReceiver.kt           Auto-restart after reboot
│       │   ├── SafeSignalApp.kt          App init
│       │   ├── service/
│       │   │   ├── PowerButtonService.kt     Her background listener
│       │   │   ├── GuardianListenerService.kt Your Firebase listener
│       │   │   └── LocationService.kt        GPS streaming
│       │   └── util/
│       │       ├── FirebaseHelper.kt         All Firebase logic
│       │       └── PrefManager.kt            Local storage
│       └── res/...
```

---

## ❓ Troubleshooting

**SOS not triggering:**
- Make sure PowerButtonService is running (check notification bar)
- Grant battery optimization exemption
- On MIUI: Enable AutoStart

**Your phone not ringing:**
- Ensure GuardianListenerService is running
- Check that pair codes match exactly
- Make sure both phones have internet

**Auto-call not working:**
- Grant CALL_PHONE permission manually: Settings → Apps → SafeSignal → Permissions → Phone

**Location not showing:**
- Grant Location "Always" permission
- Enable GPS on her phone

---

*Made with 💜 — Because she matters.*
