/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main

import cats.effect.ExitCode
import cats.syntax.all._
import monix.eval.Task
import monix.eval.TaskApp
import genetic4s.main.drawing.javafx.Painter
import genetic4s.core.algorithm.Genetic
import genetic4s.main.rng.ClosedPolygonGen
import genetic4s.main.drawing.polygon.ClosedPolygon

object Main extends TaskApp {
  override def run(args: List[String]): Task[ExitCode] =
    Painter.mkWindow(500, 500, "Circle AWT").use {
      case (painter, refresh) =>
        for {
          initialPopulation <- Vector.fill(20) { ClosedPolygonGen.gen(500) }.sequence
          _ <- Genetic(
            AlgorithmUtils.fitnessOfClosedPolygon((_: ClosedPolygon)),
            AlgorithmUtils.crossover(2f),
            AlgorithmUtils.mutate(1f),
            initialPopulation.size,
            crossoverRatio = .65f,
            mutationRatio = .05f
          ).load(AlgorithmUtils.geneticLoop(.0001f, painter, refresh)).run(initialPopulation)
        } yield ExitCode.Success
    }
}
