import com.merit.pushnotification.{ErrorResponse, ExpoClient, ExpoClientError, OkResponse, PushNotificationData, TicketResponse, ExpoPushNotificationRequest}

val body = "Body of the push notification"
val title = "Title of push notification"
// Provide the intended reciepients expo push token in the 'to' field.
// Push token look like ExponentPushToken[#TOKEN_VALUE#]
// or ExpoPushToken[#TOKEN_VALUE#]
val to = Set("ExponentPushToken[R6WKhnGQhIiXw9aspeIPsF]")

// PushNotificationData is available when the user acts on a push notification they receive
val data = PushNotificationData("extra info here")
val request = ExpoPushNotificationRequest(to, title, body, data)

// Sends a push notification
val sendEff = ExpoClient.sendExpoPushNotificationsEff(Seq(request))
val expoResponse: Either[ExpoClientError, Seq[TicketResponse]] = sendEff.unsafeRunSync


// Extracts the Expo Push Ticket ids from the response from the Expo server
val ticketIds: Seq[String] = expoResponse match {
  case Left(ExpoClientError(_)) => Vector.empty[String]
  case Right(ticketResponses) => ticketResponses.flatMap {
    case TicketResponse(OkResponse(_, id), _) => Some(id)
    case TicketResponse(ErrorResponse(_, _, _), _) => None
  }
}

// Later, process the Expo Push tickets to receive your Expo receipts.
// Verify that the push notifications were sent successfully
val processTicketsEff = ExpoClient.getPushNotificationReceiptsEff(ticketIds.toSet)
val receipts = processTicketsEff.unsafeRunSync