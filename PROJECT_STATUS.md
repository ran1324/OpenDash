# OpenDash project status and coordinated work plan

Updated 2026-10-31. This is the single entrypoint for current decisions and next work. Keep detailed evidence in the linked documents; update this page when a decision or gate changes. The [roadmap](ROADMAP.md) describes broader possibilities, not the current execution order.

## Current objective

Improve the installed Nakamichi NAM5210-A9 in a 2008 Mazda 3. Near-term work is to diagnose and improve **wireless CarPlay video smoothness** on the existing unit. In parallel, investigate whether a genuine newer Android system can be built safely for this exact hardware. A launcher alone does not satisfy the system-upgrade goal. Raspberry Pi is a separate possible path and is not the current replacement decision.

The owner reports that DiPlay wireless CarPlay connects and displays video, but motion stutters and frame rate feels low. This is **wireless**, not USB CarPlay. No on-device received-versus-presented frame rates, decoder timing, queue measurements or wireless transport traces have yet established the cause. The reported lack of 5 GHz support is context, not a diagnosed bottleneck. Do not optimize or replace a component solely on that assumption.

## Known evidence and boundaries

- [NAM5210-A9 model dossier](docs/nam5210-a9-research.md): box model, screenshots, two curated OpenDash Diagnostics exports, vendor services and HAL names, unknowns and recovery gate. The reports confirm API 27/Android 8.1 properties, ARMv7/32-bit and an AC8227L platform label; they do not identify the exact board revision or prove a safe OS image.
- [OpenDash Diagnostics](android/diagnostics/README.md): the installable, read-only reporting app. It has produced two reports on the car unit. Inventory is not a functional test of CAN, reverse camera, radio, Bluetooth or boot recovery.
- [Wireless CarPlay performance investigation](docs/wireless-carplay-performance.md): measurement plan, DiPlay source-review gate and one consolidated parked-car session.
- [Pinned DiPlay source review](docs/diplay-source-review.md): actual queue, decrypt, decoder and reporting paths, with source links and measurement limits.
- [Contributing and safety rules](CONTRIBUTING.md): do not publish personal data or proprietary firmware; do not present unverified flashing instructions as safe.

Raw device reports and private conversation content are not part of this public repository. Publish only reviewed technical summaries. The separate GOtv Android TV streamer and CleanYourTV work are unrelated to this car unit.

## Decisions

| Decision | Current position | Revisit when |
| --- | --- | --- |
| Hardware target | Keep the installed NAM5210-A9 as the primary target. | Measured limits or missing recovery make an alternative necessary. |
| Immediate performance work | Measure wireless video receive, decode and presentation separately before changing DiPlay or Android. | A baseline and repeatable bottleneck are available. |
| Android upgrade | Continue feasibility research for a real system upgrade, not only a launcher. No flash target has been validated. | Exact board, compatible vendor stack, stock image and recovery route are established. |
| Test cadence | Prepare code, logs and instructions at home; use **one batched parked-car session** after the package is ready. | The test package and rollback are reviewable. |
| Raspberry Pi | Deferred alternative, not an approved replacement or purchase. | Existing-unit options have been measured and evaluated. |

## Workstreams and instructions

| Workstream / owner | Next deliverable | Acceptance gate |
| --- | --- | --- |
| Project coordination — OpenDash maintainer | Maintain this page, reconcile findings from each workstream, and assemble a single test package for the owner. | Every new claim has provenance and uncertainty; no duplicate car visits or conflicting instructions. |
| DiPlay source review — read-only researcher | **Completed for upstream commit `fd907cd50bbcc38aac448b25cdfbc0ac9d69d2d1`.** See the [review](docs/diplay-source-review.md). Compare source/version with the installed app and determine whether its existing diagnostic report can be exported. | Do not equate source telemetry with measurements already observed on this unit or assume a locally built client can authenticate. |
| Performance instrumentation — Android engineering, after installed-report review | First inspect an existing DiPlay session report. Design the smallest reversible addition only for missing stage measurements. | Standalone build/authentication assets are lawful and feasible; exact source commit, APK version and rollback documented; local tests pass. No unmeasured optimization bundle. |
| Decode-only benchmark — Android engineering | Specify an offline H.264 test at 720p and 540p using representative SurfaceView/TextureView paths, separate from live CarPlay. | Licensed test media and compatible API 27 build; metrics are comparable and do not claim to isolate wireless behavior. Avoid background services and sustained stress on the old unit. |
| Firmware/recovery research — hardware/Android researcher | Narrow, read-only review of legacy VINTF, fstab/init and OEM interface definitions; identify exact board, stock image and recovery method. | No firmware write or bootloader change. A proposed Android image is only actionable after board, vendor compatibility and tested restore path are known. |
| Vehicle validation — owner with prepared instructions | One stationary, parked-car session using the consolidated checklist in the performance plan. | Baseline report and rollback evidence captured; reverse camera, audio, radio and controls are preserved. No road test to discover basic safety behavior. |

These are responsibility lanes, not parallel implementation projects or new repositories. The DiPlay source review is already assigned and its findings should be merged here before any instrumentation task begins.

## Next coordination checkpoint

1. Determine the installed DiPlay version and whether its existing diagnostic report contains the needed session logs. The source review found a software-first workaround keyed to `OMX.atc.video.decoder` on AC83xx, while this unit advertises `OMX.MTK.VIDEO.DECODER.AVC`; whether that workaround applies here is unproven.
2. Select one reversible baseline method using existing logs if available, and prepare any offline benchmark and live wireless logging together. Document exact APKs, versions, export steps, expected run time and rollback. Resolve the external authentication-asset gate before any custom DiPlay APK is proposed.
3. Review the complete package before asking the owner for the single parked-car test. Analyze that report before choosing decode, render, queue or transport changes.
4. Keep Android upgrade research open, but do not flash until the independent recovery and compatibility gates above are met.
