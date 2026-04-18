# FlexUpdate

[![Maven Central](https://img.shields.io/maven-central/v/io.github.evgeneprytula/flex-update)](https://central.sonatype.com/artifact/io.github.evgeneprytula/flex-update)
[![API](https://img.shields.io/badge/API-24%2B-brightgreen.svg)](https://android-arsenal.com/api?level=24)
[![CI](https://github.com/EvgenePrytula/flex-update/actions/workflows/ci.yml/badge.svg)](https://github.com/EvgenePrytula/flex-update/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

Kotlin-first in-app update library for Android. Automatically picks between flexible and immediate update flows based on Google Play priority and staleness — no boilerplate, no lifecycle wiring.

## Installation

Make sure `mavenCentral()` is in your repositories, then add:

```kotlin
dependencies {
    implementation("io.github.evgeneprytula:flex-update:<version>")
}
```

## Quick start

```kotlin
val manager = InAppUpdateManager.Builder(this).build()

lifecycleScope.launch {
    repeatOnLifecycle(Lifecycle.State.STARTED) {
        manager.outcome.collect { outcome ->
            when (outcome) {
                is UpdateOutcome.ReadyToInstall -> manager.completeUpdate()
                is UpdateOutcome.Declined -> { /* user dismissed */ }
                is UpdateOutcome.Failed -> Log.e("Update", "${outcome.error}")
                else -> Unit
            }
        }
    }
}

manager.startUpdate()
```

The manager attaches to the activity lifecycle automatically — no `onResume`, `onStop`, or `onDestroy` calls needed.

## How it works

Set the update priority (0–5) in the Google Play Developer Console. FlexUpdate reads it and picks the right flow:

| Priority | Flow | Behaviour |
|----------|------|-----------|
| 0 | — | No update shown |
| 1–2 | Flexible | Background download, app stays usable |
| 3 | Flexible → Immediate | Escalates to immediate after staleness threshold |
| 4–5 | Immediate | Full-screen overlay, update is required |

Observe `downloadState: StateFlow<DownloadState>` to track download progress and show UI (e.g. a progress bar) during a flexible download.

## Configuration

All options are set via `UpdateConfig`:

```kotlin
InAppUpdateManager.Builder(this)
    .config(
        UpdateConfig(
            mode = UpdateMode.Auto,           // default — priority drives the decision
            stalenessDaysForEscalation = 7,  // days before HIGH priority escalates to immediate
            autoInstall = false,             // true = install automatically when download finishes
        )
    )
    .build()
```

**Manual mode** — ignore Play priority and force immediate updates above your own threshold:

```kotlin
UpdateConfig(mode = UpdateMode.Manual(minPriority = UpdatePriority.HIGH))
```

## License

```
Copyright 2024 Eugene Prytula

Licensed under the Apache License, Version 2.0
http://www.apache.org/licenses/LICENSE-2.0
```
