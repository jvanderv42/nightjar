<p align="center">
  <img src="docs/assets/banner.png" width="800" alt="Nightjar game streaming client">
</p>

<h1 align="center">Nightjar Custom</h1>
<p align="center"><strong>PC game streaming with gyro controls you can actually configure.</strong></p>
<p align="center">
  An unofficial Android-focused fork of <a href="https://github.com/RamazanKara/nightjar">Nightjar</a>, built to fix troublesome controller motion input and give players control over gyroscope mapping.
</p>
<p align="center">
  <a href="https://github.com/jvanderv42/nightjar/releases/download/custom-5.2.0-1/nightjar-5.2.0-custom-universal.apk"><img src="https://img.shields.io/badge/Download-Android%20APK-3cba91?style=for-the-badge&logo=android&logoColor=white" alt="Download the universal Android APK"></a>
  <a href="https://github.com/jvanderv42/nightjar/releases/tag/custom-5.2.0-1"><img src="https://img.shields.io/badge/Release-5.2.0%20Custom-5969df?style=for-the-badge" alt="Release details"></a>
</p>
<p align="center">
  <a href="https://github.com/jvanderv42/nightjar/releases"><img src="https://img.shields.io/github/downloads/jvanderv42/nightjar/total?label=downloads" alt="Fork downloads"></a>
  <a href="LICENSE.txt"><img src="https://img.shields.io/badge/license-GPL--3.0-blue" alt="GPL-3.0 license"></a>
  <img src="https://img.shields.io/badge/Android-5.0%2B-brightgreen" alt="Android 5.0 or newer">
</p>

> **The short version:** If your controller has a gyroscope but aiming feels rotated, inverted, or inconsistent when streaming PC games, this version of Nightjar is designed to help. It adds automatically selected Flydigi mappings, manual axis correction, editable profiles and diagnostic tools, without giving up Nightjar's excellent Moonlight-compatible streaming features.

## Why this fork exists

A gamepad can work perfectly when connected directly to Windows yet behave differently over Android game streaming. In particular, some controllers expose motion through Android as a generic **“Pro Controller”**: the gyroscope is present, but its axes do not line up with the directions the game expects. Move the controller left and the camera may move up. Tilt it vertically and the camera may turn sideways. A standard sensitivity slider can't repair an axis that's mapped to the wrong direction.

This started as a practical attempt to solve exactly that problem on an **Onn. Astra 2 Android gaming handheld**, using **Flydigi Apex 4 (Wukong edition)** and **Vader 4 Pro** controllers. We confirmed the controllers' motion worked directly on Windows, then isolated inconsistent gyro orientation when streaming through Android. The solution grew into reusable controller identification, axis remapping, adjustable sensitivity and tools to understand the input a controller is actually reporting.

**The goal isn't to replace upstream Nightjar.** It's to make its Android game-streaming experience more useful to people who care about motion aiming, unusual controllers and troubleshooting input behavior. The project is built on upstream Nightjar 5.2.0 and retains its broader streaming feature set.

## What this edition adds

| Feature | Why it matters |
| --- | --- |
| **Automatic Flydigi gyro correction** | Recognizes tested Apex 4 and Vader 4 Pro descriptor signatures and applies the expected axis transform without manually choosing one. |
| **Manual mapping selection** | Choose **Automatic**, **Identity** (unaltered axes), **Flydigi XYZ**, or your own profile for a connected controller. Useful when auto-detection isn't appropriate. |
| **Named custom gyro profiles** | Create, edit, copy, delete and reuse profiles with a different source for each output axis and optional inversion. |
| **Independent horizontal and vertical sensitivity** | Fine-tune yaw and pitch separately from **0.10× to 3.00×** within a custom profile. |
| **Per-controller assignments** | Mapping selection is stored against the Android input-device descriptor, rather than applying one global correction to every gamepad. |
| **Optional live gyro diagnostics** | Inspect reported motion values and identify attached controllers while streaming. Record diagnostic data and share it as CSV when investigating a problem. |
| **Separate, updateable Android app** | Installs under `com.butterpollo.client.custom`, so it can coexist with official Nightjar. Custom releases use a persistent signing identity for future updates. |

The Flydigi-specific automatic behavior is based on the two tested controller descriptor signatures. **Other controllers are not automatically assumed to use the same axis correction.** Instead, the manual mapper makes the feature useful for further testing and different hardware.

### The problem, visualized

