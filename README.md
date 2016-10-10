# Sync Android Quickstart Application Overview

## Getting Started

Welcome to the Sync Demo application.  This application demonstrates a basic Tic-Tac-Toe client with the ability to make moves by any number of players, by synchronising the game state across the clients.

What you'll minimally need to get started:

- A clone of this repository
- [A way to create a Sync Service Instance and generate client tokens](https://www.twilio.com/docs/api/sync/identity-and-access-tokens)
- Gradle installation

## Building

### Set up gradle wrapper to use correct gradle version.

Run
```
./gradlew wrapper
```

### Set the value of `SERVER_TOKEN_URL`

Set the value of `SERVER_TOKEN_URL` in sync-quickstart-android/gradle.properties file to point to a valid Access-Token server.

Create that file if it doesn't exist with the following contents:

```
SERVER_TOKEN_URL=http://myserver.com/get-token/
```

NOTE: no need for quotes around the URL, they will be added automatically.

You can also pass this parameter to gradle during build without need to create a properties file, as follows:

```
gradle -PSERVER_TOKEN_URL=http://myserver.com/get-token/ build
```

### Build

Run `gradle build` to fetch Twilio Sync SDK files and build application.

### Android Studio

You can import this project into Android Studio if you so desire by selecting `Import Project (Eclipse ADT, Gradle, etc)` from the menu and then build using Studio's Build menu.
