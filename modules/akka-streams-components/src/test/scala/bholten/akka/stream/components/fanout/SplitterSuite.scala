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

import akka.actor.ActorSystem
import akka.stream.FlowShape
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._

import scala.concurrent.{ Await, ExecutionContext }
import scala.concurrent.duration._

class SplitterSuite extends munit.FunSuite {
  implicit val system: ActorSystem =
    ActorSystem("ConditionalFlowUnorderedSuit")
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 1.second)
    ()
  }

  test("Split even and odd numbers") {
    val source = TestSource.probe[Int]
    val sink   = TestSink.probe[Int]
    val flow = Flow.fromGraph(
      GraphDSL.create() { implicit builder =>
        import GraphDSL.Implicits._

        val split   = builder.add(Splitter((n: Int) => n % 2 == 0))
        val doubler = builder.add(Flow[Int].map(_ * 2))
        val merge   = builder.add(Merge[Int](inputPorts = 2))

          // format: OFF
          split.whenTrue  ~> doubler.in
                             doubler.out ~> merge.in(0)
          split.whenFalse        ~>         merge.in(1)
          // format: ON

        FlowShape(split.in, merge.out)
      }
    )

    val materialized            = source.via(flow).toMat(sink)(Keep.both).run()
    val (publisher, subscriber) = materialized

    publisher
      .sendNext(1)
      .sendNext(2)
      .sendNext(3)
      .sendNext(4)
      .sendNext(5)
      .sendComplete()

    subscriber
      .request(5)
      .expectNextUnordered(1, 4, 3, 8, 5)
      .expectComplete()
  }
}
