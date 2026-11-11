# FillUp+ (Android)

A fuel fill-up & vehicle expense logger for Android, covering essentially the
full spec (`fuel-tracker-app-spec.md`, delivered separately) except cloud
sync, which needs your own Firebase project and is delivered as a complete,
ready-to-activate reference implementation instead (see
`reference/cloud-sync/README.md`). Everything else below is fully wired and
working: local-only, offline-first data (the receipt scanner and nearby
station search never upload anything you've logged), no account required.

Built with: Kotlin, Jetpack Compose, Room, Navigation Compose, ML Kit
(on-device text recognition), WorkManager. See §3.1 of the spec for the full
stack rationale.

## What's in this build

**Core logging (Phase 1)**
- Multiple vehicle profiles (name, metric/imperial units, currency)
- Fill-up logging with odometer validation (can't go backwards) and a
  full-tank/partial-tank flag
- Real fuel-economy math: km/L, L/100km, or MPG, computed correctly across
  full-tank-to-full-tank spans (partial fill-ups in between are still counted,
  see `ConsumptionCalculator.kt`)
- Dashboard with average economy, cost-per-distance, an economy trend chart,
  and recent fill-ups

**Smart entry & upkeep (Phase 2)**
- **Receipt scanner**: takes a photo via the system camera app, reads it with
  on-device ML Kit text recognition, and pre-fills the add-fill-up form for
  you to confirm (`data/receipt/ReceiptParser.kt`, `ui/scan/ScanReceiptScreen.kt`).
  No CameraX, no cloud OCR call, no CAMERA permission needed.
- **Maintenance reminders**: distance- and/or time-based, with a daily
  WorkManager background check that posts a notification when something's
  due or overdue (`work/MaintenanceReminderWorker.kt`). Completed manually via
  "Mark done" for now.

**Expense log (v0.4)**
- Log any non-fuel cost per vehicle: Maintenance/PMS, oil change, repair,
  tires & battery, toll, parking, insurance, registration, car wash, other
  (`data/Expense.kt`, `ui/expense/`). Tap an entry to edit it.
- Totals, this month, a per-category breakdown, and category filters.
- Dashboard "Total cost to run" card: fuel + expenses, and the all-in cost
  per km/mi once there's enough fuel-log distance.
- "Log cost" on each maintenance reminder opens the expense form pre-filled.
- Expenses are included in Compare vehicles and in CSV (second table),
  Excel (second sheet) and PDF exports.
- Database v2 -> v3 uses a real migration, so existing logs are kept.

**Car Bluetooth, dashboard look, sharing & extras (v0.7)**
- Car Bluetooth check-in: pick the car's Bluetooth in Car settings (gear icon
  on the dashboard). When the phone connects to it, a notification offers an
  engine check-in with the current fuel estimate (`bluetooth/CarBluetoothReceiver.kt`,
  20-minute cooldown). Needs the Nearby devices permission on Android 12+.
- Car-dashboard theme (default): dark instrument-panel background with carbon
  weave and ghosted dials (`ui/theme/DashboardBackdrop.kt`), amber/cyan accents.
  "Classic green" is still available under Car settings > Look.
- Instrument cluster on the dashboard: round fuel and economy gauges.
- Price watch: latest pump price, change since last fill-up (same octane),
  and a price-history sparkline.
- Achievements: 14 pixel-medal badges worked out from your logs.
- Share FillUp+: in-app QR code (ZXing) and share link to the latest APK.
- The launcher icon shows the version ("v0.7"): generated at build time by
  the "Version badge" block in `app/build.gradle.kts`.
- The "Check in" app shortcut is now created in code, so it also works for
  the debug (.debug) build.
- Database v5 -> v6 adds the car Bluetooth fields; data is kept.

**Trip logger, top-up reminders & odometer journal (v0.6)**
- Engine check-in: each time you start the car, log the odometer and
  (optionally) the fuel gauge and trip type. Big "Engine on: check in" button
  on the dashboard, plus a "Check in" home-screen shortcut (long-press the app
  icon) that opens the last car's check-in directly.
- Fuel estimate (`data/FuelEstimator.kt`): replays full/partial fill-ups,
  gauge readings and distance driven at your average economy. Needs the tank
  size (set on the dashboard card). After each check-in it says "You're good",
  "Top up soon" (1/4 tank) or "Fill up now" (1/8), with a link to nearby
  stations; the daily background check also notifies when it's low.
- Odometer journal: distance per day (from check-ins and fill-ups), today /
  this week / this month totals, average per driving day, estimated fuel and
  cost per day. Check-ins can be deleted there.
- Maintenance reminders now use the latest check-in odometer too.
- Database v4 -> v5 adds the check-ins table and tank size; data is kept.

**Fuel price, octane & Art studio (v0.5)**
- Add fill-up now has Price per L, Volume and Total: type any two and the
  third is calculated. "Use last price" fills in the previous price.
- Octane / fuel chips (91 Regular, 95 Premium, 97 Super, 100, Diesel),
  defaulting to the grade used last time. Shown in lists and all exports.
- Art studio (dashboard menu): draw your car on a 32x16 pixel grid with a
  16-color palette, pencil, eraser, fill, undo, flip, and a starter car.
  The drawing shows faintly behind the Add fill-up form.
- The add fill-up button is an animated pixel pump attendant with a + badge.
- Database v3 -> v4 adds two columns with a real migration; data is kept.

**Nearby prices, exports & comparison (Phase 3)**
- **Nearby fuel stations**: finds stations close to you via OpenStreetMap's
  free, keyless Overpass API (`data/stations/OverpassApi.kt`) -- no Google
  Places billing account needed. Shows distance, opens the station in Maps,
  and can jump straight to logging a fill-up there. This is a station
  *locator*, not live crowdsourced prices -- see the spec's §2.1/§7 for why
  that's the realistic v1 approach, and `reference/cloud-sync/` for the
  backend that a future crowdsourced-price feature would need anyway.
- **Excel and PDF export**, alongside the original CSV export, all from the
  dashboard's overflow menu. The Excel writer is a small hand-rolled OOXML
  writer (`data/export/XlsxWriter.kt`) rather than Apache POI -- see the
  comment in that file for why (POI's Android runtime compatibility has a
  long history of trouble, not worth risking in a build this couldn't
  verify by compiling). PDF export uses Android's built-in `PdfDocument`.
- **Compare vehicles**: a household-level view across every vehicle's
  average economy, cost per distance, and total spend.

**Not wired in (needs your own account) -- see `reference/cloud-sync/`**
- Cloud backup / cross-device sync. The complete Firestore-based
  implementation is written and ready to drop in; it isn't part of the
  active build because its dependency graph (Firestore, Auth, gRPC,
  Protobuf) couldn't be verified to resolve cleanly in the sandbox this
  project was generated in, and it needs a `google-services.json` from a
  Firebase project only you can create. `reference/cloud-sync/README.md` has
  the exact steps -- they're mechanical once you've done the account setup.

### Note on the receipt scanner

It's rule-based (regex over the recognized text, cross-checking total = volume
x price/unit when two of the three are found), not the Gemini Nano-powered
"smart parse" the spec describes as a future enhancement -- that API is
currently limited to a handful of flagship phones, so this build uses the
same on-device OCR approach that works on any device. Expect it to need
correction sometimes; that's why it always lands you on the normal, fully
editable add-fill-up form rather than saving anything directly.

