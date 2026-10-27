# OpenDash roadmap

All items are planned, not delivered.

## Phase 0 — Research
- [ ] Identify hardware SoC, MCU, display, touch controller, Bluetooth chipset, GPS, audio, and reverse-camera paths
- [ ] Establish read-only diagnostics and a tested recovery strategy before any firmware modifications
- [ ] Document licensing and feasibility of genuine CarPlay receiver solutions

## Phase 1 — Prototype
- [ ] Desktop demo of large-icon home screen, clock, media and settings
- [ ] Simulated incoming-call overlay and reverse-camera priority
- [ ] Define hardware abstraction interfaces
- [ ] Measure performance on low-resource ARM hardware

## Phase 2 — Raspberry Pi
- [ ] Touchscreen and audio output
- [ ] Bluetooth A2DP/AVRCP and hands-free calling with microphone
- [ ] Wi-Fi, GPS and power management
- [ ] Camera input and reverse signal integration

## Phase 3 — Existing head units
- [ ] Prototype reversible Android display client over supported data transport
- [ ] Validate latency, touch, audio, and camera handoff
- [ ] Document compatible hardware and unsupported models

## Phase 4 — Optional integrations
- [ ] Evaluate licensed/compatible wired CarPlay solutions
- [ ] Evaluate wireless CarPlay feasibility and hardware requirements
- [ ] Package installable builds only after device-specific testing
