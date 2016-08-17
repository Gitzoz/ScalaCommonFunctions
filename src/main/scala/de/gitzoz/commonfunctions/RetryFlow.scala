package de.gitzoz.commonfunctions

import akka.stream.FlowShape
import akka.stream.scaladsl.GraphDSL
import akka.stream.stage.GraphStage
import akka.stream.FanOutShape2
import akka.stream.Inlet
import akka.stream.Outlet
import akka.stream.Attributes
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

object RetryFlow {
  def apply[In, Out <: In](businessFlow: Flow[In, Either[Out, Out], NotUsed], finallyFailedCondition: In => Boolean, onIncrementRetry: Out => Out) = {
    val graph = GraphDSL.create() { implicit b =>
      import GraphDSL.Implicits._
      val merger = b.add(MergePreferred[In](1))
      val switch = b.add(new EitherSwitch[Out]())
      val retryLimiter = b.add(new RetryLimiter[Out](finallyFailedCondition, onIncrementRetry))

      merger.out ~> businessFlow ~> switch.in
      switch.out1 ~> retryLimiter ~> merger.preferred

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
          case Left(success)  => emit(outSuccess, success)
          case Right(failure) => emit(outFailure, failure)
        }
      }
    })

    setHandler(outSuccess, new OutHandler {
      override def onPull() = {
        pull(in)
      }
    })

    setHandler(outFailure, new OutHandler {
      override def onPull() = {
        pull(in)
      }
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

