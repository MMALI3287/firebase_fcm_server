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

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.TopicManagementResponse;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

public class FcmSubscriptionManager {

  private static InputStream getServiceAccount() {
    return FcmSender.class.getClassLoader().getResourceAsStream("service-account.json");
  }

  private static void initFirebaseSDK() throws Exception {
    FirebaseOptions options =
        FirebaseOptions.builder()
            .setCredentials(GoogleCredentials.fromStream(getServiceAccount()))
            .build();

    FirebaseApp.initializeApp(options);
  }

  private static void subscribeFcmRegistrationTokensToTopic() throws Exception {
    List<String> registrationTokens =
        Arrays.asList(
            "cMSpfOPqQBuCFld6I7I8wc:APA91bERykm1Ksr7LlBVWuw5Lqs8fxgAib4oNIHsaSsjfEuCSuMGmRTatQnknkonzHP6I_sqrO_LMltYIDN9mH4C2WlOg2p9-AVyAcY-mxOZzE75t8DBPdQ"); // TODO: add FCM Registration Tokens to
    // subscribe
    String topicName = "app_promotion";

    TopicManagementResponse response =
        FirebaseMessaging.getInstance().subscribeToTopic(registrationTokens, topicName);
    System.out.printf("Num tokens successfully subscribed %d", response.getSuccessCount());
  }

  private static void unsubscribeFcmRegistrationTokensFromTopic() throws Exception {
    List<String> registrationTokens =
        Arrays.asList(
            "cMSpfOPqQBuCFld6I7I8wc:APA91bERykm1Ksr7LlBVWuw5Lqs8fxgAib4oNIHsaSsjfEuCSuMGmRTatQnknkonzHP6I_sqrO_LMltYIDN9mH4C2WlOg2p9-AVyAcY-mxOZzE75t8DBPdQ"); // TODO: add FCM Registration Tokens to
    // unsubscribe
    String topicName = "app_promotion";

    TopicManagementResponse response =
        FirebaseMessaging.getInstance().unsubscribeFromTopic(registrationTokens, topicName);
    System.out.printf("Num tokens successfully unsubscribed %d", response.getSuccessCount());
  }

  public static void main(final String[] args) throws Exception {
    initFirebaseSDK();

    // Note: Enable the call you want to execute. Disable others.
    subscribeFcmRegistrationTokensToTopic();
    // unsubscribeFcmRegistrationTokensFromTopic();
  }
}
