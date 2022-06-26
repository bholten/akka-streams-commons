/*
 * Copyright (c) 2022 Brennan Holten
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of
 * this software and associated documentation files (the "Software"), to deal in
 * the Software without restriction, including without limitation the rights to
 * use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of
 * the Software, and to permit persons to whom the Software is furnished to do so,
 * subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS
 * FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR
 * COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER
 * IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN
 * CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package bholten.akka.stream.components.fanout

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl._

case class Splitter[T](in: Inlet[T], whenTrue: Outlet[T], whenFalse: Outlet[T]) extends Shape {
  override def inlets: Seq[Inlet[_]]   = Seq(in)
  override def outlets: Seq[Outlet[_]] = Seq(whenTrue, whenFalse)
  override def deepCopy(): Shape =
    Splitter(in.carbonCopy(), whenTrue.carbonCopy(), whenFalse.carbonCopy())
}

object Splitter {
  def apply[T](predicate: T => Boolean): Graph[Splitter[T], NotUsed] =
    GraphDSL.create() { implicit builder =>
      val part = builder.add(Partition[T](2, t => if (predicate(t)) 0 else 1))

      Splitter(part.in, part.out(0), part.out(1))
    }
}
