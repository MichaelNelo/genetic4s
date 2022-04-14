/*
 * Copyright (c) 2020 Michael Nelo.
 * All rights reserved.
 */

package genetic4s.main.drawing.javafx

import cats.~>
import cats.syntax.all._
import genetic4s.main.drawing.dsl.PathOp
import cats.data.StateT
import genetic4s.main.drawing.dsl.PathOp.Point
import genetic4s.main.drawing.dsl.PathOp.Step
import cats.effect.Sync
import cats.effect.Resource
import java.awt.image.BufferedImage
import javax.imageio.ImageIO
import java.io.File
import java.awt.Graphics
import java.awt.Color
import java.awt.BasicStroke
import javax.swing.JFrame
import cats.effect.Async
import java.awt.event.WindowAdapter
import java.awt.event.WindowEvent
import genetic4s.main.drawing.dsl.Path

class Painter[F[_]: Sync](g: Graphics) extends (Path ~> F) {
  private val ops = Lambda[PathOp ~> StateT[F, (Int, Int), *]] {
    case Point(x, y) => StateT.set((x, y))
    case Step(dx, dy) =>
      for {
        (x0, y0) <- StateT.get
        (x1, y1) <- StateT.pure((x0 + dx, y0 + dy))
        () <- StateT.liftF { Sync[F].delay { g.drawLine(x0, y0, x1, y1) } }
        () <- StateT.set((x1, y1))
      } yield (x1, y1)
  }
  def apply[A](path: Path[A]) = path.interpret(ops).runEmptyA
}

object Painter {
  def mkImage[F[_]: Sync](w: Int, h: Int, filename: String) =
    for {
      image <- Resource.make(Sync[F].delay { new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB) })(image =>
        Sync[F].delay { ImageIO.write(image, "png", new File(filename)) }.void
      )
      graphics <- Resource.make(Sync[F].delay { image.createGraphics })(g => Sync[F].delay { g.dispose })
      () <- Resource.eval { Sync[F].delay { graphics.setColor(Color.WHITE) } }
      () <- Resource.eval { Sync[F].delay { graphics.fillRect(0, 0, w, h) } }
      () <- Resource.eval { Sync[F].delay { graphics.setColor(Color.BLACK) } }
      () <- Resource.eval { Sync[F].delay { graphics.setStroke(new BasicStroke(2f)) } }
    } yield new Painter(graphics)
  def mkWindow[F[_]: Async](w: Int, h: Int, windowName: String) =
    for {
      frame: JFrame <- Resource.make(Sync[F].delay { new JFrame(windowName) }) { frame =>
        Async[F].async { cb =>
          frame.addWindowListener(new WindowAdapter() {
            override def windowClosing(evt: WindowEvent) = cb(().asRight)
          })
        }
      }
      () <- Resource.eval { Sync[F].delay { frame.setVisible(true) } }
      () <- Resource.eval { Sync[F].delay { frame.setSize(w, h) } }
      graphics <- Resource.make(Sync[F].delay { frame.getContentPane.getGraphics })(g => Sync[F].delay { g.dispose })
      refreshCanvas = () => {
        Sync[F].delay {
          graphics.clearRect(0, 0, w, h)
        }
      }
    } yield new Painter(graphics) -> refreshCanvas
}
