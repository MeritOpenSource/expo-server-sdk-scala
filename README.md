# ExpoClient
A Scala and Cats based client for sending Expo based push notifications

----

This repo has two noteworthy methods, both in ExpoClient.scala, that are useful to anyone who wants to send push notifications using Expo.
  1. sendExpoPushNotificationsEff
  2. getPushNotificationReceiptsEff

sendExpoPushNotificationsEff sends a push notification

getPushNotificationReceiptsEff will query expo for the status of your push notifications

---

Look at example_usage.sc for a code example of sending push notifications.