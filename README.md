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
- Notification-listener access, purchase parsing, automatic Google Wallet expense import, and local merchant-rule categorization
- Local monthly budget alerts at 70%, 80%, 90%, and 100% of confirmed spending
- Optional spending-pace alerts using the Dashboard calculation, limited to once per local day
- A home-screen budget widget showing percentage used and remaining or over-budget amount; its wider layout also shows spent versus limit

## Planned capabilities

The following are future plans and are **not implemented yet**:

- Spending analytics

The app stores transactions locally through Room. The Transactions screen shows confirmed history; tap a row to edit or delete it, or use Add transaction to create one. Changes appear in the list automatically. The optional label on the form is stored as `merchant`, while `description` remains null for manually created transactions. Editing retains the original transaction ID, source, import fields, notes, description, and creation time.

Settings → Categories shows active and inactive categories. Users can add and rename custom categories, and deactivate or reactivate any category. Built-in category names remain fixed; their built-in status is retained when their active state changes. Inactive categories stay on historical transactions but are omitted from new category choices. Categories cannot be permanently deleted. Category-specific budgets are not implemented.

Settings → Monthly spending limit lets users set or update the current calendar month's EUR limit. The Dashboard compares it with current-month expenses, shows remaining budget and a daily allowance, and previews recent transactions. It also compares actual spending with the expected amount through today, using the larger of 5% of expected spending or €5 as the on-pace tolerance. Income and transfers do not consume the spending limit. Settings → Budget notifications lets users enable or disable local alerts and grant Android notification permission when required. Confirmed current-month expenses can trigger alerts at 70%, 80%, 90%, and 100%. Each threshold is delivered at most once per calendar month; if one change crosses several, Spendly marks all newly crossed thresholds delivered and posts only the highest alert. Users can independently opt into spending-pace alerts from the same screen. Pace alerts use the Dashboard calculation and are limited to once per local calendar day. When one change qualifies for both, the threshold alert takes priority and suppresses the pace alert for that day. Alert settings and delivery state survive app restarts.

Settings > Google Wallet tracking shows whether Android notification-listener access is enabled and opens the system settings where the user can grant or revoke it. The listener accepts notifications from `com.google.android.apps.walletnfcrel`, extracts available text fields, and ignores other packages. A parser based on an observed NFC purchase notification (`GPK MARKET IKE` / `€2.95 with Ticket Restaurant® ••5311`) recognizes a leading EUR amount and merchant. Successfully parsed purchases are inserted automatically as confirmed expenses; they appear in transaction history and count toward Dashboard spending. Before insertion, existing merchant-category rules can assign an active category to confirmed or reviewable Wallet purchases. The `merchantPattern` field is treated as exact merchant text after trimming, root-locale lowercasing, and collapsing repeated whitespace. Imports stay uncategorized when no valid rule matches. While editing or confirming a Wallet purchase with a merchant and active category, users can choose to remember the merchant. The rule is created or the existing normalized merchant rule is updated. Leaving the option unchecked leaves any existing rule unchanged. Settings > Merchant rules lists rules and lets users change their category or delete them after confirmation. Rule changes affect future Wallet imports only; historical transactions stay as they were. Purchase-like notifications with a valid amount but uncertain details appear in Review. Users can edit and confirm them, or ignore them after confirmation. Only confirmed transactions appear in normal history, Dashboard recent transactions, and budget/spending-pace calculations. Ignored Wallet imports remain stored for traceability but do not count as spending. Non-purchase notifications and results without a valid amount are not stored. Wallet imports check for an existing Wallet transaction with the same notification reference before insertion. When no reference matches, a conservative fallback compares exact amount and currency, normalized merchant, and timestamps within two minutes. This reduces duplicate imports but may still allow duplicates when identifying details are missing. Debug builds log capture, parse, and import outcomes under `SpendlyWalletCapture`; release builds do not log purchase details.

The Spendly home-screen budget widget uses Jetpack Glance. Its small layout highlights the percentage used and remaining or over-budget amount; the wider layout adds current-month spent versus limit and a progress indicator. It uses the same Dashboard budget calculation and therefore counts only confirmed current-month expenses. With no current-month budget, it invites the user to open Spendly. Tapping the widget opens Dashboard. Android refreshes it when added, resized, or requested by the widget host, with a conservative daily scheduled update; app process startup also requests a refresh. The month is recalculated on each update. Transaction and budget edits do not yet trigger an immediate widget refresh, so figures may lag until the next host, daily, or app-start update.

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
