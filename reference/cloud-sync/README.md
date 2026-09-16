# Activating cloud sync (Phase 3 of the spec)

This folder is **not compiled** into the app -- it's outside `app/src`, so
Gradle never touches it. It's a complete, ready-to-adapt reference for the
one Phase 3 feature that genuinely can't be finished inside the sandbox that
generated this project: cloud backup and cross-device sync needs a real
Firebase (or Supabase) project that only you can create, and its dependency
graph (Firestore, Auth, gRPC, Protobuf, Play Services) is exactly the kind of
thing that can compile cleanly one day and hit a version conflict the next --
not something to bolt onto a working build without being able to run
`./gradlew build` and see what happens. Everything else in this app (Phases
1 and 2, plus the rest of Phase 3 -- exports, nearby stations, multi-vehicle
comparison) ships fully wired and working; this is the one piece that's
"designed and written, activate when ready" rather than "already on."

## Why Firebase (of the two options the spec considered)

The spec's §2.5 named Firebase or Supabase. This reference uses Firebase
because it needs zero backend of your own to host -- Firestore + Auth are
enough, and that's the fastest path to a working sync layer for a personal
app like this one. Supabase remains a fine alternative if you'd rather have
SQL access to your own data later.

## What's in this folder

- `SyncMetadata.md` -- the three columns to add to each Room entity
  (`Vehicle`, `FillUp`, `MaintenanceReminder`) and the DB migration notes.
- `FirebaseSyncRepository.kt.txt` -- a full push/pull sync implementation
  against Firestore, following the local-first pattern from the spec's §3.2
  (Room stays the only thing the UI reads from; sync runs underneath it).
- `SyncWorker.kt.txt` -- a WorkManager worker that runs the sync repository
  periodically and after connectivity returns.

The `.kt.txt` extension is deliberate -- it stops these from being picked up
as real source files if this folder is ever copied somewhere inside `app/src`
by accident. Rename to `.kt` as the last step below, not the first.

## Activation steps

1. **Create a Firebase project** at https://console.firebase.google.com,
   add an Android app to it with package name `com.parengmark.fueltracker`
   (or your renamed application ID), and download the generated
   `google-services.json` into this project's `app/` folder.

2. **Add the Google Services Gradle plugin.** In the root `build.gradle.kts`:
   ```kotlin
   plugins {
       // ...existing plugins...
       id("com.google.gms.google-services") version "4.4.2" apply false
   }
   ```
   And in `app/build.gradle.kts`, add to the `plugins { }` block:
   ```kotlin
   id("com.google.gms.google-services")
   ```
   This plugin **fails the build immediately if `google-services.json` is
   missing** -- that's exactly why it isn't applied yet in the delivered
   project. Only add it after step 1's file is actually in place.

3. **Add the Firebase dependencies** to `app/build.gradle.kts`:
   ```kotlin
   implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
   implementation("com.google.firebase:firebase-firestore-ktx")
   implementation("com.google.firebase:firebase-auth-ktx")
   implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.9.0")
   ```
   Then **sync Gradle in Android Studio and confirm it resolves cleanly**
   before writing any code that depends on it -- Android Studio will surface
   any dependency conflict immediately, which is the fast, low-risk way to
   verify this (much faster than a blind edit-and-hope cycle).

4. **Add the three sync columns** to `Vehicle`, `FillUp`, and
   `MaintenanceReminder` per `SyncMetadata.md`, bump `AppDatabase`'s version
   number by one (it's currently on a `fallbackToDestructiveMigration()`
   pre-launch, so no real migration is needed yet either).

5. **Copy in `FirebaseSyncRepository.kt.txt`** as
   `app/src/main/java/com/parengmark/fueltracker/data/sync/FirebaseSyncRepository.kt`
   (rename to `.kt`), and **`SyncWorker.kt.txt`** as
   `app/src/main/java/com/parengmark/fueltracker/work/SyncWorker.kt`.

6. **Add sign-in.** The simplest option for a personal app is Firebase
   Auth's anonymous sign-in (no login screen needed at all -- each install
   gets a stable anonymous account that can later be upgraded to a real
   email/Google sign-in if you want cross-device sync to survive a
   reinstall). Call `Firebase.auth.signInAnonymously()` once in
   `FuelTrackerApp.onCreate()`, guarded by `if (Firebase.auth.currentUser ==
   null)`.

7. **Schedule the sync worker** alongside the existing maintenance-check
   worker in `FuelTrackerApp.scheduleDailyMaintenanceCheck()` -- same
   `WorkManager.enqueueUniquePeriodicWork` pattern, shorter interval (e.g.
   every 15 minutes while the constraints allow network), plus triggering an
   immediate one-off `OneTimeWorkRequest` after every local save so changes
   don't wait for the next periodic run.

8. **Update the privacy policy** (`docs/index.html`) -- once this is active,
   the app is no longer local-only. Say plainly what's stored in Firebase
   (the user's fuel/vehicle/maintenance data, tied to their anonymous or
   signed-in account id) and link Google's Firebase privacy terms.

Steps 1-3 are the only ones that need your own accounts/credentials; 4-8 are
mechanical once those are in place.
