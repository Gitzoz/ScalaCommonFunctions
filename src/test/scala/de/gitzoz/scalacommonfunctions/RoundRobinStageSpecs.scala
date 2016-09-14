/*
 * Copyright 2016 Stefan Roehrbein
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.gitzoz.scalacommonfunctions

import akka.testkit.TestKit
import org.scalatest.BeforeAndAfterAll
import akka.stream.ActorMaterializer
import org.scalatest.Matchers
import org.scalatest.WordSpecLike
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.FlowShape
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.GraphDSL
import akka.stream.scaladsl.Merge
import akka.stream.testkit.scaladsl.TestSink

class RoundRobinStageSpecs
    extends TestKit(ActorSystem("RoundRobinStageSpecs"))
    with WordSpecLike
    with Matchers
    with BeforeAndAfterAll {
  implicit val materializer = ActorMaterializer()
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  private def createFlow(parallel: Int) =
    Flow.fromGraph(GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._

      val merger     = b.add(Merge[Item](parallel))
      val roundRobin = b.add(RoundRobinStage[Item](parallel))

      (0 to (parallel - 1)).foreach { idx =>
        roundRobin.out(idx) ~> Flow[Item].map(
          item => Item(s"${ item.msg }$idx")
        ) ~> merger.in(idx)
      }

      FlowShape(roundRobin.in, merger.out)
    })

  "An RoundRobinStage" should {
    "send items to all outlets" in {
      val items  = (1 to 4).map(idx => Item(s"item$idx-"))
      val source = Source(items)
      val flow   = createFlow(4)
      source
        .via(flow)
        .runWith(TestSink.probe[Item])
        .request(1)
        .expectNext(Item("item1-0"))
        .request(1)
        .expectNext(Item("item2-1"))
        .request(1)
        .expectNext(Item("item3-2"))
        .request(1)
        .expectNext(Item("item4-3"))
        .expectComplete()
    }

    "start again by 0 after 4 items were send" in {
      val items  = (1 to 5).map(idx => Item(s"item$idx-"))
      val source = Source(items)
      val flow   = createFlow(4)
      source
        .via(flow)
        .runWith(TestSink.probe[Item])
        .request(1)
        .expectNext(Item("item1-0"))
        .request(1)
        .expectNext(Item("item2-1"))
        .request(1)
        .expectNext(Item("item3-2"))
        .request(1)
        .expectNext(Item("item4-3"))
        .request(1)
        .expectNext(Item("item5-0"))
        .expectComplete()
    }
  }
}

final case class Item(msg: String)
