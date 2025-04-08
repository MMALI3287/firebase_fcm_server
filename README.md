# Firebase FCM Server

A Java-based server application that listens for new polls/menus in a Firestore database and sends Firebase Cloud Messaging (FCM) notifications to users.

## Overview

This server application connects to a Firebase project and:

1. Listens for new documents added to the "polls" collection in Firestore
2. When a new poll is detected, it fetches all users with notifications enabled
3. Sends FCM notifications to these users with details about the new poll/menu
4. Includes deep linking data to open the specific poll in the mobile app

The server is designed to run continuously as a background service, either directly on a host machine or within a Docker container.

## Prerequisites

- Java 17 or higher
- Maven or Gradle for building
- Docker (optional, for containerized deployment)
- Firebase project with:
  - Firestore database
  - Firebase Cloud Messaging enabled
  - Service account credentials

## Setup

### 1. Firebase Service Account

1. Go to the [Firebase Console](https://console.firebase.google.com/)
2. Select your project
3. Navigate to Project Settings > Service accounts
4. Click "Generate new private key"
5. Save the JSON file as `service-account.json` in `src/main/resources/`

### 2. Configure Firestore

Ensure your Firestore database has the following collections:

- `polls`: Contains documents representing polls/menus
  - Each document should have fields:
    - `question`: String (the poll/menu title)
    - `options`: Array of strings (menu items)
    - `createdAt`: Timestamp

- `users`: Contains documents representing app users
  - Each document should have fields:
    - `fcm_token`: String (the FCM token for the user's device)
    - `notifications_enabled`: Boolean (whether the user has enabled notifications)

## Building and Running

### Using Maven

```bash
# Build the project
mvn clean package

# Run the application
java -jar target/firebase_fcm_server-1.0-SNAPSHOT-jar-with-dependencies.jar
```

Or use the provided script:

```bash
# Windows
run.bat

# Linux/Mac
./run.sh
```

### Using Gradle

```bash
# Build the project
./gradlew build

# Run the application
java -jar build/libs/server-1.0-SNAPSHOT.jar
```

### Using Docker

```bash
# Build and run with Docker
docker build -t fcm-server .
docker run -d --name fcm-server --restart unless-stopped fcm-server
```

Or use the provided script:

```bash
# Windows
deploy.bat
```

## Running as a Windows Service

The project includes an XML configuration file (`FCMService.xml`) that can be used with [WinSW](https://github.com/winsw/winsw) to run the application as a Windows service.

1. Download WinSW from the [releases page](https://github.com/winsw/winsw/releases)
2. Rename the downloaded executable to `winsw.exe` and place it in the same directory as `FCMService.xml`
3. Install and start the service:

```bash
winsw install FCMService.xml
winsw start FCMService
```

## Project Structure

```
firebase_fcm_server/
├── src/
│   └── main/
│       ├── java/
│       │   └── com/
│       │       └── flutter/
│       │           └── fcm/
│       │               └── server/
│       │                   ├── FcmSender.java             # Main application class
│       │                   └── FcmSubscriptionManager.java # Subscription management
│       └── resources/
│           └── service-account.json                       # Firebase credentials (not in Git)
├── .gitignore
├── build.gradle                                           # Gradle build configuration
├── deploy.bat                                             # Docker deployment script
├── Dockerfile                                             # Docker configuration
├── FCMService.xml                                         # Windows service configuration
├── gradlew                                                # Gradle wrapper script (Unix)
├── gradlew.bat                                            # Gradle wrapper script (Windows)
├── pom.xml                                                # Maven build configuration
├── README.md                                              # This file
├── run.bat                                                # Run script (Windows)
└── run.sh                                                 # Run script (Unix)
```

## Troubleshooting

### Invalid JWT Signature

If you see an error like:
```
Error getting access token for service account: 400 Bad Request
POST https://oauth2.googleapis.com/token
{"error":"invalid_grant","error_description":"Invalid JWT Signature."}
```

This usually means:
1. The service account key file is invalid or corrupted
2. The service account credentials have expired or been revoked
3. There's a clock synchronization issue between your machine and Google's servers

Solution: Generate a new service account key from the Firebase Console.

### Class Not Found Exceptions

If you encounter `ClassNotFoundException` or `NoClassDefFoundError`, ensure you're using the correct JAR file:
- For Maven: `target/firebase_fcm_server-1.0-SNAPSHOT-jar-with-dependencies.jar`
- For Gradle: `build/libs/server-1.0-SNAPSHOT.jar`

## License

This project is licensed under the Apache License 2.0 - see the comments in source files for details.

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request.
