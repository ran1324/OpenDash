# OpenDash

**Give your dashboard a new life.**

OpenDash is a planned, lightweight, source-available automotive infotainment system for modernizing existing car head units.

> **Project status: planning and research.** An Android diagnostics prototype is available; the full infotainment system and hardware compatibility remain unverified.

**Start here:** [Project status and coordinated work plan](PROJECT_STATUS.md). It records the current decisions, evidence, workstreams, acceptance gates, and the single planned parked-car test session.

## Vision

A fast, elegant, driver-friendly touchscreen experience with large icons, a clock, music, phone, navigation, and settings. We aim to retain existing hardware where practical instead of forcing a complete replacement.

## Planned features

- Bluetooth audio playback, media controls, hands-free calls and microphone support
- Wi-Fi and GPS support
- Automatic reverse-camera display, prioritized over other screens
- Quick startup and power management
- Raspberry Pi and embedded Linux support
- Experimental Android head-unit companion app for reversible external display integration
- Optional **genuine CarPlay receiver** integration, wired or wireless, subject to technical feasibility and licensing

## Technical direction

A lightweight Linux system with a touch-first UI (potentially Qt/QML), hardware abstraction layers, and modular services. Initial prototypes will run on desktop and Raspberry Pi. Existing Android head-unit compatibility will require hardware-specific research.

The current research unit is a Nakamichi **NAM5210-A9** (box identification) in a 2008 Mazda 3, reporting MAZDA3_W01 / NK11.0 / CVD1810-WJ. Do not treat the NAM5210 without the `-A9` suffix as an identical board; compatibility is **not verified**.

The [Nakamichi NAM5210-A9 model dossier](docs/nam5210-a9-research.md) combines earlier on-screen observations with two curated on-device diagnostic exports and an emulator plan. The reports confirm an Android 8.1/API 27 base and expose some vehicle-facing services, but do not establish a safe firmware upgrade or a recovery path. This work focuses on the installed head unit; the Raspberry Pi direction in the roadmap is deferred for this investigation.

The first installable component is [OpenDash Diagnostics](android/diagnostics/README.md), a read-only API 27 app that exports a hardware and Android report through the system file picker. Its presence does not establish firmware compatibility. Download the test APK from [Diagnostics releases](https://github.com/ran1324/OpenDash/releases/tag/diagnostics-v0.4.0).

## Roadmap and contributions

See [ROADMAP.md](ROADMAP.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## License

OpenDash is **source-available**, not OSI-approved open source. Original project code is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE.md). Commercial use requires separate permission from the relevant rights holders. Third-party components retain their own licenses. No Apple proprietary CarPlay components, trademarks, or third-party firmware are licensed or bundled by OpenDash.

**Safety:** Do not flash unverified firmware or rely on untested reverse-camera integrations while driving.
