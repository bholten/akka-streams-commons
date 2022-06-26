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

import akka.actor.ActorSystem
import akka.stream.scaladsl._
import akka.stream.testkit.scaladsl._

import scala.concurrent._
import scala.concurrent.duration._

class ConditionalFlowUnorderedSuite extends munit.FunSuite {
  implicit val system: ActorSystem =
    ActorSystem("ConditionalFlowUnorderedSuit")
  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.global

  override def afterAll(): Unit = {
    Await.result(system.terminate(), 1.second)
    ()
  }

  test("route even numbers through a flow") {
    val source = TestSource.probe[Int]
    val cFlow  = ConditionalFlowUnordered(Flow[Int].map(_ * 10))(_ % 2 == 0)
    val sink   = TestSink.probe[Int]

    val materialized            = source.via(cFlow).toMat(sink)(Keep.both).run()
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
      .expectNextUnordered(1, 20, 3, 40, 5)
      .expectComplete()
  }

  test("route even numbers through a blocking flow") {
    val source = TestSource.probe[Int]
    val flow = Flow[Int].mapAsync(42) { n =>
      Future {
        blocking {
          Thread.sleep(100)
          n * 10
        }
      }
    }
    val cFlow = ConditionalFlowUnordered(flow)(_ % 2 == 0)
    val sink  = TestSink.probe[Int]

    val materialized            = source.via(cFlow).toMat(sink)(Keep.both).run()
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
      .expectNextUnordered(1, 20, 3, 40, 5)
      .expectComplete()
  }

  test("materialize a value") {
    val source = TestSource.probe[Int]
    val flow   = Flow[Int].map(_ * 10).mapMaterializedValue(_ => 10)
    val cFlow  = ConditionalFlowUnordered(flow)(_ % 2 == 0)
    val sink   = TestSink.probe[Int]

    val materialized = source.viaMat(cFlow)(Keep.both).toMat(sink)(Keep.both).run()
    val ((publisher, mat), subscriber) = materialized

    publisher
      .sendNext(1)
      .sendNext(2)
      .sendNext(3)
      .sendNext(4)
      .sendNext(5)
      .sendComplete()

    subscriber
      .request(5)
      .expectNextUnordered(1, 20, 3, 40, 5)
      .expectComplete()

    assert(mat == 10)
  }
}
