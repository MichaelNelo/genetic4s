/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.drawing.dsl

sealed trait PathOp[A]

object PathOp {
  final case class Point(x: Int, y: Int) extends PathOp[Unit]
  final case class Step(dx: Int, dy: Int) extends PathOp[(Int, Int)]
}
