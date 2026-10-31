# Nakamichi NAM5210-A9 research unit

Status: research notes, updated 2026-10-31. OpenDash Diagnostics has run on the unit and produced two JSON reports. No firmware change, recovery image, or verified on-device OpenDash system build exists.

## Goal and scope

The owner has a 2008 Mazda 3 with an installed Nakamichi head unit. The immediate goal is to learn whether its existing hardware can support a better interface, better performance, or a genuine Android or firmware upgrade. Raspberry Pi and external hardware are deferred. The separate GOtv Android TV streamer and its CleanYourTV installation are unrelated to this car unit; its Android 14 version and network address must not be used as car hardware evidence.

## Evidence recorded so far

| Item | Observation | Source and limit |
| --- | --- | --- |
| Product model | `NAM5210-A9` | Printed on the owner’s box, not read from the PCB. [Manufacturer product page](https://nakamichicaraudio.com/products/nakamichi-nam5210-a9) describes the original product as Android 9.0; this does not establish the currently installed Android version. |
| Memory | 2 GB RAM, 32 GB storage | Box and system screenshots. |
| Reported platform | `ac8227l`, `Nakamichi_demo` | AIDA64 screenshots and device reports; exact chip marking and physical board revision remain unknown. |
| CPU and graphics | Four Cortex-A7 cores, ARMv7 32-bit, Mali-450 MP | Earlier AIDA64 screenshot reported up to 1118 MHz. The device reports confirm four ARMv7 cores and the Mali-450 MP OpenGL renderer; they do not independently verify that maximum clock. |
| Android base | API 27, Android property `8.1.0`, kernel `3.18.79+` | Device reports. API 27 corresponds to Android 8.1; the displayed `NK11.0` label is not proof of Android 11. [Android API levels](https://developer.android.com/guide/topics/manifest/uses-sdk-element). |
| System firmware | `Na_2022.05.10.09_2214` | Head-unit About screenshots. |
| MCU firmware | `CVD1810-WJ_22.04.29_339` | Head-unit About screenshots. |
| Vehicle CAN setup | `Hiworld → Mazda → MAZDA3_W01`; protocol `146.0.1.20251110` | Earlier head-unit settings screenshots. The newer device reports identify an enabled `com.hcn.autocan` app at version `148.0.1.20261023`; that is an installed app version, not evidence of an Android or CAN-box firmware upgrade. |
| Display and touch IDs | `1280×720_ID1`; `RY0120_1061x600` | Head-unit settings screenshots. |
| Root indication | AIDA64 reported “Rooted Device: Yes”; the device reports found a `su` path | The optional probe changed from `not_tested` to `unavailable` (could not start or timed out). It did **not** confirm `uid=0`. Root access and partition-read access remain unverified. |
| USB connection | Mac did not enumerate the head unit; ADB list was empty | A previous USB test saw only an iPhone. The unit showed a mirroring prompt, which does not establish ADB device mode. Box wiring diagram lists 4-pin and 6-pin USB connectors without identifying an OTG/debug port. |
| Local update interface | Names `update_ty.zip` (system), `cm69_update.bin` (MCU), `base.xt`, `apd`, `zxdzlogo.png`; separate CAN BOX update | Head-unit screen. These are expected file names, not evidence that compatible update files are present. |

The box shows distributor Radio Amir Ltd., phone `04-6044377`. No matching, verified stock firmware or recovery package has been obtained. The [Nakamichi downloads page](https://nakamichicaraudio.com/pages/downloads-dsp-software) did not provide a confirmed package for this exact unit and firmware combination during the prior review.

## Evidence from on-device diagnostics

Two OpenDash Diagnostics schema 2 reports were exported from the head unit on 2026-10-31 at 00:16:13 and 00:16:32 UTC. These are read-only ordinary-app observations. The meaningful difference is the optional root check: the first says `not_tested`, and the second says `unavailable` because `su` could not start or timed out. RAM and storage availability are momentary snapshots; the two collection durations, 1098 and 807 ms, are not performance benchmarks. Raw reports are deliberately not published: they contain a broader package inventory, mount paths and other device details. The following is curated technical evidence.

| Area | Observed evidence | Interpretation and limit |
| --- | --- | --- |
| Android identity | `ro.build.version.release=8.1.0`, SDK 27, security patch reported `2018-07-05`; vendor build identifies an `8227L_demo` Android 8.1 family; `ro.hardware`, `ro.boot.hardware` and `ro.board.platform` report `ac8227l`. Supported ABIs are `armeabi-v7a` and `armeabi`. | The `NK11.0` display label does not change the API level. Neither build string nor platform property identifies the exact physical board revision. |
| Display and input | Active display `1280×720`, 160 dpi, approximately 60 Hz. Android input devices include `TS_GT9xx` and `mtk-kpd`. | These are enumerated interfaces, not proof of touch accuracy, steering-wheel input, or a particular touch-controller revision. The earlier `RY0120_1061x600` settings value is not necessarily screen resolution. |
| Graphics and memory | OpenGL ES reports `Mali-450 MP`. Total RAM is 2,035,564,544 bytes; available RAM varies with time. | GPU identification is stronger than the earlier screenshot alone. Available memory does not predict performance of a new Android build. |
| CAN app | Enabled `com.hcn.autocan` version `148.0.1.20261023` declares `com.hcn.autocan.service.CanService`. | This identifies the earlier update number as an installed CAN application version. It does not establish the CAN-box firmware version or prove that the CAN service works correctly. |
| Vehicle-facing framework services | Binder registers `mcu_service` (`android.carsource.IMcuManager`), `carstatus` (`android.carstatus.ICarStatus`), `sourceinfo` (`android.sourceservice.ISourceService`) and `fmradio` (`android.radio.IRadioPlayer`). | Service registration identifies integration points. Methods, permissions, data formats and MCU transport have not been inspected or invoked. |
| Vendor interface | `ro.treble.enabled=true`. A readable legacy `/vendor/manifest.xml` is 13,424 bytes and names standard Android, MediaTek and AutoChips HALs, including AutoChips audio, backcar, DVR, IAP, metalogo and USB. | Only top-level HAL names were collected, not the complete XML or compatibility matrices. `ro.vndk.version` was unset or hidden; modern VINTF paths were unavailable to the app. Treble alone does not establish GSI or newer-Android compatibility. |
| Storage layout | The observed MMC controller path includes `ANDROID` mounted read-only at `/system`, `VENDOR` read-only at `/vendor`, `userdata` at `/data`, `CACHE` at `/cache`, and `APD` at `/apd`, all shown as ext4 mounts. | This is a mount observation, not a complete partition table or a restorable image. Boot and recovery locations, sizes and contents remain unknown. |
| Driver and HAL files | Names under `/vendor/lib/modules` include `bt_drv.ko`, `fmradio_drv.ko`, `gps_drv.ko`, `wlan_drv_gen2.ko`, `wmt_chrdev_wifi.ko` and `wmt_drv.ko`. Visible HAL/service filenames include `ac8227l` audio, GPS, graphics and power components and AutoChips audio/backcar services. | Presence of files does not prove they are loaded or functional. Several `/proc` and `/sys` inventories were unreadable or denied to the ordinary app. |

The working architecture to investigate is: OpenDash and Android framework services → OEM Binder interfaces and vendor HALs → kernel drivers and hardware. The MCU/CAN relationship is a hypothesis until interface definitions and data flow are inspected. Any future system must preserve and test audio, radio, Bluetooth, reverse camera, steering-wheel controls and ignition/power behavior. The reports do not functionally validate any of them.

## Read-only diagnosis before firmware work

1. Preserve the existing photos and version strings. Confirm whether any existing `update_ty.zip` or `cm69_update.bin` file is actually present; analyze a copy only.
2. [OpenDash Diagnostics](../android/diagnostics/README.md) has produced two reports on the unit. Its ordinary-app inventory cannot prove that a driver works or that a partition can be backed up. Record each field's readability status before drawing conclusions.
3. If further root research is necessary, establish why the optional `su -c id` probe was unavailable before treating root as usable. Do not infer it from AIDA64 or a `su` file alone. Only after `uid=0` is confirmed should read-only partition mapping be considered.
4. Before any flashing, obtain stock firmware for the exact board revision and MCU, plus a tested recovery path for a unit that will not boot. Determine whether Android, MCU and CAN firmware live on separate storage and how each is restored. Verify complete backups and hashes on another computer. A partition dump alone is not yet a proven restorable backup.
5. Only then evaluate an OS port against display, touch, audio, radio, Bluetooth, GPS, reverse camera, steering-wheel controls, power management and MCU/CAN integration. Bench testing cannot substitute for stationary in-car validation of these functions.

The next narrow read-only research targets are the complete legacy vendor manifest, relevant compatibility matrices, fstab/init declarations and definitions for the four vehicle-facing Binder interfaces. Use an explicit allowlist and review exports for private data; do not call undocumented Binder methods just to discover behavior. Bootloader state, unlock procedure, GSI compatibility, stock firmware and a recovery route are not verified. There is currently no safe flash target.

## Emulator and performance plan

An Android Virtual Device can help develop and test an OpenDash Android app or launcher at **API 27**, 1280×720 layout and a 2 GB memory budget. It cannot reproduce this board’s Mali-450 GPU, MCU/CAN bus, audio and camera paths, boot timing, thermals or actual responsiveness. An image of the unit also would not automatically boot as an AVD. CPU clock settings in an AVD do not make its performance equivalent to a Cortex-A7 device.

This Mac is Apple Silicon (`arm64`). Its installed SDK has Android platform 34, build-tools 34.0.0 and platform-tools 37.0.1, but no installed API 27 platform, system image or emulator package. `sdkmanager` exists; Android Studio and an `emulator` executable were not found. JDK 26 and JDK 11 are present. No installation was performed for this assessment.

If approved later, install Android Studio/Emulator and an available AVD image after checking its ABI. A host-matched ARM64 AVD is useful for fast UI iteration; an API 27 32-bit ARM image, if available, would improve API/ABI fidelity but should be expected to run slowly on an ARM64 Mac. Do not treat either as a performance replica. Keep an Android 8.1/API 27 compatibility target for app behavior, and measure frame time, startup, memory, package size and thermal behavior on the real unit once read-only access or a reversible app install is available. Mock the MCU, CAN, reverse-camera and steering-wheel interfaces in desktop tests.

Official Android documentation: [AVD configuration](https://developer.android.com/studio/run/managing-avds), [emulator acceleration](https://developer.android.com/studio/run/emulator-acceleration), [emulator release notes](https://developer.android.com/studio/releases/emulator).
