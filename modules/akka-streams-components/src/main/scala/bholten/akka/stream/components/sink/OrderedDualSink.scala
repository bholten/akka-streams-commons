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

package bholten.akka.stream.components.sink

import akka.stream.Graph
import akka.stream.scaladsl._

import scala.concurrent.{ Future, Promise }

object OrderedDualSink {
  def apply[I1, I2, M1, M2, M3](sink0: Sink[I1, M1], sink1: Sink[I2, M2])(
      combineMat: (M1, Future[M2]) => M3
  ): Graph[DualSink[I1, I2], M3] = {
    val p0 = Promise[Sink[I2, M2]]()
    val sinkEnriched0 = sink0.mapMaterializedValue { m1 =>
      p0.success(sink1)
      m1
    }
    val sinkEnriched1 = Sink.futureSink(p0.future)

    DualSink(sinkEnriched0, sinkEnriched1)(combineMat)
  }
}
