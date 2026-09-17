# LumaLight

LumaLight is an Android flashlight and screen-strobe app with configurable colors, brightness, frequency, and home-screen widgets.

## Features

- Solid screen flashlight mode.
- Screen strobe mode with independently configurable Color 1 and Color 2.
- Adjustable hue and brightness for each strobe color.
- Adjustable strobe frequency from 1–20 Hz.
- Optional rear-camera LED torch support when the device exposes a camera flash.
- Flashlight and strobe home-screen widgets.
- Widget configuration is saved per widget instance.
- Active lighting remains on until a volume key or the Android power/lifecycle interruption stops it; active overlays do not stop when touched.

## Requirements

- Android Studio with a JDK supported by the Android Gradle Plugin.
- Android SDK Platform 34.
- Android 6.0 (API 23) or newer.
- A camera permission is required only when using the optional rear-camera LED torch.
- A Gemini API key is required only if the corresponding AI features are enabled in the source configuration.

## Build and run

1. Clone the repository and open it in Android Studio.
2. Allow Gradle sync to finish.
3. If AI features are enabled, create a root `.env` file based on `.env.example` and set `GEMINI_API_KEY`.
4. Select a device or emulator running Android 6.0 or newer.
5. Run the `app` configuration.

To create a local release APK:

```bash
./gradlew assembleRelease
```

The APK is written to `app/build/outputs/apk/release/`.

## Using LumaLight

### Solid flashlight

1. Open the app and select **Solid**.
2. Choose a color and brightness.
3. Press **Activate**.
4. Press either volume button to stop. Turning the screen off with the physical power button also safely stops the active light.

### Two-color strobe

1. Select **Strobe**.
2. Configure **Color 1** and **Color 2** independently.
3. Set brightness for both colors and choose the frequency.
4. Optionally enable the camera LED after granting camera permission.
5. Press **Activate**.
6. Press either volume button to stop. Do not tap the active light to stop it.

### Strobe widget

1. Add the **LumaLight Strobe** widget from the Android widget picker.
2. During placement, choose Color 1 and Color 2 separately.
3. Press **Add Widget**.
4. Tap the widget to start the two-color strobe.
5. Press a volume button to stop it; screen-off/power lifecycle events stop it safely.

Existing widgets created with an older version keep their original color as Color 1 and use black as Color 2. Reconfigure or remove and add the widget again to choose both colors.

## Release APKs

Pushes to `main` build and update the rolling `latest` pre-release when GitHub Actions succeeds. Version tags such as `v1.0.1` create a named GitHub release. APKs are attached as release assets rather than committed to the repository.

## Permissions and safety

The app uses a wake lock while an active light session is running and includes a safety timeout. Camera permission is not needed for screen-only lighting. Strobe effects can be unsafe for people with photosensitive epilepsy; use responsibly and stop immediately if discomfort occurs.

## License

No license has been declared for this repository yet. All rights remain with the repository owner unless a license is added.