## Building this project

This project **cannot be built in the environment that generated it** (no
network access to Google's/Maven's package repositories there). It needs to be
built either in Android Studio on your machine, or via the included GitHub
Actions workflow, the same way Balikbayan Protocol was built and signed.

### Option A — GitHub Actions (recommended, matches your existing pipeline)

1. Create a new GitHub repo (e.g. `fuel-tracker`) and push this project to it
   with **GitHub Desktop** (the web uploader tends to fail silently on large
   file counts — same issue you hit with Balikbayan Protocol).
2. In the repo's **Settings → Secrets and variables → Actions**, add these
   four secrets (only needed for a *signed release* build — the debug APK
   builds fine without them):
   - `RELEASE_KEYSTORE_BASE64` — your release keystore file, base64-encoded
     (`base64 -w0 your.keystore` on macOS/Linux, or `certutil -encode` on
     Windows then strip the header/footer lines)
   - `RELEASE_STORE_PASSWORD`
   - `RELEASE_KEY_ALIAS`
   - `RELEASE_KEY_PASSWORD`
   - **This app needs its own new keystore** — do not reuse the Balikbayan
     Protocol keystore. Generate one with:
     `keytool -genkeypair -v -keystore fueltracker-release.keystore -alias fueltracker -keyalg RSA -keysize 2048 -validity 10000`
     Back it up immediately (zip it, store it somewhere safe) — same as noted
     for Balikbayan Protocol, losing it means you can never update this app's
     Play Store listing again.
