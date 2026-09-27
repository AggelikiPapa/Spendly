# Spendly

Spendly is a local-first Android personal expense tracker. The project has Room-based local persistence, manual transaction management, category management, and current-month spending-limit configuration. Automatic Google Wallet capture is planned.

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
- Settings-based category management with custom creation, custom renaming, and activation controls
- Three primary bottom destinations: Dashboard, Transactions, and Analytics; Settings is available from Dashboard and Review from Transactions
- Current-month EUR spending-limit configuration through Settings

## Planned capabilities

The following are future plans and are **not implemented yet**:

- Google Wallet notification import
- Dashboard budget progress and spending calculations
- Spending alerts
- Spending analytics
- A home-screen budget widget

The app stores transactions locally through Room. The Transactions screen shows current history; tap a row to edit or delete it, or use Add transaction to create one. Changes appear in the list automatically. The optional label on the form is stored as `merchant`, while `description` remains null for manually created transactions. Editing retains the original transaction ID, source, import fields, notes, description, and creation time.

Settings → Categories shows active and inactive categories. Users can add and rename custom categories, and deactivate or reactivate any category. Built-in category names remain fixed; their built-in status is retained when their active state changes. Inactive categories stay on historical transactions but are omitted from new category choices. Categories cannot be permanently deleted. Category-specific budgets are not implemented.

Settings → Monthly spending limit lets users set or update the current calendar month's EUR limit. The Dashboard does not yet calculate spending against that limit.

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
