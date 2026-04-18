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
    implementation("io.github.evgeneprytula:flex-update:0.4.4")
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

        updateManager.startUpdate { outcome ->
            when (outcome) {
                is UpdateOutcome.ReadyToInstall -> showInstallPrompt()
                is UpdateOutcome.Declined       -> { /* user dismissed */ }
                is UpdateOutcome.Failed         -> Log.e("Update", "${outcome.errorCode}")
                else -> Unit
            }
        }
    }

    private fun showInstallPrompt() {
        // Show a snackbar, dialog, or any UI to let the user restart
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

The library picks the flow, registers listeners, observes lifecycle, and cleans up — the only thing you act on is the `UpdateOutcome` callback.

## Configuration modes

`UpdateConfig` supports two modes.

### Auto mode (default)

No configuration needed. The library reads the Google Play priority and applies the built-in mapping.

```kotlin
updateManager = InAppUpdateManager.Builder(this).build()
```

You can only tune the staleness threshold in auto mode:

```kotlin
updateManager = InAppUpdateManager.Builder(this)
    .config(UpdateConfig.Builder().stalenessDaysForEscalation(14).build())
    .build()
```

### Manual mode

You set a single `minPriority`. If the Play update's priority is below it, no update is shown. At or above it, an IMMEDIATE update is forced — Play priority is otherwise ignored.

```kotlin
updateManager = InAppUpdateManager.Builder(this)
    .config(
        UpdateConfig.Builder()
            .manual(UpdatePriority.HIGH)  // force IMMEDIATE for priority 3+, skip anything lower
            .build()
    )
    .build()
```

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
                is DownloadState.Failed     -> showError(state.errorCode)
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
| `Failed(errorCode)` | Download or install error |

## UpdateOutcome reference

`startUpdate` delivers exactly one outcome per call:

| Outcome | When |
|---------|------|
| `NotAvailable` | No update in Play Store |
| `Accepted` | User tapped Update in the dialog |
| `Declined` | User dismissed the dialog |
| `ReadyToInstall` | FLEXIBLE download complete — call `completeUpdate()` |
| `Failed(errorCode)` | Technical error |

## Lifecycle

No manual wiring needed. `InAppUpdateManager` implements `DefaultLifecycleObserver` and handles everything automatically:

- **onResume** — re-launches IMMEDIATE if an update was already in progress; emits `ReadyToInstall` if a FLEXIBLE download completed while the app was in the background.
- **onStop** — unregisters the install state listener.
- **onDestroy** — clears all references to prevent memory leaks.

## License

```
Copyright 2024 Eugene Prytula

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```
