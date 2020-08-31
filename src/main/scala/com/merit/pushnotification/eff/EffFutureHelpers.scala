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

package com.merit.pushnotification.eff

import cats.effect.IO
import com.twitter.util.{Future, Return, Throw}

object EffFutureHelpers {
  /**
   * A helper method to resolve an asynchronous Future when it is contained within an IO monad.
   * @param effect a function containing asynchronous function which returns a value
   * @return a synchronous (blocking) function which returns a value
   */
  def fromFuture[A](effect: IO[Future[A]]): IO[A] = {
    effect.flatMap(f => {
      f.poll match {
        case Some(Return(a)) => IO.pure(a)
        case Some(Throw(e)) => IO.raiseError(e)
        case None => {
          IO.async(callback => {
            f.respond({
              case Return(a) => callback(Right(a))
              case Throw(e) => callback(Left(e))
            })

            ()
          })
        }
      }
    })
  }
}
