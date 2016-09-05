package de.gitzoz.commonfunctions

import scala.concurrent.duration.DurationInt
import scala.concurrent.duration.FiniteDuration

import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL
import akka.stream.stage.GraphStage
import akka.stream.FanOutShape2
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.Attributes
import akka.stream.DelayOverflowStrategy
import akka.stream.stage.GraphStageLogic
import akka.stream.stage.InHandler
import akka.stream.stage.OutHandler
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import akka.stream.scaladsl.Merge
import akka.stream.SinkShape
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Sink
import akka.NotUsed
import scala.concurrent.Future
import akka.Done
import akka.stream.scaladsl.MergePreferred
import akka.stream.impl.fusing.Delay

object RetryFlow {
/**
   * Generates a Flow which retries items until a given condition is meet.
   * @param businessFlow must end with an Either. Right is a success and Left is a Failure
   * @finallyFailedCondition when should a item discarded
   * @onIncrementRetry for example increment a counter on the item
   * @delayTime retry an item after this delay time
   */
  def apply[In, Out <: In](businessFlow: Flow[In, Either[Out, Out], NotUsed], finallyFailedCondition: In => Boolean, onIncrementRetry: Out => Out, delayTime: FiniteDuration = 5.seconds) = {
    val graph = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val merger = b.add(MergePreferred[In](1))
      val switch = b.add(new EitherSwitch[Out]())
      val retryLimiter = b.add(new RetryLimiter[Out](finallyFailedCondition, onIncrementRetry))
      val delay = b.add(new Delay[In](delayTime, DelayOverflowStrategy.emitEarly))

      merger.out ~> businessFlow ~> switch.in
      merger.preferred <~ delay <~ retryLimiter <~ switch.out1

      FlowShape(merger.in(0), switch.out0)
    }

    Flow.fromGraph(graph)
  }
}

final class EitherSwitch[A] extends GraphStage[FanOutShape2[Either[A, A], A, A]] {

  val in = Inlet[Either[A, A]]("in")
  val outSuccess = Outlet[A]("outSuccess")
  val outFailure = Outlet[A]("outFailure")
  override val shape = new FanOutShape2(in, outSuccess, outFailure)

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {

    setHandler(in, new InHandler {
      override def onPush() = {
        val item = grab(in)
        item match {
          case Left(failure)  => emit(outFailure, failure)
          case Right(success) => emit(outSuccess, success)
        }
      }
    })

    setHandler(outSuccess, new OutHandler {
      override def onPull() = if (!hasBeenPulled(in)) pull(in)
    })

    setHandler(outFailure, new OutHandler {
      override def onPull() = if (!hasBeenPulled(in)) pull(in)
    })
  }
}

final class RetryLimiter[A](finallyFailedCondition: A => Boolean, incrementRetry: A => A) extends GraphStage[FlowShape[A, A]] {
  override val shape = new FlowShape(Inlet[A]("in"), Outlet[A]("out"))

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    import shape._

    setHandler(in, new InHandler {
      override def onPush() = {
        val item = grab(in)
        if (!finallyFailedCondition(item))
          push(out, incrementRetry(item))
        else
          pull(in)
      }
    })

    setHandler(out, new OutHandler {
      override def onPull() = pull(in)
    })
  }
}

final class RetrySwitch[A](finallyFailedCondition: A => Boolean, incrementRetry: A => A) extends GraphStage[FanOutShape2[A, A, A]] {

  override val shape = new FanOutShape2(Inlet[A]("in"), Outlet[A]("outSuccess"), Outlet[A]("outFailure"))

  override def createLogic(inheritedAttributes: Attributes) = new GraphStageLogic(shape) {
    import shape._

    setHandler(in, new InHandler {
      override def onPush() = {
        val item = grab(in)
        if (finallyFailedCondition(item))
          emit(out1, item)
        else
          emit(out0, incrementRetry(item))
      }
    })

    setHandler(out0, new OutHandler {
      override def onPull() = pull(in)
    })

    setHandler(out1, new OutHandler {
      override def onPull() = pull(in)
    })
  }
}

