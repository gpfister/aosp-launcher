# GPLauncher

Open-source (MIT) home/launcher app for a custom Android (AOSP 16) device.

- App id: `org.gpfister.aosp.gplauncher`
- Kotlin, Jetpack Compose, Material 3; follows the system light/dark mode
- Platform-signed priv-app and **device owner**
- Starts `org.gpfister.aosp.gpkiosk` automatically when installed

## Exiting the kiosk

While the kiosk runs, an invisible 96dp hot zone in the top-left corner (overlay window) watches for a
touch held for 10 seconds. It then force-stops the kiosk and shows the fallback screen, with kiosk
auto-start paused until the launcher process restarts (e.g. a reboot) or **Start kiosk** is pressed.

## Fallback screen (no kiosk app)

- Device information: Ethernet MAC and IP, launcher version, kiosk version (`N/A` if absent)
- Image versions: active A/B slot and build version of every partition (boot, system, system_ext, vendor, product, odm, vendor_dlkm, odm_dlkm)
- System controls: Reboot, Power off, Android Settings, Android Files, Start kiosk (when the kiosk is installed)

## Updates

Updates are driven by a WebSocket service (not part of this repo yet) that will call
`UpdateCoordinator.submit(UpdateCommand)`:

- `SystemUpdate`: incremental (or full) A/B OTA payload streamed from a URL by `update_engine`, then reboot.
- `AppUpdate`: APK downloaded from a URL, SHA-256 verified, installed silently via `PackageInstaller`.

## Build

Requirements: JDK 17+, Android SDK with platform `android-37.2` (`compileSdk` 37; `minSdk`/`targetSdk` 36).

```sh
./gradlew test lint assembleDebug
```

`android.os.UpdateEngine` is a system API missing from the public SDK, so `:platform-stubs`
provides compile-only stubs. They are not packaged; the real classes come from the platform.

## Installing on the device

1. Sign the APK with the platform key and install it as a priv-app (`/system_ext/priv-app/GPLauncher/`).
2. Install `privapp-permissions-gplauncher.xml` to `/system_ext/etc/permissions/`.
3. Make it device owner (on a fresh, unprovisioned device) at build time or with:
   ```sh
   adb shell dpm set-device-owner org.gpfister.aosp.gplauncher/.admin.GPDeviceAdminReceiver
   ```

## License

[MIT](LICENSE)
