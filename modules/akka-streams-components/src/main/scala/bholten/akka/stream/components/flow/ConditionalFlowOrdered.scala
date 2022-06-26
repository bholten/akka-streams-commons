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

import akka.stream.scaladsl._
import akka.stream._
import bholten.akka.stream.components.fanout.Splitter

object ConditionalFlowOrdered {
  def apply[T, M](conditionalFlow: Flow[T, T, M])(
      predicate: T => Boolean
  ): Graph[FlowShape[T, T], M] =
    Flow.fromGraph(
      GraphDSL.createGraph(conditionalFlow) { implicit builder => flowShape =>
        import GraphDSL.Implicits._

        val zipWithIndex = builder.add(Flow[T].zipWithIndex)
        val split        = builder.add(Splitter((tl: (T, Long)) => predicate(tl._1)))
        val merge        = builder.add(MergeSequence[(T, Long)](inputPorts = 2)(_._2))
        val extract0     = builder.add(Flow[(T, Long)].map(_._1))
        val extract1     = builder.add(Flow[(T, Long)].map(_._2))
        val extract2     = builder.add(Flow[(T, Long)].map(_._1))
        val bcast        = builder.add(Broadcast[(T, Long)](outputPorts = 2))
        val zip          = builder.add(Zip[T, Long]())

          // format: OFF
          zipWithIndex ~> split.in
                          split.whenTrue   ~>    bcast ~> extract1        ~>       zip.in1
                                                 bcast ~> extract0 ~> flowShape ~> zip.in0
                                                      merge               <~       zip.out
                          split.whenFalse      ~>     merge
                                                      merge ~> extract2
          // format: ON

        FlowShape(zipWithIndex.in, extract2.out)
      }
    )
}
