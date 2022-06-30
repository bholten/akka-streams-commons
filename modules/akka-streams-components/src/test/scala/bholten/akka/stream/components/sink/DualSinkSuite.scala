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

import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._

import scala.concurrent.duration._
import scala.concurrent.{ Await, ExecutionContext }

class DualSinkSuite extends munit.FunSuite {
  implicit val system: ActorSystem =
    ActorSystem("DualSinkSuite")
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 1.second)
    ()
  }

  test("execute two sinks") {
    val source0  = TestSource.probe[Int]
    val source1  = TestSource.probe[Char]
    val sink0    = TestSink.probe[Int]
    val sink1    = TestSink.probe[Char]
    val dualSink = DualSink(sink0, sink1)((_, _))

    val graph =
      RunnableGraph.fromGraph(
        GraphDSL.createGraph(source0, source1, dualSink)((_, _, _)) {
          implicit builder => (sourceShape0, sourceShape1, ds) =>
            {
              import GraphDSL.Implicits._
              val f0 = builder.add(Flow[Int].map(_ * 10))
              val f1 = builder.add(Flow[Char].map(_.toUpper))

              // format: OFF
              sourceShape0.out ~> f0.in
                                  f0.out ~> ds.in0
              sourceShape1.out ~> f1.in
                                  f1.out ~> ds.in1
              // format: ON

              ClosedShape
            }
        }
      )

    val (pub0, pub1, (sub0, sub1)) = graph.run()

    pub0
      .sendNext(0)
      .sendNext(1)
      .sendNext(2)
      .sendNext(3)
      .sendNext(4)
      .sendComplete()

    pub1
      .sendNext('a')
      .sendNext('b')
      .sendNext('c')
      .sendNext('d')
      .sendNext('e')
      .sendComplete()

    sub0
      .request(5)
      .expectNext(0, 10, 20, 30, 40)
      .expectComplete()

    sub1
      .request(5)
      .expectNext('A', 'B', 'C', 'D', 'E')
      .expectComplete()
  }

  test("execute two sinks, without order") {
    val source0  = TestSource.probe[Int]
    val source1  = TestSource.probe[Char]
    val sink0    = TestSink.probe[Int]
    val sink1    = TestSink.probe[Char]
    val dualSink = DualSink(sink0, sink1)((_, _))

    val graph =
      RunnableGraph.fromGraph(
        GraphDSL.createGraph(source0, source1, dualSink)((_, _, _)) {
          implicit builder => (sourceShape0, sourceShape1, ds) =>
            {
              import GraphDSL.Implicits._
              val f0 = builder.add(Flow[Int].map(_ * 10))
              val f1 = builder.add(Flow[Char].map(_.toUpper))

            // format: OFF
            sourceShape0.out ~> f0.in
            f0.out ~> ds.in0
            sourceShape1.out ~> f1.in
            f1.out ~> ds.in1
            // format: ON

              ClosedShape
            }
        }
      )

    val (pub0, pub1, (sub0, sub1)) = graph.run()

    pub1
      .sendNext('a')
      .sendNext('b')
      .sendNext('c')

    pub0
      .sendNext(0)
      .sendNext(1)

    pub1
      .sendNext('d')
      .sendNext('e')
      .sendComplete()

    pub0
      .sendNext(2)
      .sendNext(3)
      .sendNext(4)
      .sendComplete()

    sub0
      .request(5)
      .expectNext(0, 10, 20, 30, 40)
      .expectComplete()

    sub1
      .request(5)
      .expectNext('A', 'B', 'C', 'D', 'E')
      .expectComplete()
  }
}
