/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main

import cats._
import cats.implicits._
import cats.effect._
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit
import java.time.Instant
import java.{util => ju}

object ExampleUtils {

  def sumAll[F[_]: Traverse, A: Monoid](list: F[A]): A =
    list.foldLeft(Monoid[A].empty)(Monoid[A].combine)

  def currentTime[F[_]: Sync](implicit clock: Clock[F]): F[LocalDateTime] =
    clock.realTime(TimeUnit.MILLISECONDS).map { ts =>
      LocalDateTime.ofInstant(
        Instant.ofEpochMilli(ts),
        ju.TimeZone.getDefault().toZoneId()
      )
    }
}
