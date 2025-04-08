/*
 * Copyright 2022 Google LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.flutter.fcm.server;

import com.android.identity.util.Timestamp;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.FirestoreException;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.AndroidConfig;
import com.google.firebase.messaging.AndroidNotification;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import java.io.InputStream;
import java.util.concurrent.ExecutionException;
import java.util.List;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.EventListener;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.QuerySnapshot;
import javax.annotation.Nullable;


public class FcmSender {
  private static Firestore db;
  private static com.google.cloud.Timestamp serverStartTime;


  private static InputStream getServiceAccount() {
    return FcmSender.class.getClassLoader().getResourceAsStream("service-account.json");
  }

  private static void initFirebaseSDK() throws Exception {
    FirebaseOptions options = FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(getServiceAccount()))
            .build();

    FirebaseApp.initializeApp(options);

    // Record server start time to avoid processing old documents
    serverStartTime = com.google.cloud.Timestamp.now();

    System.out.println("Server start time: " + serverStartTime);
  }

  private static void initFirestoreListener() throws Exception {
    FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
            .setCredentials(GoogleCredentials.fromStream(getServiceAccount()))
            .build();
    db = firestoreOptions.getService();

    // Only listen for new documents created after server start
    db.collection("polls")
            .whereGreaterThan("createdAt", serverStartTime)
            .addSnapshotListener(
                    new EventListener<QuerySnapshot>() {

                      @Override
                      public void onEvent(
                              @Nullable QuerySnapshot snapshots, @Nullable FirestoreException e) {
                        if (e != null) {
                          System.err.println("Listen failed: " + e);
                          return;
                        }

                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                          if (dc.getType() == DocumentChange.Type.ADDED) {
                            DocumentSnapshot document = dc.getDocument();
                            try {
                              // Extract poll data
                              String pollId = document.getId();
                              String question = document.getString("question");
                              com.google.cloud.Timestamp createdAt = document.getTimestamp("createdAt");

                              System.out.println("Processing new poll: " + pollId + " created at " + createdAt);

                              // Get options from the document
                              List<String> options = null;
                              try {
                                options = (List<String>) document.get("options");
                              } catch (Exception ex) {
                                System.err.println("Error extracting options: " + ex);
                              }

                              // Create notification content
                              String title = question;

                              // Create a preview of options (first 2-3)
                              StringBuilder contentBuilder = new StringBuilder("Menu: ");
                              if (options != null && !options.isEmpty()) {
                                int previewCount = Math.min(options.size(), 3); // Preview first 3 options
                                for (int i = 0; i < previewCount; i++) {
                                  contentBuilder.append(options.get(i));
                                  if (i < previewCount - 1) {
                                    contentBuilder.append(", ");
                                  }
                                }
                                if (options.size() > previewCount) {
                                  contentBuilder.append(" and more...");
                                }
                              } else {
                                contentBuilder = new StringBuilder("New menu available. Tap to vote!");
                              }
                              String content = contentBuilder.toString();

                              // Create data payload with poll ID for deep linking
                              Map<String, String> data = Map.of(
                                      "pollId", pollId,
                                      "type", "new_poll",
                                      "question", question != null ? question : ""
                              );

                              // Get tokens from your users collection
                              ApiFuture<QuerySnapshot> future = db.collection("users")
                                      .whereEqualTo("notifications_enabled", true)
                                      .get();

                              try {
                                // Use a set to ensure we only send to each token once
                                Set<String> processedTokens = new HashSet<>();

                                QuerySnapshot userSnapshots = future.get();
                                for (DocumentSnapshot user : userSnapshots.getDocuments()) {
                                  String token = user.getString("fcm_token");
                                  if (token != null && !token.isEmpty() && !processedTokens.contains(token)) {
                                    try {
                                      processedTokens.add(token); // Add to set to avoid duplicates
                                      sendPollNotification(token, title, content, data);
                                    } catch (Exception ex) {
                                      System.err.println("Failed to send notification: " + ex);
                                    }
                                  }
                                }
                                System.out.println("Sent notifications to " + processedTokens.size() + " unique devices");
                              } catch (InterruptedException | ExecutionException ex) {
                                System.err.println("Error fetching users: " + ex);
                              }
                            } catch (Exception ex) {
                              ex.printStackTrace();
                            }
                          }
                        }
                      }
                    });
  }

  private static void sendPollNotification(String token, String title, String content, Map<String, String> data) throws Exception {
    // Build message with notification and data payload
    Message.Builder messageBuilder = Message.builder()
            .setNotification(
                    Notification.builder()
                            .setTitle(title != null ? title : "New Menu Available")
                            .setBody(content != null ? content : "A new menu was added. Tap to vote!")
                            .build())
            .setToken(token);

    // Add data payload for deep linking
    if (data != null) {
      for (Map.Entry<String, String> entry : data.entrySet()) {
        messageBuilder.putData(entry.getKey(), entry.getValue());
      }
    }

    // Add Android specific configuration
    messageBuilder.setAndroidConfig(AndroidConfig.builder()
            .setNotification(AndroidNotification.builder()
                    .setClickAction("OPEN_POLL_ACTIVITY")
                    .build())
            .build());

    Message message = messageBuilder.build();
    FirebaseMessaging.getInstance().send(message);
    System.out.println("Poll notification sent to token: " + token);
  }

  public static void main(final String[] args) throws Exception {
    try {
      initFirebaseSDK();
      initFirestoreListener();
      System.out.println("Menu Notification Server started. Listening for new menus...");
      Thread.sleep(Long.MAX_VALUE);
    } catch (Exception e) {
      e.printStackTrace();  // Print detailed error
    }
  }
}