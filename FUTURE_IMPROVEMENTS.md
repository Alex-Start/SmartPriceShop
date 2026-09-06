FUTURE IMPROVEMENTS & DEVELOPER CHECKLIST

Purpose

This document captures concrete, prioritized improvements and engineering notes observed during the current migration and feature work. Use this as a living checklist for follow-up PRs, QA tests, and release notes.

High-priority items (must fix before stable release)

- Currency consistency everywhere
  - Ensure every UI label, chart axis, tooltip, log entry and export shows the currently selected currency symbol and converted values.
  - Store original currency per price record (e.g., PriceRecord.currencyCode) and always convert at render-time using CurrencyRates.
  - Convert numeric datasets used for graph plotting to the selected currency before generating axis scales or path points.
  - Update export/import routines to preserve original currencyCode and write converted display values only when explicitly requested.

- Room schema & migrations
  - Keep Room migration tests: add automated migration smoke tests for 2→3→4 to ensure identity hash matches across versions.
  - When adding non-null columns, provide safe defaults and register migrations in AppDatabase.addMigrations().

- Remaining hardcoded currency strings
  - Sweep repo for hardcoded "$" or "USD" strings and replace with formatting utilities (UnitPriceCalculator.formatCurrencyWithConversion or ViewModel.formatAmountForDisplay).
  - Update localization strings to use placeholders and format at runtime.

- Dual-image UX
  - Confirm Product has two image URIs: product image (how it looks) and price photo (AI scan). Ensure both stored and exported.
  - Product list and Product Detail sheet should show both thumbnails side-by-side with tap-to-open full-size viewer for each.

- Dark-mode contrast issues
  - Revisit sort/filter menus and any dropdowns that may use theme surface color for text. Use MaterialTheme.colorScheme.onSurface or dynamically invert text color when background is dark.
  - Add UI tests asserting contrast for sort menu items in dark theme.

- Gemini API key UX & AI scanner messaging
  - Do not display technical messages like "Auto-scanned (Set GEMINI_API_KEY in Secrets for live AI)" to end users. Replace with human-friendly guidance: "Auto-scanned (Enter API key in Settings → AI Scanner to use live model)".
  - Add a small settings dialog showing where to acquire a free key (link to https://aistudio.google.com/app/apikey) and explain free-tier limits.

Medium-priority items

- Shopping lists & estimate conversions
  - Ensure list totals compute using converted prices based on selected currency; preserve original purchase currency for historical records.

- Tests and CI
  - Add unit tests for CurrencyRates.convert() pivoting via USD and for formatCurrencyWithConversion().
  - Add UI screenshot or Compose tests for ProductDetail and PriceComparisonCard showing both thumbnails and correct currency symbol.

- Export/Import & Backup
  - Include image files in ZIP export and map image paths on import. Preserve original currencyCode in manifest.json.

Developer troubleshooting notes

- If encountering Room identity hash mismatch on startup:
  1. Increase DB version and add a migration that alters tables and adds default values for new columns.
  2. Do NOT uninstall the app during migration testing — that hides migration problems.

- If Kotlin compilation fails after model changes:
  1. Run ./gradlew :app:compileDebugKotlin --stacktrace locally to get exact compile errors.
  2. Update all call sites to match changed data class constructors and add default parameter values to ease migrations.

QA checklist (manual)

- [ ] Start the app without uninstalling previous build and confirm migration 3→4 runs without crash.
- [ ] Set selected currency to CZK and verify every screen (product list, detail, graph labels, AI scanner results, log window) shows Kč/converted values, not $.
- [ ] Open a product with both images and confirm thumbnails and full-size view work for each photo independently.
- [ ] Toggle dark mode and open sort filter — ensure items are readable (contrast passed).

Notes for future architecture

- Consider storing CurrencyRates in persistent DB table (instead of SharedPreferences) and syncing rates via optional remote endpoint.
- Consider centralizing currency formatting into a single UI composition/local (LocalAppCurrency) supplied at top-level so previews and non-composables have access to formatting functions.


Credit

This checklist was generated during the "Currency & UI Migration" checkpoint and should be kept in sync with issues and PRs created against the repository.
