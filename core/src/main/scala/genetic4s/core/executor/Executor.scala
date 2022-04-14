/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core.executor

import cats.~>
import genetic4s.core.dsl.Gen
import genetic4s.core.algorithm.Genetic

final case class GenInfo[F[_], C, A](
  mutationRation: Float,
  crossoverRatio: Float,
  populationSize: Int,
  initialPopulation: Vector[C],
  process: Gen[F, C, A],
  fitness: Genetic.Fitness[F, C],
  crossover: Genetic.Crossover[F, C],
  mutation: Genetic.Mutation[F, C]
)

trait Executor[F[_]] {
  type Dependency
  def execute[C]: GenInfo[F, C, *] ~> F
}

object Executor {
  type Aux[F[_], A] = Executor[F] { type Dependency = A; }

  def apply[F[_]](implicit E: Executor[F]) = E
}
