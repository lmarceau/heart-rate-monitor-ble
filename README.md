# Bluetooth LE heart rate monitor

Android application that tracks data coming from a heart rate monitor following the Bluetooth LE Generic Attribute Profile (GATT).

## Getting started

This project scan surrounding LE devices, asking the user to choose which one to connect to. The battery service and the heart rate profile are used the battery level and the heart rate. The heart rate data is then plotted to show the history. 

### Prerequisites

* Android SDK 27
* Android Build Tools v27.0.2
* Android Support Repository
* Grade 3.1.1
* Java version 1.8
* JUnit 4.12

### Screenshots

### Building the project

This project uses the Gradle build system. To build this project, use the "gradlew build" command or use "Import Project" in Android Studio.

## Running the tests

## Project was based on
* Google sample: https://github.com/googlesamples/android-BluetoothLeGatt
* The android usage examples: https://developer.android.com/guide/topics/connectivity/bluetooth-le.html
