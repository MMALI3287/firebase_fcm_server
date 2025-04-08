#!/bin/bash
echo "Building with Maven..."
mvn clean package

echo ""
echo "Running application..."
java -jar target/firebase_fcm_server-1.0-SNAPSHOT-jar-with-dependencies.jar
