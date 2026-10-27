# OpenDash

**Give your dashboard a new life.**

OpenDash is a planned, lightweight, source-available automotive infotainment system for modernizing existing car head units.

> **Project status: planning and research.** There is no functional release or verified hardware compatibility yet.

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

Early research includes the Nakamichi NAM5210 family and a unit reporting MAZDA3_W01 / NK11 / CVD1810-WJ; compatibility is **not verified**.

## Roadmap and contributions

See [ROADMAP.md](ROADMAP.md) and [CONTRIBUTING.md](CONTRIBUTING.md).

## License

OpenDash is **source-available**, not OSI-approved open source. Original project code is licensed under the [PolyForm Noncommercial License 1.0.0](LICENSE.md). Commercial use requires separate permission from the relevant rights holders. Third-party components retain their own licenses. No Apple proprietary CarPlay components, trademarks, or third-party firmware are licensed or bundled by OpenDash.

**Safety:** Do not flash unverified firmware or rely on untested reverse-camera integrations while driving.
