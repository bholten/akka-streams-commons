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

package bholten.akka.stream.components.flow

import akka.stream._
import akka.stream.scaladsl._
import bholten.akka.stream.components.fanout.Splitter

object ConditionalFlowUnordered {
  def apply[T, M](conditionalFlow: Flow[T, T, M])(
      predicate: T => Boolean
  ): Graph[FlowShape[T, T], M] =
    Flow.fromGraph(
      GraphDSL.createGraph(conditionalFlow) { implicit builder => flowShape =>
        import GraphDSL.Implicits._

        val split = builder.add(Splitter[T](predicate))
        val merge = builder.add(Merge[T](inputPorts = 2))

          // format: OFF
          split.whenTrue ~> flowShape ~> merge
          split.whenFalse       ~>       merge
          // format: ON

        FlowShape(split.in, merge.out)
      }
    )
}
