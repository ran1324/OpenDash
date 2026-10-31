# Wireless CarPlay video performance investigation

Status: measurement plan, 2026-10-31. No cause or performance fix has been validated on this unit.

## Observed symptom and test question

The owner uses **wireless** CarPlay through DiPlay on the NAM5210-A9. It connects and renders a picture, but motion stutters and perceived FPS is low. The installed DiPlay version, its active decoder/render mode, received versus displayed FPS and network timing are not yet recorded. The question is where frames are delayed or lost: wireless receive, decoder input/output, queueing, or presentation. The on-device diagnostics inventory shows media codec capabilities, but not which decoder DiPlay actually selects during CarPlay.

## Source review gate — code reviewed, installed build unverified

The [read-only source review](diplay-source-review.md) is pinned to [DiPlay Legacy Android](https://github.com/programmerguohuajing/DiPlay-Legacy-Android) commit `fd907cd50bbcc38aac448b25cdfbc0ac9d69d2d1`. `VideoStats` reports 5-second `rx` and `shown` FPS, maximum gap, bitrate, recoveries, touch-to-frame, decode p50/p90 and pacing delay. **`rx` counts submissions after decryption, not network ingress; `shown` counts decoder output released to a Surface, not physical panel presentation.** `ScreenStream` has receive and decrypt timing, but these are not all guaranteed to appear in an exported report. These source features have **not** been confirmed in the owner's installed APK.

The source review traced receive → encrypted queue → decrypt → decode queue → MediaCodec → Surface. Smooth video and Direct SurfaceView remain candidate settings, not measured fixes for this unit. A software-first workaround matches `OMX.atc.video.decoder` for AC83xx; the unit advertises `OMX.MTK.VIDEO.DECODER.AVC`. The actual decoder used at runtime is unknown. Do not change frame dropping, codec selection or renderer from source names alone.

The upstream [build notes](https://github.com/programmerguohuajing/DiPlay-Legacy-Android/blob/fd907cd50bbcc38aac448b25cdfbc0ac9d69d2d1/docs/BUILD.md) say an ordinary source APK lacks accessory identity and needs external runtime authentication assets for standalone functionality. **Try the installed app's existing session telemetry first.** A source build that starts is not enough: a test APK must be able to connect lawfully and consistently before it can replace the installed client for a live measurement. Do not extract or publish private identities. If that gate fails, use available logs or noninvasive observation from the installed build and keep the offline decoder test separate.

## Measurement design

| Path | Record | What it can and cannot conclude |
| --- | --- | --- |
| Offline H.264 playback on the unit | Actual decoder name, input/output frame counts, decode latency distribution, presented frames, dropped/late frames, queue depth, 720p and 540p, SurfaceView and TextureView where supported, CPU/RAM and elapsed time. | Separates local decode/presentation capacity from wireless transport. It does not prove live CarPlay performance. |
| Live wireless CarPlay baseline | Installed DiPlay version and active settings; 5-second post-decrypt `rx`, released-output `shown`, maxGap, decode p50/p90, recoveries and pacing delay from any available session report; actual decoder and surface mode where observable. | Compare stages in the same time window, using their precise meanings. Ingress timing, true panel presentation, queue residence and drop reasons need extra evidence if absent. Do not infer transport failure from visual stutter alone. |
| Functional preservation | Launch, reconnect, touch response, sound, phone behavior, reverse-camera priority and restoration of previous settings/app. | Ensures a measurement or candidate change is reversible and does not break the car experience. These checks are stationary only. |

Thirty stable FPS is a **provisional target**, not an observed baseline or promise. Use the same route, phone, app version and test clip/scenario for comparisons; change one variable at a time. If live telemetry is unavailable, report that limit rather than inventing an FPS breakdown. A 5 GHz hypothesis belongs in transport analysis only after timing evidence warrants it.

## One batched parked-car session

Do not schedule the visit until all materials are prepared and reviewed: installed-app version, any APK/source versions and hashes, lawful build/authentication path if a custom app is involved, short offline clips if ready, a prefilled checklist, export destination, expected duration, and rollback to the current working setup. In the **wireless** session, record version/settings once; repeat a moving UI for about 90 seconds rather than measuring an idle screen; capture a baseline at H.264/30 FPS and native resolution; then change only resolution to about 80% if supported. Use an optional Smooth video A/B only if the installed build and safety checks justify it. Mark the segments in one export, restore settings if worse, and check touch, audio, navigation and reverse-camera return before leaving. Run any prepared offline decode matrix in the same visit. No firmware flash, bootloader unlock, CAN/MCU write or road test is part of this session.

After the single report is reviewed, choose one narrowly scoped change. A decoder or surface optimization should be accepted only if the measured frame stage improves without regressions in latency, reconnection, audio, reverse camera and touch. Otherwise retain the baseline and investigate the next measured bottleneck.
