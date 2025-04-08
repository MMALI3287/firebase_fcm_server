@echo off
echo Building Firebase FCM Server Docker image...
docker build -t fcm-server .

echo Starting FCM Server container...
docker run -d --name fcm-server --restart unless-stopped fcm-server

echo Container started. View logs with: docker logs -f fcm-server