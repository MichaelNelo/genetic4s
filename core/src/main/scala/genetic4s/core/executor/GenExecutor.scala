/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core.executor

import cats.~>
import cats.syntax.all._
import genetic4s.core.dsl.GenOp
import cats.data.StateT
import genetic4s.core.algorithm.Genetic
import cats.Parallel
import java.util.Random
import cats.effect.Sync

final case class AlgorithmState[F[_], C](
  mutationRatio: Float,
  crossOverRatio: Float,
  populationSize: Int,
  random: Random,
  population: Vector[C],
  fitness: Genetic.Fitness[F, C],
  crossover: Genetic.Crossover[F, C],
  mutation: Genetic.Mutation[F, C]
)

class GenExecutor[F[_]: Sync: Parallel] extends Executor[F] {
  type Dependency = Unit

  private def select[C](
    size: Int,
    random: Random,
    population: Vector[C],
    fitness: Genetic.Fitness[F, C],
    ordering: Vector[C] => Vector[C]
  ): StateT[F, AlgorithmState[F, C], Vector[C]] =
    for {
      meassuredPopulation <- StateT.liftF { population.parTraverse(c => fitness(c) tupleLeft c) }
      (_, fitnesses) = meassuredPopulation.unzip
      sumFitness = fitnesses.sum
      firstQuarter = ordering(
        meassuredPopulation
          .sortBy(_._2)
          .map(_._1)
          .take(size / 4)
      )
      selectedPopulation <- StateT.liftF {
        meassuredPopulation
          .flatTraverse[F, C] {
            case (c, fitness) =>
              val rnd = Sync[F].delay { random.nextFloat }

              rnd.map { rng =>
                Vector(rng < ((sumFitness - fitness) / sumFitness))
                  .ifM(Vector(c), Vector.empty)
              }
          }
          .map { firstQuarter ++ _ }
      }
    } yield selectedPopulation

  private def nt[C] =
    Lambda[GenOp[F, C, *] ~> StateT[F, AlgorithmState[F, C], *]] {
      case GenOp.SelectToMinimize() =>
        for {
          state @ AlgorithmState(_, _, size, random, population, fitness, _, _) <- StateT.get
          selectedPopulation <- select(size, random, population, fitness, identity((_: Vector[C])))
          () <- StateT.set { state.copy(population = selectedPopulation) }
        } yield ()
      case GenOp.SelectToMaximize() =>
        for {
          state @ AlgorithmState(_, _, size, random, population, fitness, _, _) <- StateT.get
          selectedPopulation <- select(size, random, population, fitness, (_: Vector[C]).reverse)
          () <- StateT.set { state.copy(population = selectedPopulation) }
        } yield ()
      case GenOp.Crossover() =>
        def crossoverUntil(
          random: Random,
          crossoverRatio: Float,
          population: Vector[C],
          crossover: Genetic.Crossover[F, C],
          accum: Vector[C],
          size: Int
        ): Vector[F[Vector[C]]] = {
          val offspring = for {
            father <- population
            mother <- population
            effect = crossover(father, mother)
            child: F[Vector[C]] = Sync[F].delay { random.nextFloat }.flatMap { rng =>
              if (rng < crossoverRatio) effect.map {
                case (brother, sister) => Vector(brother, sister)
              }
              else
                Vector.empty.pure[F]
            }
          } yield child
          if (offspring.size >= size) offspring.take(size)
          else crossoverUntil(random, crossoverRatio, population ++ offspring, crossover, accum ++ offspring, size)
        }
        for {
          state @ AlgorithmState(_, crossoverRatio, size, random, population, _, crossover, _) <- StateT.get
          offspring <- StateT.liftF(
            crossoverUntil(random, crossoverRatio, population, crossover, Vector.empty, size).flatTraverse(identity)
          )
          () <- StateT.set(state.copy(population = offspring))
        } yield ()
      case GenOp.Mutate() =>
        for {
          state @ AlgorithmState(mutationRatio, _, _, random, population, _, _, mutate) <- StateT.get
          rng <- StateT.liftF { Sync[F].delay { random.nextFloat } }
          mutatedPopulation <- StateT.liftF {
            population.traverse { c => if (rng < mutationRatio) mutate(c) else c.pure[F] }
          }
          () <- StateT.set(state.copy(population = mutatedPopulation))
        } yield ()
      case GenOp.CurrentSolution() => StateT.inspect(_.population)
      case GenOp.Fittest() =>
        StateT.inspectF {
          case AlgorithmState(_, _, _, _, population, fitness, _, _) =>
            val fittest = population.head
            fitness(fittest) tupleLeft fittest
        }
    }

  def execute[C] =
    Lambda[GenInfo[F, C, *] ~> F] { fa =>
      fa.process.freeT
        .hoist(StateT.liftK[F, AlgorithmState[F, C]])
        .foldMap(nt)
        .runA(
          AlgorithmState(
            fa.mutationRation,
            fa.crossoverRatio,
            fa.populationSize,
            new Random(),
            fa.initialPopulation,
            fa.fitness,
            fa.crossover,
            fa.mutation
          )
        )
    }
}
