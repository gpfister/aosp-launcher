# GPLauncher: AGENTS.md

Guidance for AI coding agents working on GPLauncher (Gradle project name: `GPLauncher`; APK name: `gplauncher`).

## What this is
Android launcher for a custom AOSP 16 device. Platform-signed priv-app, device owner. Starts
`org.gpfister.aosp.gpkiosk` if present; otherwise shows a device info / system controls screen.
App id and namespace: `org.gpfister.aosp.gplauncher`. License: MIT.

## Stack
Kotlin, Jetpack Compose, Material 3, Gradle (Kotlin DSL, version catalog in `gradle/libs.versions.toml`).
`compileSdk` 37, `minSdk`/`targetSdk` 36. Keep dependencies on the latest stable versions.

## Commands
- `./gradlew test` unit tests (JUnit4 + MockK + coroutines-test)
- `./gradlew lint assembleDebug`
- `./gradlew connectedDebugAndroidTest` Compose UI tests (needs a device)

## Layout (`app/src/main/java/org/gpfister/aosp/gplauncher`)
- `MainActivity`, `GPLauncherApplication` (singletons)
- `data/` device info, system properties, kiosk resolution
- `admin/` device-owner receiver, policy, power controls
- `update/` `UpdateCoordinator` + `SystemUpdater` (update_engine) + `AppUpdater` (PackageInstaller)
- `HotZoneService` overlay hot zone (10 s hold exits the kiosk); `data/MaintenanceMode` pauses kiosk auto-start
- `ui/` Compose screens and theme
- `platform-stubs/` compile-only stubs for system APIs (`UpdateEngine`); never packaged

## Conventions
- All user-visible text lives in `res/values/strings.xml`.
- UI must work in light and dark; the theme follows the system (`GPLauncherTheme`). No hardcoded colors.
- Hidden/system APIs: prefer compile-only stubs; use reflection only for single calls (`SystemProps`, `PowerController.powerOff`).
- Keep logic behind small interfaces (`SystemProps`, `SystemUpdater`, `AppUpdater`) so it is unit-testable without a device.

## Platform caveats
- Needs the platform signature; privileged permissions are declared in `privapp-permissions-gplauncher.xml`.
- Device owner can only be set on an unprovisioned device (see README).
- Updates are triggered by a future WebSocket client calling `UpdateCoordinator.submit`. Do not add network listening elsewhere.

## Commits
Short imperative subject. Do not commit `local.properties` or build output.
