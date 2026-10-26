# SpaceSync-AR - Dimension-Verified Furniture Fit Checker

An Android augmented reality (AR) app that helps you decide **before you buy**
whether a piece of furniture will fit in your room.

You measure the free space with your phone camera, type the furniture size from
the online shop, and the app shows the furniture at its real size in your room
with a clear answer: **CAN BUY** or **CANNOT BUY**.

## Features
- **Measure** - tap two points on the floor to get the real distance.
- **Fit Check** - choose Sofa, Chair, Cupboard or Other (box), type the length, width and height, then place it in AR.
- **Live verdict** - green *CAN BUY*, red *CANNOT BUY*, or orange *SIZE OK* (fits, but move it between the two points).
- **Move and rotate** - drag with one finger, twist with two fingers. Resizing by pinch is disabled on purpose so the result cannot be faked.
- **Saved Rooms** - save a room layout and view it later as a 2D floor plan.
- Works offline. No paid APIs, no login.

## How it works
1. ARCore detects the floor and turns each screen tap into a real-world point.
2. The distance between the two points is the free space (the "gap").
3. The furniture is drawn as boxes scaled to the exact size you typed.
4. The app checks how much room the furniture takes along the gap at its current angle,
   and whether it sits between the two points.

## Technologies
- Java 17, Android Studio, Gradle
- Google ARCore 1.54
- OpenGL ES 3.0 and Wavefront OBJ models
- Android SDK (AppCompat, Material), custom Canvas views
- SharedPreferences with JSON for saved rooms

## Requirements
- A phone that supports ARCore (see https://developers.google.com/ar/devices) running Android 7.0 or higher
- Google Play Services for AR installed (the app prompts for it)
- Android Studio with a recent Android SDK

## How to run
1. Clone the repository:
   `git clone https://github.com/Mansi-Maheswaran/SpaceSync-AR.git`
2. Open the folder in Android Studio and wait for Gradle sync.
3. Turn on **USB debugging** on your phone and connect it.
4. Press **Run**.
5. Point the camera at the floor and move slowly until white dots appear.

## How to use
1. Open the app and tap **New Room Scan**.
2. Choose **Measure** and tap two points on the floor.
3. Tap **Fit Check**, pick the furniture, type its size and tap **Place**.
4. Tap the floor to place it. Read the verdict at the top.
5. Drag or twist the furniture to try other positions.
6. Tap **Save** to keep the layout. Find it under **Saved Rooms**.

## Tips and known limits
- ARCore needs surface texture. Plain white walls or shiny tiles may not be detected - place a newspaper or rug on the floor to help.
- Measurements can drift by a few centimetres, as with any phone-camera AR.
- Saved rooms reopen as a 2D plan, not back in AR, because ARCore cannot recognise the same real room later without cloud anchors.

## Project structure
```
app/src/main/java/com/truefitar/
  measurement/   two-point distance measurement
  fitcheck/      furniture types, fit maths, AR drawing
  storage/       saved rooms (SharedPreferences + JSON)
  ui/            launcher, saved rooms, dialogs, 2D floor plan
```
