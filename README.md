# Bluetooth LE heart rate monitor

Android application that tracks data coming from a heart rate monitor following the Bluetooth LE Generic Attribute Profile (GATT).

## Introduction

This project scan surrounding LE devices, asking the user to choose which one to connect to. The battery service and the heart rate profile are used the battery level and the heart rate. The heart rate data is then plotted to show the history. 

### Screenshots

### Prerequisites

* Android SDK 27
* Android Build Tools v27.0.3
* Android Support Repository
* Grade 3.1.1
* Java version 1.8
* JUnit 4.12

### Building the project

Clone this GitHub repository and open it into Android Studio. Be sure to have the prerequisites. This project uses the Gradle build system. To build this project, use ```gradlew build``` or ```Import Project``` in Android Studio. Android emulator does not have bluetooth capabilities, so please use a live device. You can use an application on another device to enable a heart rate monitor like LightBlue explorer which is available on [iOS](https://itunes.apple.com/us/app/lightblue-explorer/id557428110?mt=8) and [Android](https://play.google.com/store/apps/details?id=com.punchthrough.lightblueexplorer&hl=en). 

## Running the tests

## Project was based on
* [Android BluetoothLeGatt Google sample](https://github.com/googlesamples/android-BluetoothLeGatt)
* [The android usage examples](https://developer.android.com/guide/topics/connectivity/bluetooth-le.html)
