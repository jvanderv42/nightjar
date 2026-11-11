# Flydigi controller motion correction (development)

This branch is experimental. Do not merge into main or distribute an APK as a working fix until device testing confirms the axis mapping.

## Confirmed observations
- Flydigi APEX 4 and Vader 4 Pro in Switch Bluetooth mode expose gyro to Android.
- Gyro axes are swapped in SiN: Reloaded when streamed to Windows through Nightjar and through Moonlight Android.
- A real DualShock 4 paired to the same Android client works correctly with native gyro.
- Direct Windows connection of the Flydigi controllers in Switch mode works correctly in SiN.
- Switching Windows streaming hosts did not fix the Flydigi symptom.

These observations narrow the problem to the Switch-mode Android-to-streaming motion path, but do not prove which component is responsible.

## Current implementation
- `GyroAxisTransformer` supports axis permutation and sign inversion.
- `ControllerHandler.MotionListener` calls the transformer on gyroscope values after existing display-rotation correction and before radians-to-degrees conversion.
- The only selected mapping is identity. There is **no user-facing control or Flydigi-specific mapping yet**.
- Accelerometer forwarding remains unchanged.
- The working DualShock 4 path should remain behaviorally unchanged, subject to build and device verification.

## Next steps
1. Run `GyroAxisTransformerTest` and the existing controller input tests in an Android-capable build environment.
2. Add a diagnostic view or limited logging to capture sensor X/Y/Z and Android vendor/product identifiers for both the working DS4 and Flydigi controller. Avoid high-frequency unbounded logs.
3. Compare controlled pitch, yaw and roll movements with matching device orientations.
4. Add a controller-specific, opt-in mapping setting with an explicit identity/off option. Do not hardcode a swap based solely on the game camera's apparent directions.
5. Test native gyro in SiN: Reloaded, including correct signs, roll/yaw, no intermittent loss, and DS4 regression.
6. Consider an upstream pull request after confirming a generalizable fix.

## Build and signing
This branch does not contain upstream's release signing keys. A locally generated debug APK uses a different signing certificate and may not update an existing Nightjar install in place. Back up settings before testing, and do not replace the known-good app until validated.
