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

import io.circe.Json

case class ExpoPushToken(token: String)
case class PushNotificationData(data: String)
case class ExpoPushNotificationRequest(to: Set[String], title: String, body: String, data: PushNotificationData)

case class ExpoGetReceiptRequest(ids: Set[String])

sealed trait TicketResponseJson
sealed trait ReceiptResponseJson
case class OkResponse(status: String, id: String) extends TicketResponseJson
case class ErrorResponse(status: String, message: String, details: Json) extends TicketResponseJson with ReceiptResponseJson
case class OkReceiptResponse(status: String, details: Option[Json]) extends ReceiptResponseJson

case class TicketResponse(response: TicketResponseJson, pushToken: String)

sealed trait ExpoPushNotificationResponse
sealed trait ExpoGetReceiptResponse
case class TicketResponses(data: Seq[TicketResponseJson]) extends ExpoPushNotificationResponse
case class ExpoErrors(errors: Seq[ExpoError]) extends ExpoPushNotificationResponse with ExpoGetReceiptResponse
case class ExpoError(code: Int, message: String)

case class ReceiptResponses(data: Map[String, ReceiptResponseJson]) extends ExpoGetReceiptResponse

case class ExpoClientError(message: String)