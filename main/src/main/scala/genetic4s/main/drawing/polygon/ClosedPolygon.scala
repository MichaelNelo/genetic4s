/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.drawing.polygon

import genetic4s.main.drawing.dsl.Path

final case class ClosedPolygon(point: (Int, Int), steps: Vector[(Int, Int)]) {
  def toDsl = {
    Path.closed(point, steps)
  }
}
