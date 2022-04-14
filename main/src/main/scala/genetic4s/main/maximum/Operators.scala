/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.maximum

import monix.eval.Task
import java.util.Random

object Operators {
  def simulatedBinaryCrossover(distribution: Int)(x0: Float, x1: Float) =
    for {
      rnd <- Task { new Random() }
      generateFloat = Task { rnd.nextFloat() }
      mu <- generateFloat
      beta =
        if (mu <= .5f) Math.pow(2d * mu, 1d / (distribution + 1d)).toFloat
        else Math.pow(1d / (2d * (1d - mu)), 1d / (distribution + 1d)).toFloat
      nx0 = .5f * ((1f + beta) * x0 + (1f - beta) * x1)
      nx1 = .5f * ((1f - beta) * x0 + (1f + beta) * x1)
    } yield nx0 -> nx1
  def normallyDistributedMutation(distribution: Float)(x0: Float) =
    for {
      rnd <- Task { new Random() }
      d <- Task { rnd.nextGaussian() * distribution }
    } yield x0 + d
}
