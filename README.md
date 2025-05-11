# FlexUpdate

[![Maven Central](https://img.shields.io/maven-central/v/io.github.evgeneprytula/flex-update)](https://search.maven.org/artifact/io.github.evgeneprytula/flex-update)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://github.com/EvgenePrytula/flex-update/blob/ep/feature/Add-readme-file-and-instructions-to-instal/LICENSE)

**FlexUpdate** is a lightweight Android library that intelligently handles [in-app updates](https://developer.android.com/guide/playcore/in-app-updates) — either automatically based on the priority number, or manually based on your own custom logic.

It helps your app decide how to handle updates depending on the update priority configured via the Google Play Developer API (typically through CI/CD), or lets you invoke [flexible](https://developer.android.com/guide/playcore/in-app-updates#flexible) or [immediate](https://developer.android.com/guide/playcore/in-app-updates#immediate) updates programmatically when needed.

---

## Use Cases

**FlexUpdate** supports two modes for handling in-app updates:

- **Automatic mode**
  Automatically determines the update flow based on the priority defined during the release process via the **Google Play Developer API**.  
  The priority levels range from `0` to `5`, and the corresponding update types are decided as follows:

| **Priority**   | **Value** | **Use Case**                                     | **Update Type** |
|----------------|-----------|-------------------------------------------------|-----------------|
| **NONE**       | `0`       | Default priority, no update required.            | None            |
| **VERY_LOW**   | `1`       | Minor UI tweaks or non-essential changes.        | None            |
| **LOW**        | `2`       | Small performance improvements or features.      | Flexible        |
| **MEDIUM**     | `3`       | Moderate updates with noticeable improvements.   | Flexible        |
| **HIGH**       | `4`       | Important improvements or optimizations.         | Flexible        |
| **CRITICAL**   | `5`       | Critical security fixes or major issues.         | Immediate       |

- **Manual mode**
  If you want full control over when updates are triggered, you can bypass the automatic priority logic and manually start **flexible** or **immediate** updates.  
  Use this mode when you need custom logic for your update flow or if you just want to stick with one specific flow.

---

## Installation

To use **FlexUpdate** in your Android project, add the following dependency to your `build.gradle` file:

```gradle
implementation("io.github.evgeneprytula:flex-update:x.x.x")
```

---

## Using

### Declare the Update Manager Configuration
  
Auto Mode (Default)
  
```kotlin
private val updateManager by lazy { FlexUpdateManager.from(this) }
```
  
Manual Mode
  
```kotlin
private val updateManager by lazy { FlexUpdateManager.from(this, Manual(MEDIUM)) }
```

### Launch the Update Flow

```kotlin
lifecycleScope.launch {
    updateManager.checkForUpdate()
}
```

---

## License

This library is licensed under the [MIT License](https://github.com/EvgenePrytula/flex-update/blob/ep/feature/Add-readme-file-and-instructions-to-instal/LICENSE).