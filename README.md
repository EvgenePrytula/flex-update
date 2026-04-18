# FlexUpdate

A lightweight Android library that wraps the Google Play In-App Updates API and automatically selects between **FLEXIBLE** and **IMMEDIATE** update flows based on update priority and staleness.

## Installation

```kotlin
// settings.gradle.kts
dependencyResolutionManagement {
    repositories { mavenCentral() }
}

// build.gradle.kts (app module)
dependencies {
    implementation("io.github.evgeneprytula:flex-update:0.5.7")
}
```

## Quick start

Your activity must extend `AppCompatActivity`. Build the manager once in `onCreate` — it attaches to the lifecycle automatically.

```kotlin
class MainActivity : AppCompatActivity() {

    private lateinit var updateManager: InAppUpdateManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        updateManager = InAppUpdateManager.Builder(this).build()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                updateManager.outcome.collect { outcome ->
                    when (outcome) {
                        is UpdateOutcome.ReadyToInstall -> showInstallPrompt()
                        is UpdateOutcome.Declined       -> { /* user dismissed */ }
                        is UpdateOutcome.Failed         -> Log.e("Update", "${outcome.error}")
                        else -> Unit
                    }
                }
            }
        }

        updateManager.startUpdate()
    }

    private fun showInstallPrompt() {
        // Show a dialog or any UI to let the user restart
        updateManager.completeUpdate()
    }
}
```

## How the flow is selected

Set the update priority (0–5) in the **Google Play Developer API**. The library maps it to a flow automatically — you never choose FLEXIBLE or IMMEDIATE directly.

**Default behaviour:**

| Priority | Flow | Notes |
|----------|------|-------|
| `NONE` (0) | — | No update shown |
| `LOW` (1) | FLEXIBLE | Background download; user can keep using the app |
| `MEDIUM` (2) | FLEXIBLE | Background download; user can keep using the app |
| `HIGH` (3) | FLEXIBLE → IMMEDIATE | Escalated after ≥ 7 days staleness; immediate otherwise flexible |
| `CRITICAL` (4) or `URGENT` (5) | IMMEDIATE | Play Store overlay blocks the app until updated |

The library picks the flow, registers listeners, observes lifecycle, and cleans up — the only thing you act on is the `UpdateOutcome`.

## Configuration

`UpdateConfig` is a data class — use named parameters and only set what you need.

### Auto mode (default)

```kotlin
updateManager = InAppUpdateManager.Builder(this).build()
// equivalent to:
updateManager = InAppUpdateManager.Builder(this)
    .config(UpdateConfig())
    .build()
```

Tune the staleness escalation threshold:

```kotlin
updateManager = InAppUpdateManager.Builder(this)
    .config(UpdateConfig(stalenessDaysForEscalation = 14))
    .build()
```

### Manual mode

You set a single `minPriority`. If the Play update's priority is below it, no update is shown. At or above it, an IMMEDIATE update is forced.

```kotlin
updateManager = InAppUpdateManager.Builder(this)
    .config(UpdateConfig(mode = UpdateMode.Manual(UpdatePriority.HIGH)))
    .build()
```

### Auto-install

When `autoInstall = true`, `completeUpdate()` is called automatically once the download finishes — no `ReadyToInstall` outcome is emitted and no manual action is needed.

```kotlin
updateManager = InAppUpdateManager.Builder(this)
    .config(UpdateConfig(autoInstall = true))
    .build()
```

When `autoInstall = false` (default), observe `UpdateOutcome.ReadyToInstall` and call `completeUpdate()` yourself to control when the app restarts.

### `UpdatePriority` values

| Enum | Play priority |
|------|--------------|
| `NONE` | 0 |
| `LOW` | 1 |
| `MEDIUM` | 2 |
| `HIGH` | 3 |
| `CRITICAL` | 4 |
| `URGENT` | 5 |

## Tracking download progress (FLEXIBLE flow)

Observe `downloadState` as a `StateFlow` — useful for showing a progress bar during background downloads.

```kotlin
lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        updateManager.downloadState.collect { state ->
            when (state) {
                is DownloadState.InProgress -> progressBar.progress = state.percent
                is DownloadState.Completed  -> progressBar.hide()
                is DownloadState.Failed     -> showError(state.error)
                else -> Unit
            }
        }
    }
}
```

`DownloadState` values:

| State | Meaning |
|-------|---------|
| `Idle` | Nothing in progress |
| `InProgress(percent)` | Download running, 0–100 |
| `Completed` | File ready, awaiting restart |
| `Installing` | User confirmed restart |
| `Failed(error)` | Download or install error |

## UpdateOutcome reference

| Outcome | When |
|---------|------|
| `NotAvailable` | No update in Play Store |
| `Accepted` | User tapped Update in the dialog |
| `Declined` | User dismissed the dialog |
| `ReadyToInstall` | FLEXIBLE download complete — call `completeUpdate()` (only when `autoInstall = false`) |
| `Failed(error)` | Technical error — see `UpdateError` |

## UpdateError reference

| Error | When |
|-------|------|
| `ApiUnavailable` | Failed to reach the Play Store API |
| `DownloadFailed(code)` | Install/download error — `code` is the Play Core error code |
| `InstallFailed` | Activity result indicated an update failure |

## Lifecycle

No manual wiring needed. `InAppUpdateManager` implements `DefaultLifecycleObserver` and handles everything automatically:

- **onResume** — re-launches IMMEDIATE if an update was already in progress; triggers install if a FLEXIBLE download completed while the app was in the background.
- **onStop** — unregisters the install state listener.
- **onDestroy** — clears all references to prevent memory leaks.

## License

```
Copyright 2024 Eugene Prytula

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```
