# Spendly

Spendly is a local-first Android personal expense tracker. The project has Room-based local persistence, manual transaction management, category management, and a current-month budget dashboard. It can detect Google Wallet notifications, parse purchase notifications, and automatically import successful purchases as expense transactions.

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
- Dashboard showing the monthly limit, expense spending, remaining budget, percentage used, daily allowance, and recent transactions
- Spending pace showing expected spending by today, above/on/below-plan status, and the signed difference from plan
- Notification-listener access, purchase parsing, and automatic Google Wallet expense import

## Planned capabilities

The following are future plans and are **not implemented yet**:

- Google Wallet review workflow
- Spending alerts
- Spending analytics
- A home-screen budget widget

The app stores transactions locally through Room. The Transactions screen shows current history; tap a row to edit or delete it, or use Add transaction to create one. Changes appear in the list automatically. The optional label on the form is stored as `merchant`, while `description` remains null for manually created transactions. Editing retains the original transaction ID, source, import fields, notes, description, and creation time.

Settings → Categories shows active and inactive categories. Users can add and rename custom categories, and deactivate or reactivate any category. Built-in category names remain fixed; their built-in status is retained when their active state changes. Inactive categories stay on historical transactions but are omitted from new category choices. Categories cannot be permanently deleted. Category-specific budgets are not implemented.

Settings → Monthly spending limit lets users set or update the current calendar month's EUR limit. The Dashboard compares it with current-month expenses, shows remaining budget and a daily allowance, and previews recent transactions. It also compares actual spending with the expected amount through today, using the larger of 5% of expected spending or €5 as the on-pace tolerance. Income and transfers do not consume the spending limit. Notification-based budget warnings are not implemented.

Settings > Google Wallet tracking shows whether Android notification-listener access is enabled and opens the system settings where the user can grant or revoke it. The listener accepts notifications from `com.google.android.apps.walletnfcrel`, extracts available text fields, and ignores other packages. A parser based on an observed NFC purchase notification (`GPK MARKET IKE` / `€2.95 with Ticket Restaurant® ••5311`) recognizes a leading EUR amount and merchant. Successfully parsed purchases are inserted automatically as confirmed expenses without a category; they appear in transaction history and count toward Dashboard spending. Purchase-like notifications with a valid amount but uncertain details are stored as needing review for a later workflow. Non-purchase notifications and results without a valid amount are not stored. Wallet imports now check for an existing Wallet transaction with the same notification reference before insertion. When no reference matches, a conservative fallback compares exact amount and currency, merchant after trimming, root-locale lowercasing, and whitespace collapsing, and timestamps within two minutes. This reduces duplicate imports but may still allow duplicates when identifying details are missing. Debug builds log capture, parse, and import outcomes under `SpendlyWalletCapture`; release builds do not log purchase details.

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
