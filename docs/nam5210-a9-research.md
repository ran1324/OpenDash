# Nakamichi NAM5210-A9 research unit

Status: research notes, updated 2026-10-31. A diagnostic APK has been built locally but not installed on this unit. No firmware change, recovery image, or verified on-device OpenDash build exists.

## Goal and scope

The owner has a 2008 Mazda 3 with an installed Nakamichi head unit. The immediate goal is to learn whether its existing hardware can support a better interface, better performance, or a genuine Android or firmware upgrade. Raspberry Pi and external hardware are deferred. The separate GOtv Android TV streamer and its CleanYourTV installation are unrelated to this car unit; its Android 14 version and network address must not be used as car hardware evidence.

## Evidence recorded so far

| Item | Observation | Source and limit |
| --- | --- | --- |
| Product model | `NAM5210-A9` | Printed on the owner’s box, not read from the PCB. [Manufacturer product page](https://nakamichicaraudio.com/products/nakamichi-nam5210-a9) describes the original product as Android 9.0; this does not establish the currently installed Android version. |
| Memory | 2 GB RAM, 32 GB storage | Box and system screenshots. |
| Reported platform | `ac8227l`, `Nakamichi_demo` | AIDA64 screenshots; exact board revision remains unknown. |
| CPU and graphics | Four Cortex-A7 cores up to 1118 MHz, ARMv7 32-bit, Mali-450 MP | AIDA64 screenshots. |
| Android base | API 27, kernel `3.18.79+` | AIDA64 screenshots. API 27 corresponds to Android 8.1; the displayed `NK11.0` label is not proof of Android 11. [Android API levels](https://developer.android.com/guide/topics/manifest/uses-sdk-element). |
| System firmware | `Na_2022.05.10.09_2214` | Head-unit About screenshots. |
| MCU firmware | `CVD1810-WJ_22.04.29_339` | Head-unit About screenshots. |
| Vehicle CAN setup | `Hiworld → Mazda → MAZDA3_W01`; protocol `146.0.1.20251110` | Head-unit settings screenshots. A later update screen reportedly offered `148.0.1.20261023` with new vehicle-model support, but the app/component presenting it is not yet conclusively identified. |
| Display and touch IDs | `1280×720_ID1`; `RY0120_1061x600` | Head-unit settings screenshots. |
| Root indication | AIDA64 reported “Rooted Device: Yes” | Usable `su` access and partition-read access have not been tested. |
| USB connection | Mac did not enumerate the head unit; ADB list was empty | A previous USB test saw only an iPhone. The unit showed a mirroring prompt, which does not establish ADB device mode. Box wiring diagram lists 4-pin and 6-pin USB connectors without identifying an OTG/debug port. |
| Local update interface | Names `update_ty.zip` (system), `cm69_update.bin` (MCU), `base.xt`, `apd`, `zxdzlogo.png`; separate CAN BOX update | Head-unit screen. These are expected file names, not evidence that compatible update files are present. |

The box shows distributor Radio Amir Ltd., phone `04-6044377`. No matching, verified stock firmware or recovery package has been obtained. The [Nakamichi downloads page](https://nakamichicaraudio.com/pages/downloads-dsp-software) did not provide a confirmed package for this exact unit and firmware combination during the prior review.

## Read-only diagnosis before firmware work

1. Preserve the existing photos and version strings. Confirm whether any existing `update_ty.zip` or `cm69_update.bin` file is actually present; analyze a copy only.
2. [OpenDash Diagnostics](../android/diagnostics/README.md) is built locally. It collects build, CPU/ABI, display/GPU, package and partition metadata available to an ordinary app and exports JSON through Android's system file picker. It explains permission and readability limits and never writes firmware partitions. Installation and results on the unit remain unverified.
3. On the parked unit, a terminal can test `id` and `su -c id` to establish whether usable root exists. Do not infer it from AIDA64 alone. If root works, map partition names, sizes and read access without modifying them.
4. Before any flashing, obtain stock firmware for the exact board revision and MCU, plus a tested recovery path for a unit that will not boot. Determine whether Android, MCU and CAN firmware live on separate storage and how each is restored. Verify complete backups and hashes on another computer. A partition dump alone is not yet a proven restorable backup.
5. Only then evaluate an OS port against display, touch, audio, radio, Bluetooth, GPS, reverse camera, steering-wheel controls, power management and MCU/CAN integration. Bench testing cannot substitute for stationary in-car validation of these functions.

## Emulator and performance plan

An Android Virtual Device can help develop and test an OpenDash Android app or launcher at **API 27**, 1280×720 layout and a 2 GB memory budget. It cannot reproduce this board’s Mali-450 GPU, MCU/CAN bus, audio and camera paths, boot timing, thermals or actual responsiveness. An image of the unit also would not automatically boot as an AVD. CPU clock settings in an AVD do not make its performance equivalent to a Cortex-A7 device.

This Mac is Apple Silicon (`arm64`). Its installed SDK has Android platform 34, build-tools 34.0.0 and platform-tools 37.0.1, but no installed API 27 platform, system image or emulator package. `sdkmanager` exists; Android Studio and an `emulator` executable were not found. JDK 26 and JDK 11 are present. No installation was performed for this assessment.

If approved later, install Android Studio/Emulator and an available AVD image after checking its ABI. A host-matched ARM64 AVD is useful for fast UI iteration; an API 27 32-bit ARM image, if available, would improve API/ABI fidelity but should be expected to run slowly on an ARM64 Mac. Do not treat either as a performance replica. Keep an Android 8.1/API 27 compatibility target for app behavior, and measure frame time, startup, memory, package size and thermal behavior on the real unit once read-only access or a reversible app install is available. Mock the MCU, CAN, reverse-camera and steering-wheel interfaces in desktop tests.

Official Android documentation: [AVD configuration](https://developer.android.com/studio/run/managing-avds), [emulator acceleration](https://developer.android.com/studio/run/emulator-acceleration), [emulator release notes](https://developer.android.com/studio/releases/emulator).
