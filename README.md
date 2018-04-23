# Bluetooth LE heart rate monitor

Android application that tracks data coming from a heart rate monitor following the Bluetooth LE Generic Attribute Profile (GATT).

## Introduction

This project scan surrounding LE devices, asking the user to choose which one to connect to. It shows the name of the device and use the battery service and heart rate profile to display and update the battery level and the heart rate. The heart rate data is then plotted to show the history. At the moment, up to 120 values are shown in the heart rate history chart but this can be increased if needed :thumbsup:

### Screenshots
Device scan activity       |      Device activity
:-------------------------:|:-------------------------:
<img src="https://github.com/LaurieMarceau/Heart-rate-monitor-BLE/blob/master/screenshots/Screenshot_DeviceScanActivity.jpg" width="200">  | <img src="https://github.com/LaurieMarceau/Heart-rate-monitor-BLE/blob/master/screenshots/Screenshot_DeviceActivity_RealHXDevice.jpg" width="200">

### Prerequisites

* Android SDK 27
* Android Build Tools v27.0.3
* Grade 3.1.1
* Java version 1.8
* [MPAndroidChart 3.0.3](https://github.com/PhilJay/MPAndroidChart)
* JUnit 4.12

### Building the project

You can clone this GitHub repository and open it into Android Studio. Be sure to have the prerequisites. This project uses the Gradle build system. To build this project, use ```gradlew build``` or import a project in Android Studio. Android emulator does not have bluetooth capabilities so you have to use this app on a real device.

You can use an application on another device to enable a heart rate monitor like LightBlue explorer which is available on [iOS](https://itunes.apple.com/us/app/lightblue-explorer/id557428110?mt=8) and [Android](https://play.google.com/store/apps/details?id=com.punchthrough.lightblueexplorer&hl=en). Before using the heart rate monitor application, pairing your health device through the settings screen of your phone might be necessary. If you use a virtual heart rate monitor, be careful to pair the virtual device and not your actual phone.

## Tests

The application was tested with local and instrumented tests launched from Android Studio with Ctrl+Shift+F10. UI/Application Exerciser Monkey was also used to test the UI with 

```adb shell monkey -p lauriemarceau.heart_rate_monitor_ble -v 500```

This command sent pseudo-random events to the application. Firebase Test Lab for Android Robo Test was also used to test the application on different devices and assure intercompatibility. 

## Project was based on
* [Android BluetoothLeGatt Google sample](https://github.com/googlesamples/android-BluetoothLeGatt)
* [The Android usage examples](https://developer.android.com/guide/topics/connectivity/bluetooth-le.html)
* [MPAndroidChart Realtime LineChart example](https://github.com/PhilJay/MPAndroidChart/blob/master/MPChartExample/src/com/xxmassdeveloper/mpchartexample/RealtimeLineChartActivity.java)
