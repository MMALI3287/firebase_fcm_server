@echo off
echo Building with Maven...
call mvn clean package

echo.
echo Running application...
java -jar target\firebase_fcm_server-1.0-SNAPSHOT-jar-with-dependencies.jar
