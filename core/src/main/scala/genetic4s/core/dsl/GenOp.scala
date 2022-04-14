/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core.dsl

sealed trait GenOp[F[_], C, A];

private[core] object GenOp {
  final case class SelectToMinimize[F[_], C]() extends GenOp[F, C, Unit]
  final case class SelectToMaximize[F[_], C]() extends GenOp[F, C, Unit]
  final case class Crossover[F[_], C]() extends GenOp[F, C, Unit]
  final case class Mutate[F[_], C]() extends GenOp[F, C, Unit]
  final case class CurrentSolution[F[_], C]() extends GenOp[F, C, Vector[C]]
  final case class Fittest[F[_], C]() extends GenOp[F, C, (C, Float)]
  // final case class SetMutationRatio[F[_], C](ratio: Int) extends GenOp[F, C, Unit]
  // final case class SetCrossoverRatio[F[_], C](ratio: Int) extends GenOp[F, C, Unit]
  // final case class HasElitism[F[_], C]() extends GenOp[F, C, Boolean]
}
