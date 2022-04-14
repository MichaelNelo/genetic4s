/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.core

import cats.Monad
import cats.free.FreeT
import dsl.Gen
import cats.Applicative

package object implicits {
  implicit def monadForGen[F[_]: Applicative, C]: Monad[Gen[F, C, *]] =
    new Monad[Gen[F, C, *]] {
      def pure[A](a: A) = Gen(FreeT.pure(a))
      def flatMap[A, B](fa: Gen[F, C, A])(ff: A => Gen[F, C, B]) =
        fa.flatMap(ff)
      def tailRecM[A, B](a: A)(f: A => Gen[F, C, Either[A, B]]): Gen[F, C, B] = Gen.tailRecM(a)(f)
    }
}
