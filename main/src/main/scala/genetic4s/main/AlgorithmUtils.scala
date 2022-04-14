/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main

import cats.syntax.all._
import genetic4s.core.implicits._
import genetic4s.main.drawing.polygon.ClosedPolygon
import genetic4s.core.dsl.Gen
import cats.effect.Sync
import genetic4s.main.drawing.javafx.Painter
import scala.util.Random
import monix.eval.Task

object AlgorithmUtils {
  private def mutateTuple(deviation: Float): (Int, Int) => Task[(Int, Int)] = {
    case (x, y) =>
      val random = new Random()
      val gaussRng = Task { random.nextGaussian() }
      Task.map2(gaussRng, gaussRng) { (r1, r2) =>
        (x + r1 * deviation.toDouble).toInt -> (y + r2 * deviation.toDouble).toInt
      }
  }

  private def crossoverTuple(alpha: Float): ((Int, Int), (Int, Int)) => Task[((Int, Int), (Int, Int))] = {
    case ((x0, y0), (x1, y1)) =>
      val random = new Random()
      val nx0 = x0 min x1
      val nx1 = x0 max x1
      val ny0 = y0 min y1
      val ny1 = y0 max y1
      val xl = (nx0 - alpha * (nx1 - nx0))
      val xu = (nx1 + alpha * (nx1 - nx0))
      val yl = (ny0 - alpha * (ny1 - ny0))
      val yu = (ny1 + alpha * (ny1 - ny0))

      val generateDouble = Task { random.nextDouble() }

      Task.map4(
        generateDouble,
        generateDouble,
        generateDouble,
        generateDouble
      ) { (b1, b2, b3, b4) =>
        ((xl + (xu - xl) * b1).toInt -> (yl + (yu - yl) * b2).toInt) ->
          ((xl + (xu - xl) * b3).toInt -> (yl + (yu - yl) * b4).toInt)
      }
  }

  def mutate(deviation: Float): ClosedPolygon => Task[ClosedPolygon] = {
    case ClosedPolygon((x, y), steps) =>
      for {
        pos <- mutateTuple(deviation)(x, y)
        steps <- steps.traverse(mutateTuple(deviation).tupled)
      } yield ClosedPolygon(pos, steps)
  }
  def crossover(alpha: Float): (ClosedPolygon, ClosedPolygon) => Task[(ClosedPolygon, ClosedPolygon)] = {
    case (ClosedPolygon((x0, y0), steps0), ClosedPolygon((x1, y1), steps1)) =>
      for {
        (p1, p2) <- crossoverTuple(alpha)((x0, y0), (x1, y1))
        childSteps <- steps0.zip(steps1).traverse(crossoverTuple(alpha).tupled)
        (s1, s2) = childSteps.unzip
      } yield ClosedPolygon(p1, s1) -> ClosedPolygon(p2, s2)

  }

  def geneticLoop(
    goal: Float,
    painter: Painter[Task],
    refresh: () => Task[Unit]
  ): Gen[Task, ClosedPolygon, Float] =
    for {
      () <- Gen.selectToMinimize[Task, ClosedPolygon]
      () <- Gen.crossover[Task, ClosedPolygon]
      () <- Gen.mutate[Task, ClosedPolygon]
      population <- Gen.currentSolution[Task, ClosedPolygon]
      (fittest, fitness) <- Gen.fittest[Task, ClosedPolygon]
      () <- Gen.eval { refresh() }
      () <- Gen.eval { population.traverse(c => painter(c.toDsl)).void }
      fittest <-
        (
          if (fitness <= goal)
            Gen.pure[Task, ClosedPolygon, Float](fitness)
          else geneticLoop(goal, painter, refresh)
        )
    } yield fittest

  def fitnessOfClosedPolygon(closedPolygon: ClosedPolygon) =
    Task.delay {
      val diff = closedPolygon.steps
      val ddiff = closedPolygon.steps.scan((0, 0)) {
        case ((x0, y0), (x1, y1)) => (x1 - x0, y1 - y0)
      }

      val kdiff = diff
        .zip(ddiff)
        .map {
          case ((dx, dy), (ddx, ddy)) =>
            val k = (ddy * dx - dy * ddx) / Math.pow(Math.pow(dx.toDouble, 2) + Math.pow(dy.toDouble, 2), 3d / 2d)
            val diff = Math.pow(25d - (1d / k), 2)

            if (diff.isInfinity || diff.isNaN)
              Float.MaxValue
            else diff
        }
        .reduce(_ + _)

      val err = (kdiff / diff.size).toFloat

      println(kdiff)

      err
    }

  def circleDrawing(radius: Int) = {
    val circlePointsTop = (-radius)
      .to(radius)
      .map { x =>
        x -> -Math.sqrt(((radius * radius).toDouble - (x * x).toDouble)).toInt
      }
      .sortBy(_._1)
      .foldLeft(((0, 0), Vector.empty[(Int, Int)])) {
        case (((x0, y0), vector), (x1, y1)) => {
          val p = (x1 - x0, y1 - y0)
          ((x1, y1), vector.appended(p))
        }
      }
      ._2
    val circlePointsBottom = radius
      .to(-radius, -1)
      .map { x =>
        x -> -Math.sqrt(((radius * radius).toDouble - (x * x).toDouble)).toInt
      }
      .sortBy(_._1)
      .foldLeft(((250, 0), Vector.empty[(Int, Int)])) {
        case (((x0, y0), vector), (x1, y1)) => {
          val p = (x0 - x1, y0 - y1)
          ((x1, y1), vector.appended(p))
        }
      }
      ._2
    ClosedPolygon((50, 250), circlePointsTop.tail ++ circlePointsBottom.tail)
  }

  def drawPivot[F[_]: Sync](radius: Int, filename: String) = {
    val c = circleDrawing(radius)

    Painter.mkImage(500, 500, filename).use(painter => painter(c.toDsl)).as(c)
  }
}
