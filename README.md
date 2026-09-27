# Spendly

Spendly is a local-first Android personal expense tracker. The project currently has an application shell with five navigable placeholder screens, automatic light/dark theming, a core finance domain model, and Room-based local persistence. Transaction entry and automatic Google Wallet capture are not implemented yet.

## Technology

- Kotlin, Jetpack Compose, and Material 3
- Navigation Compose with a single activity
- Gradle Kotlin DSL and a version catalog
- A feature-oriented UI structure ready for state-driven screens and coroutines
- Plain Kotlin domain models for money, transactions, categories, and monthly budgets
- Room database and repository implementations for local storage

## Planned capabilities

The following are future plans and are **not implemented yet**:

- Manual expense tracking
- Google Wallet notification import
- Monthly spending limits
- Spending alerts
- Spending analytics
- A home-screen budget widget

The app stores domain records locally through Room. The current UI does not create or display those records.

## Build

Install JDK 17 and Android SDK Platform 36. Set `ANDROID_HOME` or configure the SDK in Android Studio, then run:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
```

Open the project in Android Studio and run the `app` configuration to view the Dashboard and bottom navigation.

On Windows, Gradle's JVM test runner may fail to load test classes when the project path contains non-ASCII characters. Run the same project through a temporary ASCII drive alias in that case:

```powershell
$projectRoot = (Get-Location).Path
subst S: $projectRoot
Push-Location S:\
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest
Pop-Location
subst S: /D
```
