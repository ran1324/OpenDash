# OpenDash Diagnostics

An installable, read-only Android 8.1 (API 27) diagnostic app for the Nakamichi NAM5210-A9 investigation. It runs as an ordinary app and requests no Android permissions. It is not a ROM, recovery tool, or proof that another Android build will work.

## Install and use

1. Copy `opendash-diagnostics-debug.apk` to the head unit with a USB drive or other supported file transfer. Allow installation from that file manager if Android asks. The app does not require ADB.
2. Open **OpenDash Diagnostics** and tap **Scan capabilities & drivers**. Collection does not request root.
3. If you want to test usable root, tap **Test root (su -c id)** while parked. A root manager may ask for approval. This runs only `id`, does not dump or change partitions, and stops waiting after four seconds.
4. Tap **Export JSON…** and select a location through Android's system file picker. Review the report before sharing; build properties and package names may identify the unit.

The JSON field format is `{ "status": "readable", "value": ..., "source": ... }` or `{ "status": "missing|permission_denied|unreadable|unavailable|not_tested|not_confirmed|too_large|partial|timeout", "note": ... }`. A missing or denied value is not evidence that the underlying feature is absent. `oemPackages.candidates` is a heuristic list of visible system packages; OEM services can be hidden in other packages or native binaries.

Collection covers build and ABI, CPU and memory, current free RAM, kernel, active display, OpenGL ES runtime strings, input and audio devices, advertised sensors and hardware features, storage, selected system properties, Treble markers, VINTF file metadata and top-level HAL names where readable, partition table and mounts, block-device directory names, candidate OEM packages, and root capability. It does not open block devices, back up firmware, change settings, or request elevated access automatically. Partition directory names and `/proc/partitions` are metadata, not a verified recovery map. Some `/proc`, `/sys`, `/vendor`, and `/dev` entries may be unavailable to a normal app. The display dimensions are for the active Android display and do not identify the touch controller.

The APK has no third-party libraries, background service, network access, or scheduled collection. It collects only after a button press. Individual file reads and lists have fixed size limits, and the report stays in memory only until the app exits or you export it. The native interface and bounded inventories are designed for the 2 GB unit; actual RAM use and scan duration still need measurement on it. Concurrent scans are blocked and the screen preview is capped at 64 KiB; export retains the collected report. The root probe is a separate, optional button.

## Build locally

Run `./build.sh` on a Mac with Android SDK platform 34, build-tools 34.0.0, and JDK 17. Override `ANDROID_HOME`, `ANDROID_BUILD_TOOLS`, `ANDROID_PLATFORM_JAR`, `JAVAC`, and `KEYTOOL` for other installations. The script uses platform tools directly and writes `build/release/opendash-diagnostics-debug.apk`. The local debug signing key stays in ignored `build/` and should not be treated as a release key.

## Validation status

The source compiles against Android platform 34 with Java 8 bytecode, DEX is built with min API 27, and APK signature verification passes. No NAM5210-A9 is connected over ADB in this environment, so installation, screen layout, root prompts, and actual hardware observations still need a parked-device run. Save the report before using it to select any firmware direction.

## Extended inventory (0.2)

The scan additionally enumerates loaded kernel modules and module directories; platform, I2C, SPI, USB, SDIO and PCI driver registrations and device-to-driver symlinks; HAL library/service filenames; sound/input device metadata; CPU frequency policies, thermal readings, backlight and framebuffer metadata; Android Binder service names; all visible system packages and their declared services; camera characteristics, USB host device IDs, advertised media codecs, and location provider names. It never opens a camera, microphone, USB connection, device node or CAN/MCU channel. No audio or video playback, location request, sensor subscription, driver load/unload or stress test occurs.

`coverage` explicitly lists untested functions. Enumeration does not validate driver correctness, and drivers compiled into the kernel may not appear in `/proc/modules`. A proprietary reverse camera, radio or MCU may bypass standard Android APIs. Root testing only verifies identity; it does not run the inventory as root. Paths hidden by SELinux remain unknown.

Inventories have entry and byte caps recorded in the report. Commands drain their output concurrently and stop waiting at their deadlines. Individual OEM framework/file calls have no hard deadline; a defective vendor implementation could still stall a scan. Thermal and CPU sysfs values retain the units supplied by the kernel and are not interpreted as calibrated measurements.

## Tests and device acceptance

Run `./test.sh` for host tests covering normal command output, output exceeding pipe capacity, memory caps, timeouts, nonzero exits and missing executables. APK build, API 27 metadata, absence of manifest permissions and signature verification are checked locally. These do not replace an Android runtime test.

On the parked NAM5210-A9, install the APK, run one scan, export and reopen the JSON, then verify it contains the `kernelDrivers`, `driverBindings`, `services` and `coverage` sections. Check missing/denied statuses and record scan duration from `collection`. Cancel an export once to verify it returns to the app. Test optional root separately if desired. Physical vehicle functions remain a later supervised acceptance step.