```text
Controller motion                 Incorrectly interpreted motion
Turn left / right     ────────►   Camera looks up / down
Tilt up / down        ────────►   Camera turns left / right

Nightjar Custom
Android gyro input    ────────►   Controller-specific axis mapping
                                ├─ swap source axes
                                ├─ invert directions where needed
                                └─ tune horizontal and vertical gain
                                ────────►   Corrected motion sent to the host
```

The mapper changes how **controller motion input** is interpreted by Nightjar before that motion is forwarded to the PC. It does **not** remap physical thumbsticks, require game modification, or promise that every PC game will use gyro natively. The host/game must support the resulting motion input, directly or through an input-mapping layer such as Steam Input.

## Install

### Getting started

1. **Download the universal Android APK** from the button above or the [release page](https://github.com/jvanderv42/nightjar/releases/tag/custom-5.2.0-1).
2. Install it on an Android handheld, phone, tablet or Android TV device. Android may ask you to allow installation from the browser or file manager.
3. Run a **Moonlight-compatible host** on your PC—such as [Sunshine](https://github.com/LizardByte/Sunshine), [Apollo](https://github.com/ClassicOldSong/Apollo), [Vibepollo](https://github.com/Nonary/Vibepollo) or [Rubylight](https://github.com/RamazanKara/Rubylight).
4. Open Nightjar Custom, discover or manually add the PC, and complete the host's normal PIN pairing process.
5. Connect your gamepad in a mode that actually exposes its motion sensors to Android. Enable **Motion controls** under **Settings → Controls → More settings** if needed.
6. Under **Settings → Controls → More settings → Gyroscope Mapping**, select your controller and leave **Automatic** enabled for a recognized Flydigi device. If motion behaves incorrectly, choose a built-in mapping or create a custom profile.
7. Start a new stream after changing a mapping. Use the optional diagnostics overlay to inspect motion input if necessary.

**Flydigi tip:** During initial testing, the Apex 4 and Vader 4 Pro exposed motion through Switch/Pro Controller mode; Android Bluetooth/Xbox-style modes do not necessarily expose the same sensors. The best connection mode can depend on your controller firmware and Android device.

### Example: correcting another controller

Open **Gyroscope Mapping → your connected controller → Create new custom profile**. Give the profile a name, then assign the input source and sign for each output axis. Each source axis is used once. Adjust horizontal and vertical sensitivity separately, save the profile, assign it, and **start a new stream**.

You can keep different mappings for different controllers, reuse named profiles and return to Automatic at any time. If you can't tell which axes are being reported, enable **Show Gyro Diagnostics** under **Settings → Advanced → Diagnostics**, reproduce the movement and record/share the diagnostic CSV. Diagnostics are **off by default** for normal play.

## What you still get from Nightjar

This fork preserves the underlying Nightjar Android streaming client rather than building a new streaming protocol. Its core capabilities include:

- **Moonlight-compatible PC streaming:** connect to a compatible host over your local network or a properly configured remote connection.
- **High-quality video:** hardware-assisted H.264, HEVC and AV1 where supported by the Android device and host; HDR and higher refresh rates depend on your screen, decoder and PC.
- **Controller flexibility:** gamepads, customizable button assignments, on-screen controls, touch, keyboard and mouse support.
- **Streaming controls and diagnostics:** adjustable resolution, frame rate and bitrate, performance overlays and network troubleshooting.
- **Host-specific extras:** Nightjar exposes additional capabilities when the paired host actually supports them.

For full descriptions of those *upstream Nightjar* capabilities, see the [original Nightjar project](https://github.com/RamazanKara/nightjar), its [features](docs/features.md), [settings reference](docs/settings.md) and [host compatibility notes](docs/PARITY-MATRIX.md). Not every feature is available on every device or host.

## Compatibility and testing

| Item | Status |
| --- | --- |
| Flydigi Apex 4 (Wukong edition) in Android Switch/Pro Controller mode | Gyro correction tested on the Astra 2 |
| Flydigi Vader 4 Pro in Android Switch/Pro Controller mode | Gyro correction tested on the Astra 2 |
| Custom profiles and manual overrides | Implemented; additional controller-specific field results welcome |
| Other Android phones, tablets, TV boxes and handhelds | Universal APK available; motion support varies by Android version, input driver and controller |
| CPU architectures | One universal APK packages ARM64, ARMv7, x86 and x86_64 native libraries |
| CI validation | Android build, unit tests, lint and native tests passed; universal APK and its signature verified |

**Android 5.0+** is the upstream minimum OS requirement, but Android's general motion-sensor support varies. Nightjar's controller motion settings target **Android 12+** and particular controller/driver combinations may require newer versions or USB support. The two Flydigi models above were tested in the stated configuration; packaging additional architectures is not the same as hands-on testing of every device.

## Why it installs separately

This is **not** a replacement APK for the official Nightjar installation. Its Android application ID is:

```text
com.butterpollo.client.custom
```

That means you can keep the original Nightjar installed, complete with its settings and paired PCs, while trying this version. The custom app maintains its **own** settings, pairing identity and controller profiles; you will pair and configure it once. Future versions of this custom fork can update it in place when they retain the same application ID, signing certificate and a compatible version code.

The custom build is **non-debuggable** and uses the visible app name **Nightjar** (without “Debug”). It is signed with the fork's own signing certificate, **not** the original Nightjar release certificate. It cannot be installed over the official application.

## Download

### Downloads and integrity

**Current public release:** [Nightjar Custom 5.2.0 — Universal Android](https://github.com/jvanderv42/nightjar/releases/tag/custom-5.2.0-1)

- **[Download universal APK](https://github.com/jvanderv42/nightjar/releases/download/custom-5.2.0-1/nightjar-5.2.0-custom-universal.apk)** (ARM64, ARMv7, x86, x86_64)
- **[Download SHA-256 checksum](https://github.com/jvanderv42/nightjar/releases/download/custom-5.2.0-1/SHA256SUMS.txt)**

The release APK was built by [GitHub Actions](https://github.com/jvanderv42/nightjar/actions/workflows/custom-nightjar-universal.yml) from this fork and validated for the expected package ID, signature and native streaming engines. **Releases** are the download source; you don't need to compile anything or download an automatically expiring Actions artifact.

## Under the hood

Nightjar receives motion reports from Android's input system. In this fork, the input-device descriptor can select a known controller mapping automatically. The selected mapping transforms the motion axes (including direction/sign), and a custom profile can additionally scale horizontal and vertical motion independently. Nightjar then forwards the processed motion through its normal game-streaming input path to the host.

This deliberately keeps the fix on the **client side**—you don't need a modified Vibepollo/Sunshine/Rubylight server just to select an axis mapping. Whether a PC game uses gyro as camera movement is still controlled by its input support and any host-side mapping you choose.

Developers and testers can inspect the [gyro mapping implementation](app/src/main/java/com/limelight/binding/input/GyroMappingPreferences.java), [profile editor](app/src/main/java/com/limelight/preferences/GyroProfileEditor.java) and [build/signing guide](docs/custom-apk.md).

## Feedback, contributions and limitations

If your gyro is upside down, rotated, drifting, missing, or stops responding, useful bug reports include the Android device and version, controller model and connection mode, Nightjar version, selected gyro mapping, host software and steps to reproduce. A diagnostics CSV can help, but **review it for information you don't want to share before attaching it publicly**.

Use [this fork's Issues](https://github.com/jvanderv42/nightjar/issues) for custom gyro profiles, diagnostics and builds. Upstream Nightjar issues unrelated to these modifications belong in [upstream Nightjar](https://github.com/RamazanKara/nightjar/issues).

Feature requests, tested controller mappings and reproducible reports are welcome. **Known limitations:** this fork has not been verified against every controller; changing a gyro profile normally requires a new stream; gyro aiming requires compatible host/game input handling. The GitHub APK is Android-only: desktop Nightjar source is present because this is a fork, but no custom desktop release is advertised here.

## Credits and license

**Nightjar Custom is an independent, unofficial community fork, not an official release or endorsement by the Nightjar developer.** The original Nightjar client, streaming engine, user interface and extensive feature set are the work of [RamazanKara and Nightjar contributors](https://github.com/RamazanKara/nightjar). This fork adds its custom Android motion-control features and build/distribution configuration on top of that foundation.

Nightjar itself builds on [Moonlight Android](https://github.com/moonlight-stream/moonlight-android) and other open-source components, with additional acknowledgements in [CONTRIBUTORS.md](CONTRIBUTORS.md) and the [upstream project's README](https://github.com/RamazanKara/nightjar#license-and-credits).

Licensed under [GNU GPL-3.0](LICENSE.txt). Source code and build workflows are available in this repository.

---

<p align="center">
<strong>Want to try better gyro controls on your Android handheld?</strong><br><br>
<a href="https://github.com/jvanderv42/nightjar/releases/tag/custom-5.2.0-1"><strong>Download Nightjar Custom 5.2.0</strong></a>
&nbsp;·&nbsp;
<a href="https://github.com/jvanderv42/nightjar/issues">Report a controller issue</a>
&nbsp;·&nbsp;
<a href="https://github.com/RamazanKara/nightjar">Visit original Nightjar</a>
</p>
