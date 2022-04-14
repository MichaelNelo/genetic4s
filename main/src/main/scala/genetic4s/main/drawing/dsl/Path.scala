/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.drawing.dsl

import cats.~>
import cats.syntax.all._
import cats.free.Free
import cats.Monad

final case class Path[A](private val op: Free[PathOp, A]) extends AnyVal {
  def interpret[F[_]: Monad](interpreter: PathOp ~> F) = op.foldMap(interpreter)
}

object Path {
  def drawPoint(x: Int, y: Int) = Path(Free.liftF(PathOp.Point(x, y)))
  def step(dx: Int, dy: Int) = Path(Free.liftF(PathOp.Step(dx, dy)))

  def closed[A](initialPoint: (Int, Int), steps: Vector[(Int, Int)]) =
    Path(for {
      () <- drawPoint(initialPoint._1, initialPoint._2).op
      (x0, y0) = initialPoint
      differences <- steps.traverse { case (dx, dy) => step(dx, dy).op }
      (x1, y1) = differences.last
      _ <- step(x0 - x1, y0 - y1).op
    } yield (0, 0))
}
