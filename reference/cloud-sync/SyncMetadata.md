# Schema additions for sync

Add these three fields to `Vehicle`, `FillUp`, and `MaintenanceReminder`
(all in `app/src/main/java/com/parengmark/fueltracker/data/`):

```kotlin
val pendingSync: Boolean = true,       // true until successfully pushed to Firestore
val updatedAtEpochMillis: Long = System.currentTimeMillis(),
val deletedAt: Long? = null            // soft delete -- see below
```

## Why soft delete

A hard `DELETE` on one device has nothing to tell the other device to delete
too once sync is added -- the row is just gone locally, with no trace to sync.
Soft-deleting (setting `deletedAt` and leaving the row in place, `pendingSync
= true`) lets the delete itself propagate through the same push/pull path as
every other change, and only gets physically removed after both the local DB
and Firestore agree it's gone (a cleanup pass can purge anything with
`deletedAt` older than, say, 30 days).

Making this real means:

- Changing the three `@Delete` DAO methods (`VehicleDao.delete`,
  `FillUpDao.delete`, `MaintenanceReminderDao.delete`) to instead be `@Update`
  calls that set `deletedAt`/`pendingSync`, OR adding new
  `softDelete(id: Long)` DAO methods and switching the repository + ViewModel
  delete calls to use those instead.
- Adding `WHERE deletedAt IS NULL` to every `SELECT` query in `VehicleDao`,
  `FillUpDao`, and `MaintenanceReminderDao` so soft-deleted rows stop
  appearing in the UI immediately, the same as a hard delete would.

That query/DAO change touches working, already-tested code paths (every
list screen in the app reads through these DAOs), which is exactly why it's
left as a documented step here rather than made live in a build that
couldn't be compiled to verify it. Do this one with Android Studio open so
you get immediate feedback on any query you break.

## Bumping the database version

`AppDatabase.kt` is currently:

```kotlin
@Database(
    entities = [Vehicle::class, FillUp::class, MaintenanceReminder::class],
    version = 2,
    exportSchema = false
)
```

Bump `version` to `3` after adding the new columns. It's still using
`.fallbackToDestructiveMigration()` (see the comment in that file) since
there's no real install base yet -- that's fine to keep doing right up until
this app has actual users you don't want to lose local data for, at which
point write a real `Migration(2, 3)` that adds the three columns with
`ALTER TABLE ... ADD COLUMN` instead.
