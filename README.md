# Crypto Price Tracker

A modern Android app for tracking cryptocurrency prices, built with Kotlin and Jetpack Compose.

## Features

- **Live Market Data** — Top 100 coins by market cap with real-time prices from CoinGecko's free API
- **Color-Coded Changes** — Green for positive and red for negative 24h price changes
- **Search** — Find any cryptocurrency by name or symbol with debounced search
- **Favorites** — Star coins to save them locally (persisted with Room database)
- **Price Alerts** — Set target prices and receive local notifications when triggered (WorkManager checks every 15 minutes)
- **Historical Charts** — Interactive 7-day, 30-day, and 1-year price charts with touch selection
- **Dark Theme** — Full Material 3 dark theme support with dynamic colors on Android 12+
- **Auto-Refresh** — Fresh data loads every time the app is opened

## Tech Stack

| Layer | Library |
|-------|---------|
| UI | Jetpack Compose, Material 3 |
| Navigation | Compose Navigation |
| Networking | Retrofit 2 + OkHttp |
| Local DB | Room |
| DI | Hilt |
| Background | WorkManager |
| Images | Coil |

## Project Structure

```
app/src/main/java/com/cryptotracker/
├── CryptoApp.kt              # Application class (Hilt, notifications, WorkManager setup)
├── MainActivity.kt            # Entry point
├── data/
│   ├── local/                 # Room database, entities, DAOs
│   └── remote/                # Retrofit API interface and DTOs
├── repository/                # Single source of truth for data
├── di/                        # Hilt dependency injection modules
├── domain/model/              # Domain models (Coin, ChartData)
├── ui/
│   ├── theme/                 # Material 3 theme, colors, typography
│   ├── components/            # Reusable composables (CoinListItem, charts)
│   ├── navigation/            # Navigation graph with bottom nav
│   ├── home/                  # Home screen (top coins list)
│   ├── search/                # Search screen
│   ├── detail/                # Coin detail with chart and stats
│   └── favorites/             # Favorited coins screen
└── worker/                    # WorkManager price alert checker
```

## How to Build and Run

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or newer
- JDK 17
- Android SDK 34
- An Android device or emulator running API 26+

### Steps

1. **Open the project** — Open Android Studio and select `File > Open`, then navigate to this `CryptoPriceTracker` folder.

2. **Sync Gradle** — Android Studio should automatically start syncing. If not, click `File > Sync Project with Gradle Files`.

3. **Run** — Select a device/emulator and click the Run button (or `Shift+F10`).

No API key is required. The app uses CoinGecko's free public API.

### Note on API Rate Limits

CoinGecko's free API has rate limits (~10-30 calls/minute). If you see errors, wait a moment and refresh. The app handles this gracefully with error states and retry.

## Permissions

- `INTERNET` — Required for API calls
- `POST_NOTIFICATIONS` — Required on Android 13+ for price alert notifications (requested at runtime)

## Architecture

The app follows MVVM architecture with a repository pattern:

- **ViewModels** expose UI state as `StateFlow` consumed by Compose screens
- **Repository** abstracts remote API and local database access
- **Room** persists favorites and price alerts locally
- **Hilt** provides dependency injection throughout the app
- **WorkManager** runs periodic background checks for price alerts
