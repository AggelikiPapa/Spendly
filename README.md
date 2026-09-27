# Spendly

Spendly is a local-first Android personal expense tracker. The project has an application shell, a core finance domain model, Room-based local persistence, and manual transaction creation, history, editing, and deletion. Automatic Google Wallet capture is planned.

## Technology

- Kotlin, Jetpack Compose, and Material 3
- Navigation Compose with a single activity
- Gradle Kotlin DSL and a version catalog
- A feature-oriented UI structure ready for state-driven screens and coroutines
- Plain Kotlin domain models for money, transactions, categories, and monthly budgets
- Room database and repository implementations for local storage
- State-driven transaction entry and editing with shared validation and decimal-safe amount conversion
- Reactive transaction history with category names and newest-first ordering
- Transaction deletion with confirmation

## Planned capabilities

The following are future plans and are **not implemented yet**:

- Google Wallet notification import
- Monthly spending limits
- Spending alerts
- Spending analytics
- A home-screen budget widget

The app stores transactions locally through Room. The Transactions screen shows current history; tap a row to edit or delete it, or use Add transaction to create one. Changes appear in the list automatically. The optional label on the form is stored as `merchant`, while `description` remains null for manually created transactions. Editing retains the original transaction ID, source, import fields, notes, description, and creation time.

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
