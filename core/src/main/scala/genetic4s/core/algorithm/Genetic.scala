/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core.algorithm

import genetic4s.core.dsl.Gen
import genetic4s.core.executor.GenInfo
import genetic4s.core.executor.GenExecutor
import cats.Parallel
import cats.effect.Sync

object Genetic {
  type Crossover[F[_], C] = (C, C) => F[(C, C)]
  type Mutation[F[_], C] = C => F[C]
  type Fitness[F[_], C] = C => F[Float]

  final case class GeneticBuilderStep2[F[_]: Sync: Parallel, C, A] private (
    mutationRatio: Float,
    crossoverRatio: Float,
    populationSize: Int,
    process: Gen[F, C, A],
    fitness: Fitness[F, C],
    crossover: Crossover[F, C],
    mutation: Mutation[F, C]
  ) {
    def run(initialPopulation: Vector[C]) = {
      val executor = (new GenExecutor[F]()).execute[C]
      executor(
        GenInfo(mutationRatio, crossoverRatio, populationSize, initialPopulation, process, fitness, crossover, mutation)
      )
    }
  }
  final case class GeneticBuilderStep1[C, F[_]: Sync: Parallel] private (
    fitness: Genetic.Fitness[F, C],
    crossover: Genetic.Crossover[F, C],
    mutation: Genetic.Mutation[F, C],
    crossoverRatio: Float,
    mutationRation: Float,
    populationSize: Int
  ) {
    def load[A](process: Gen[F, C, A]) =
      GeneticBuilderStep2(mutationRation, crossoverRatio, populationSize, process, fitness, crossover, mutation)
  }
  def apply[F[_]: Sync: Parallel, C](
    fitness: Fitness[F, C],
    crossover: Crossover[F, C],
    mutation: Mutation[F, C],
    populationSize: Int,
    crossoverRatio: Float = 0.65.floatValue,
    mutationRatio: Float = 0.05.floatValue
  ) =
    GeneticBuilderStep1(fitness, crossover, mutation, crossoverRatio, mutationRatio, populationSize)
}
