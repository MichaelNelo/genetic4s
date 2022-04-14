/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core.dsl

import cats.free.FreeT
import cats.Applicative

final case class Gen[F[_], C, A](private[core] val op: FreeT[GenOp[F, C, *], F, A]) extends AnyVal {
  def flatMap[B](ff: A => Gen[F, C, B]) =
    Gen(op.flatMap(ff.andThen(_.freeT)))
  def freeT = op
}

object Gen {
  def selectToMinimize[F[_]: Applicative, C]: Gen[F, C, Unit] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.SelectToMinimize[F, C]()))
  def selectToMaximize[F[_]: Applicative, C]: Gen[F, C, Unit] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.SelectToMaximize[F, C]()))
  def crossover[F[_]: Applicative, C]: Gen[F, C, Unit] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.Crossover[F, C]()))
  def mutate[F[_]: Applicative, C]: Gen[F, C, Unit] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.Mutate[F, C]()))
  def currentSolution[F[_]: Applicative, C]: Gen[F, C, Vector[C]] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, Vector[C]](GenOp.CurrentSolution[F, C]()))
  def fittest[F[_]: Applicative, C]: Gen[F, C, (C, Float)] =
    Gen(FreeT.liftF[GenOp[F, C, *], F, (C, Float)](GenOp.Fittest[F, C]()))
  def eval[F[_]: Applicative, C, A](eval: => F[A]): Gen[F, C, A] = Gen(FreeT.liftT(eval))
  def pure[F[_]: Applicative, C, A](a: A): Gen[F, C, A] = Gen(FreeT.pure(a))
  def tailRecM[F[_]: Applicative, C, A, B](a: A)(f: A => Gen[F, C, Either[A, B]]) =
    Gen(FreeT.tailRecM(a)(f andThen (_.freeT)))
  // def setMutationRatio[F[_]: Applicative, C](ratio: Int): Gen[F, C, Unit] =
  //   Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.SetMutationRatio[F, C](ratio)))
  // def setCrossoverRatio[F[_]: Applicative, C](ratio: Int): Gen[F, C, Unit] =
  //   Gen(FreeT.liftF[GenOp[F, C, *], F, Unit](GenOp.SetCrossoverRatio[F, C](ratio)))
  // def mutateAll[F[_], C]: Gen[F, C, Unit] =
}
