/*
 * Copyright (c) 2020 Merit International Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package com.merit.pushnotification

import java.nio.charset.Charset
import cats.effect.IO
import cats.syntax.either._
import cats.syntax.functor._
import com.merit.pushnotification.eff.EffFutureHelpers
import com.twitter.finagle.Http
import com.twitter.finagle.http.{RequestBuilder, Status}
import com.twitter.io.Buf
import io.circe.Decoder
import io.circe.generic.auto._
import io.circe.parser.decode
import io.circe.syntax._

/**
 * Helpful methods for sending push notifications using Expo, Scala, Cats and Circe.
 * @version 1.0
 * @author Kevin Strong <Kevin@gomerits.com>
 *
 */
object ExpoClient {
  private val Host = "exp.host"
  private val client = {
    Http
      .client
      .withTls(Host)
      .newService(s"$Host:443")
  }

  implicit val decodeTicketResponse: Decoder[TicketResponseJson] = (
    List[Decoder[TicketResponseJson]](
      Decoder[OkResponse].widen,
      Decoder[ErrorResponse].widen
    ).reduceLeft(_ or _)
  )

  implicit val decodeExpoReceiptResponse: Decoder[ReceiptResponseJson] = (
    List[Decoder[ReceiptResponseJson]](
      Decoder[OkReceiptResponse].widen,
      Decoder[ErrorResponse].widen
    ).reduceLeft(_ or _)
  )

  implicit val decodeExpoPushNotificationResponse: Decoder[ExpoPushNotificationResponse] = (
    List[Decoder[ExpoPushNotificationResponse]](
      Decoder[TicketResponses].widen,
      Decoder[ExpoErrors].widen
    ).reduceLeft(_ or _)
  )

  implicit val decodeExpoGetReceiptResponse: Decoder[ExpoGetReceiptResponse] = (
    List[Decoder[ExpoGetReceiptResponse]](
      Decoder[ReceiptResponses].widen,
      Decoder[ExpoErrors].widen
    ).reduceLeft(_ or _)
  )

  /**
   * Send push notifications
   * <p>
   * If you don't get an ExpoClientError, you are guaranteed there will be a ticket for each message sent,
   * in corresponding order they were passed in.
   * @param notificationRequests a Seq of ExpoPushNotificationRequests, one for each push notification you want to send.
   * @return Either an ExpoClientError if the request failed, or a list of TicketResponses
   *         which document the status of each push notification sent
   */
  def sendExpoPushNotificationsEff(
      notificationRequests: Seq[ExpoPushNotificationRequest]
  ): IO[Either[ExpoClientError, Seq[TicketResponse]]] = {
    val nonEmptyNotificationRequests = (
        notificationRequests
            .filter(_.to.nonEmpty)
        )
    if (nonEmptyNotificationRequests.isEmpty) {
      IO.pure(Right(Vector.empty))
    } else {
      // both this buffer ("content-encoding") and response ("accept-encoding" and decompression options on client)
      // can be gzipped, this is accepted by expo
      val requestString = nonEmptyNotificationRequests.asJson
      val buffer = Buf.Utf8(requestString.noSpaces)
      val request = (
        RequestBuilder()
          .url(s"https://$Host/--/api/v2/push/send")
          .setHeader("Content-Type", "application/json")
          .buildPost(buffer)
      )

      val requestEff = EffFutureHelpers.fromFuture(IO.delay(client(request)))

      for {
        response <- requestEff
      } yield {
        val contentString = Buf.decodeString(response.content, Charset.defaultCharset)

        val responseBodyResult = response.status match {
          case Status.Ok => {
            decode[ExpoPushNotificationResponse](contentString)
          }
          case _ => {
            Right(ExpoErrors(Vector(ExpoError(
              response.status.code,
              s"Received an error code from Expo server, with the following message: $contentString"
            ))))
          }
        }

        responseBodyResult.leftMap(error => ExpoClientError(error.toString)).flatMap({
          case TicketResponses(data) => {
            val actualMessagesCount = nonEmptyNotificationRequests.foldLeft(0)(_ + _.to.size)

            if (data.length == actualMessagesCount) {
              Right(
                data
                  .zip(nonEmptyNotificationRequests.flatMap(_.to))
                  .map({ case (ticketResponse, pushToken) => TicketResponse(ticketResponse, pushToken) })
              )
            } else {
              val ticketPluralize = if (actualMessagesCount == 1) "ticket" else "tickets"

              Left(ExpoClientError(
                s"Expected Expo to respond with $actualMessagesCount $ticketPluralize but got ${data.length}"
              ))
            }
          }
          case ExpoErrors(errors) => Left(ExpoClientError(errors.asJson.noSpaces))
        })
      }
    }
  }

  /**
   * Get push notification receipts from Expo for each push notification ticket id.
   * <p>
   * A push notification receipt contains final information about the status of a push notification, including
   * if the push notification sent successsfully or if any errors occurred.
   * <p>
   * If the ticket id does not exist on the server, it will not show up in the result map
   * @param ticketIds
   * @return Either an ExpoClientError if the request failed, or a map of ticket ids to ReceiptResponses,
   *        where each String represents the ticketId and the ReceiptResponse represents
   *        the final status of the push notification.
   */
  def getPushNotificationReceiptsEff(
    ticketIds: Set[String]
  ): IO[Either[ExpoClientError, Map[String, ReceiptResponseJson]]] = {
    if (ticketIds.isEmpty) {
      IO.pure(Right(Map.empty))
    } else {
      val requestString = ExpoGetReceiptRequest(ticketIds).asJson
      val buffer = Buf.Utf8(requestString.noSpaces)
      val request = (
        RequestBuilder()
          .url(s"https://$Host/--/api/v2/push/getReceipts")
          .setHeader("Content-Type", "application/json")
          .buildPost(buffer)
      )

      val requestEff = EffFutureHelpers.fromFuture(IO.delay(client(request)))

      for {
        response <- requestEff
      } yield {
        val contentString = Buf.decodeString(response.content, Charset.defaultCharset)

        val responseBodyResult = response.status match {
          // returns a map from the ticket id to the status of that ticket in the expo server
          case Status.Ok => decode[ExpoGetReceiptResponse](contentString)
          case _ => {
            Right(ExpoErrors(Vector(ExpoError(
              response.status.code,
              s"Received an error code from Expo server, with the following message: $contentString"
            ))))
          }
        }

        responseBodyResult.leftMap(error => ExpoClientError(error.toString)).flatMap({
          case ReceiptResponses(data) => Right(data)
          case ExpoErrors(errors) => Left(ExpoClientError(errors.asJson.noSpaces))
        })
      }
    }
  }
}
