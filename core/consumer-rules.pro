# ── FlexUpdate public API ──────────────────────────────────────────────────────

# Main manager and its nested Builder / Companion
-keep class com.madappgang.flexupdate.core.InAppUpdateManager { public *; }
-keep class com.madappgang.flexupdate.core.InAppUpdateManager$Builder { public *; }
-keep class com.madappgang.flexupdate.core.InAppUpdateManager$Companion { public *; }

# Configuration and strategy
-keep class com.madappgang.flexupdate.core.UpdateConfig { public *; }
-keep class com.madappgang.flexupdate.core.UpdateStrategy { public *; }

# Provider interface and implementations (including Fake for testing)
-keep interface com.madappgang.flexupdate.core.AppUpdateManagerProvider
-keep class com.madappgang.flexupdate.core.DefaultAppUpdateManagerProvider { public *; }
-keep class com.madappgang.flexupdate.core.FakeAppUpdateManagerProvider { public *; }

# Sealed classes, data objects, and enums in the types package.
# Consumers use these in when-expressions — all subclasses must survive shrinking.
-keep class com.madappgang.flexupdate.core.types.** { *; }

# Kotlin metadata — required for sealed class exhaustiveness and reflection
-keepattributes *Annotation*, Signature, InnerClasses, EnclosingMethod