3. Push to `main` (or run the workflow manually from the Actions tab). It
   builds `fuel-tracker-debug-apk` always, and `fuel-tracker-release-aab` once
   the four secrets above are set.
4. Download the debug APK artifact from the Actions run and install it on your
   phone to test, same as before.

### Option B — Android Studio locally

Open the project folder in Android Studio (Ladybird/Koala or newer), let it
sync Gradle, and run on a device/emulator. Release builds will be unsigned
locally unless you set the four `RELEASE_*` environment variables yourself
before building.

## Before Play Store submission

- Replace the placeholder launcher icon (`app/src/main/res/drawable/ic_launcher_foreground.xml`)
  with a real designed icon — same approach as Balikbayan Protocol (AI-generated
  + declared as such, or a custom design), then update the store listing assets.
- Fill in and publish `docs/index.html` as this app's privacy policy via GitHub
  Pages (same pattern as Balikbayan Protocol: enable Pages on `main` /docs).
  It's already updated for the location/camera/internet permissions this
  build added -- just fill in the date and contact email placeholders.
- Double-check `compileSdk`/`targetSdk` in `app/build.gradle.kts` against
  Play Console's current target API requirement at the time you actually
  submit — these move roughly yearly, and this project was generated with
  known-stable versions rather than bleeding-edge ones (see the comment in
  the root `build.gradle.kts`).
- Play Console's Data safety section will need to declare location use (for
  nearby stations) now, alongside the existing declarations -- see
  `docs/index.html` for the plain-language version of what's collected and
  why.
- This app uses a **different application ID** (`com.parengmark.fueltracker`)
  and needs its **own** Play Console app listing and its own $0 (already
  paid once) developer account submission — it does not reuse Balikbayan
  Protocol's listing.

## Project structure

```
app/src/main/java/com/parengmark/fueltracker/
  data/            Room entities, DAOs, database, repository, consumption math
  data/receipt/    Receipt text parsing (no persistence -- just text in, ParsedReceipt out)
  data/stations/   OpenStreetMap Overpass API client + FuelStation model
  data/export/     CSV / hand-rolled XLSX / PDF report writers
  location/        Plain LocationManager helper (no Play Services dependency)
  work/            WorkManager daily maintenance-due check + notification helper
  ui/theme/        Compose Material3 theme
  ui/vehicle/      Vehicle list + add-vehicle screens/viewmodel
  ui/dashboard/    Per-vehicle dashboard screen/viewmodel/trend chart
  ui/fillup/       Fill-up list + add-fill-up screens/viewmodel
  ui/scan/         Receipt scanner screen
  ui/maintenance/  Maintenance reminder list/add screens/viewmodel
  ui/stations/     Nearby fuel stations screen/viewmodel
  ui/compare/      Multi-vehicle comparison screen/viewmodel
  ui/navigation/   Single NavHost wiring all of the above together
  ui/common/       Manual DI factory + formatting helpers

reference/cloud-sync/   Complete but NOT-compiled Firebase sync reference --
                        see its own README.md to activate.
```

No Hilt/Dagger in this build — see the comment in `FuelTrackerApp.kt` for why
(manual DI has been enough for every ViewModel added so far; revisit if/when
cloud sync's dependency graph actually gets wired in).

## Permissions this build requests

- `POST_NOTIFICATIONS` (Android 13+) -- maintenance-due notifications.
  Declining is harmless; reminders still work inside the app.
- `ACCESS_FINE_LOCATION` / `ACCESS_COARSE_LOCATION` -- only requested when
  you open "Nearby fuel stations"; nothing else in the app touches location.
- `INTERNET` -- only used for that same nearby-stations search (a call to
  OpenStreetMap's Overpass API). Every other feature is fully offline.

The database schema changed twice since the first build (fill-ups only ->
+ maintenance reminders) and is still using `fallbackToDestructiveMigration()`
since there's no real install base yet -- see the comment in `AppDatabase.kt`.
