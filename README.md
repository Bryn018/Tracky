# Tracky - Offline SMS Transaction Tracker

> **Your financial data, on your device, under your control.**

Tracky is a 100% offline Android application that automatically tracks mobile money transactions by reading SMS messages. It parses bank and mobile money SMS (M-Pesa, Airtel Money, etc.) and provides a clean dashboard to visualize your spending, income, and financial habits — all without an internet connection.

---

## Features

- 📱 **Automatic SMS Detection** — Real-time monitoring of incoming SMS for transaction data
- 💰 **Multi-Channel Support** — Works with M-Pesa, Airtel Money, and other mobile money services
- 📊 **Dashboard Overview** — See your balance, recent spending, and income at a glance
- 📈 **Analytics & Insights** — Spending habits, income overview, top contacts, most used channels
- 📋 **Transaction History** — Search, filter by type/channel, and browse all transactions
- 📅 **Daily Report Notifications** — Optional scheduled notification summarizing your daily spending
- 🌙 **Dark Mode** — Follow system theme or manually toggle
- 🔒 **100% Offline** — No internet permission, no data leaves your device
- 📦 **No Account Required** — No sign-up, no cloud sync, no tracking

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | **Kotlin** |
| UI | **Jetpack Compose** (Material 3) |
| DI | **Hilt** (Dagger) |
| Database | **Room** (SQLite) |
| Async | **Kotlin Coroutines & Flow** |
| Background | **WorkManager**, **AlarmManager** |
| Navigation | **Navigation Compose** |
| DataStore | **DataStore Preferences** |
| Build | **Gradle KTS** + **KSP** |
| Min SDK | **26** (Android 8.0) |
| Target SDK | **35** (Android 15) |

---

## Screenshots

<!-- Screenshots will be added once the UI is finalized -->

| Home Screen | Analytics | Transactions | Settings |
|:-----------:|:---------:|:------------:|:--------:|
| *(coming soon)* | *(coming soon)* | *(coming soon)* | *(coming soon)* |

---

## Architecture

Tracky follows a **clean architecture** approach with three layers:

```
┌──────────────────────────────────────────────────────────────┐
│                      PRESENTATION LAYER                      │
│  ┌──────────┐  ┌──────────┐  ┌──────────┐  ┌──────────┐   │
│  │  Home    │  │Analytics │  │Transact. │  │ Settings │   │
│  │  Screen  │  │  Screen  │  │  Screen  │  │  Screen  │   │
│  └────┬─────┘  └────┬─────┘  └────┬─────┘  └────┬─────┘   │
│       │              │             │              │         │
│  ┌────▼──────────────▼─────────────▼──────────────▼─────┐   │
│  │                ViewModels (Hilt-Injected)             │   │
│  └────────────────────────┬─────────────────────────────┘   │
├───────────────────────────┼─────────────────────────────────┤
│                    DOMAIN LAYER                              │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │               Use Cases / Domain Models               │   │
│  └────────────────────────┬─────────────────────────────┘   │
├───────────────────────────┼─────────────────────────────────┤
│                     DATA LAYER                               │
│  ┌────────────────────────▼─────────────────────────────┐   │
│  │                  Repositories                          │   │
│  └────┬──────────────────────────────────┬──────────────┘   │
│       │                                  │                  │
│  ┌────▼──────┐                    ┌──────▼────────────┐     │
│  │  Room DB  │                    │  SMS Reader       │     │
│  │ (SQLite)  │                    │  + SmsParser      │     │
│  └───────────┘                    └───────────────────┘     │
│                                                             │
│  ┌──────────────────────────────────────────────────────┐   │
│  │               Background Workers                      │   │
│  │  ┌──────────────────┐  ┌──────────────────────────┐  │   │
│  │  │ DailyReportWorker │  │  SmsReceiver (Broadcast) │  │   │
│  │  │  (WorkManager)   │  │  (Real-time SMS)         │  │   │
│  │  └──────────────────┘  └──────────────────────────┘  │   │
│  └──────────────────────────────────────────────────────┘   │
└──────────────────────────────────────────────────────────────┘
```

**Data flow:**
1. SMS arrives → `SmsReceiver` (BroadcastReceiver) intercepts it
2. `SmsParser` extracts transaction details (amount, type, channel, etc.)
3. Parsed transaction is saved to Room DB via `TransactionRepository`
4. UI observes Room DB via Kotlin Flow → auto-updates all screens
5. Daily report: `AlarmManager` triggers `ReportAlarmReceiver` → enqueues `DailyReportWorker` → reads today's summary → shows notification

---

## Installation / Build

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 35 (compileSdk)
- Gradle 8.9 (wrapper included)

### Steps

```bash
# Clone the repository
git clone https://github.com/waly/tracky.git
cd tracky

# Build the app
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug

# Build release APK (requires signing config)
./gradlew assembleRelease
```

For a release build, create `keystore.properties` in the project root with:

```properties
storePassword=your_store_password
keyPassword=your_key_password
keyAlias=your_key_alias
storeFile=path/to/keystore.jks
```

---

## Permissions

Tracky requires the following permissions:

| Permission | Purpose | Required? |
|-----------|---------|-----------|
| `RECEIVE_SMS` | Detect incoming SMS transactions in real-time | Yes |
| `READ_SMS` | Optional: scan existing SMS history on first launch | Optional |
| `POST_NOTIFICATIONS` | Show daily report and SMS monitoring notifications | Runtime (Android 13+) / Auto (8-12) |
| `SCHEDULE_EXACT_ALARM` | Schedule precise daily report notifications | Yes |
| `USE_EXACT_ALARM` | Alternative for exact alarm scheduling | Yes |

> **Note:** On Android 14+, runtime permission is required for `RECEIVE_SMS`. Tracky will request this permission on first launch.

---

## Privacy & Security

**Tracky is designed with privacy as a core principle.**

- 🔌 **No Internet Permission** — The app manifest explicitly does not include `INTERNET` permission
- 📁 **All Data Stays Local** — Transactions are stored only in the local Room database (SQLite)
- 📡 **No Network Calls** — The app never sends or receives data over the network
- 👤 **No Accounts** — No sign-up, no user profiles, no telemetry
- 🔒 **No Third-Party Analytics** — No Firebase, no Crashlytics, no tracking SDKs
- 🗑️ **Full Control** — Clear all data or uninstall to permanently remove all information

Your financial data is yours. Tracky simply helps you visualize it — nothing more.

---

## Developer Info

- **Author:** Waly
- **License:** MIT (see [LICENSE](LICENSE))
- **GitHub:** [github.com/waly/tracky](https://github.com/waly/tracky)

### Contributing

Contributions are welcome! Please follow these steps:

1. Fork the repository
2. Create a feature branch (`git checkout -b feature/amazing-feature`)
3. Commit your changes (`git commit -m 'Add amazing feature'`)
4. Push to the branch (`git push origin feature/amazing-feature`)
5. Open a Pull Request

### Building for Contributors

```bash
# Run lint checks
./gradlew lint

# Run unit tests (when available)
./gradlew test

# Build all variants
./gradlew assemble
```

---

*Tracky — Know your money. Keep it private.*
