# InstaDraw

An Android app that turns any image into line strokes and draws them automatically on Instagram's chat draw screen, using your phone's accessibility gestures. It works fully on the phone. No internet, no server, nothing gets uploaded.

## Requirements

- Android 7.0 or newer (iPhones are not supported)
- The ability to install an APK from outside the Play Store

## Install

1. Download app-debug.apk (from the GitHub Actions artifact "InstaDraw", or from whoever sent it to you).
2. Open the file on your phone and allow "Install from unknown sources" if asked.
3. Open InstaDraw.

## Set up (one time)

1. Tap "1. Turn on accessibility".
2. Find InstaDraw in the list and switch it on.
3. If the switch is greyed out (Android 13 and newer): go to Settings > Apps > InstaDraw, tap the three dots at the top right, choose "Allow restricted settings", then try again.

The accessibility permission is only used to replay finger strokes on the screen. The app does not read your screen or collect any data.

## How to use

1. Tap "2. Pick image" and choose a picture.
2. Check the preview. It shows the lines that will be drawn, and how many strokes and roughly how many seconds it will take.
3. Adjust the sliders if needed:
   - Detail: move right for more lines, left for fewer.
   - Size: how wide the drawing is on screen.
4. Tap "3. Start". The app goes to the background and waits 10 seconds.
5. In that time, open an Instagram chat and go to the draw screen.
6. Don't touch the screen while it draws. Tap the "Stop" button in the app to cancel.

## Tips

- Simple images work best: logos, line art, doodles, simple portraits.
- Busy photos create thousands of strokes and take very long. Lower Detail for those.
- If the drawing gets cut off by Instagram's top or bottom toolbars, lower the Size slider.
- If the lines look messy, try a higher-contrast image or change Detail.

## How it works

1. The image is scaled down and edges are found by comparing neighbouring pixel brightness.
2. Connected edge pixels are followed to form stroke paths.
3. The strokes are scaled to fit the screen and replayed in small batches using Android's AccessibilityService gesture API (dispatchGesture).

## Build it yourself

The project builds on GitHub Actions. Push the files to a repo, open the Actions tab, run "build", and download the InstaDraw artifact. To build locally instead, open the project in Android Studio and run assembleDebug.

## Limitations

- Android only.
- Draws outlines only, so there are no colours or shading.
- The 10 second start delay and the drawing area are fixed in the code.
- Instagram can change its draw screen at any time, which may affect results.
