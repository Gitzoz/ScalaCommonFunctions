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

import akka.stream.Attributes
import akka.stream.FanOutShape
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.UniformFanOutShape
import akka.stream.stage.GraphStage
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler

object RoundRobinStage {
  def apply[T](nrOfOutlets: Int) = new RoundRobinStage[T](nrOfOutlets)
}

/**
  * A very simple round robin implementation in a Akka Stream Stage.
  * It will send one item to one outlet.
  * @param nrOfOutlets Number of outlets this stage have.
  */
final class RoundRobinStage[T](nrOfOutlets: Int)
    extends GraphStage[UniformFanOutShape[T, T]] {
  val in      = Inlet[T]("in")
  val outlets = (0 to (nrOfOutlets - 1)).map(idx => Outlet[T](s"$idx"))
  override val shape =
    new UniformFanOutShape(outlets.size, FanOutShape.Ports(in, outlets.toList))

  override def createLogic(inheritedAttributes: Attributes) =
    new GraphStageLogic(shape) {
      var outletToogle = 0

      setHandler(in, new InHandler {
        override def onPush() = {
          emit(outlets(outletToogle), grab(in))
          outletToogle = (outletToogle + 1) % nrOfOutlets
        }
      })

      outlets.foreach { outlet =>
        setHandler(outlet, new OutHandler {
          override def onPull() = if (!hasBeenPulled(in)) pull(in)
        })
      }
    }
}
