# Sync Android Quickstart Application Overview

## Getting Started

Welcome to the Sync Demo application.  This application demonstrates a basic Tic-Tac-Toe client with the ability to make moves by any number of players, by synchronising the game state across the clients.

What you'll minimally need to get started:

- A clone of this repository
- A running instance of the backend quickstart of your choice ([Ruby](https://github.com/TwilioDevEd/sync-quickstart-ruby), [Python](https://github.com/TwilioDevEd/sync-quickstart-python), [Node.js](https://github.com/TwilioDevEd/sync-quickstart-node), [Java](https://github.com/TwilioDevEd/sync-quickstart-java), [C#](https://github.com/TwilioDevEd/sync-quickstart-csharp), or [PHP](https://github.com/TwilioDevEd/sync-quickstart-php)) to issue [Access Tokens](https://www.twilio.com/docs/api/sync/identity-and-access-tokens)
- A functioning Gradle installation

## Building

### Set up gradle wrapper to use correct gradle version.

Run
```
./gradlew wrapper
```

### Wire in your Token Service

Set the value of `ACCESS_TOKEN_SERVICE_URL` in sync-quickstart-android/gradle.properties file to point to a valid Access-Token server. If you're using one of the quickstarts to provide tokens, you'll probably want to use [`ngrok`](http://ngrok.io) to direct a publicly-accessible URL to your localhost service.

```bash
$ ngrok http localhost:4567
```

Your `gradle.properties` file should look something like this, borrowing from the output of the above.

```
ACCESS_TOKEN_SERVICE_URL=http://9349392.ngrok.io/token
```

You can also pass this address to gradle during the build.

```bash
$ gradle -ACCESS_TOKEN_SERVICE_URL=http://9349392.ngrok.io/token build
```

### Build

Run `gradle build` to fetch Twilio Sync SDK files and build application.

### Android Studio

You can import this project into Android Studio if you so desire by selecting `Import Project (Eclipse ADT, Gradle, etc)` from the menu and then build as you would ordinarily. The token server setup is still important.
