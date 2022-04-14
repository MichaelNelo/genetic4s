/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.rng

import cats.syntax.all._
import cats.effect.Sync
import cats.Parallel
import genetic4s.main.drawing.polygon.ClosedPolygon
import java.util.Random

object ClosedPolygonGen {
  def gen[F[_]: Sync: Parallel](size: Int) =
    for {
      random <- Sync[F].delay { new Random() }
      x <- Sync[F].delay { random.nextInt(500) }
      y <- Sync[F].delay { random.nextInt(500) }
      vector = Vector.fill(size) { Sync[F].delay { random.nextInt(5) -> random.nextInt(5) } }
      steps <- vector.parTraverse(identity)
    } yield ClosedPolygon((x, y), steps)
}
