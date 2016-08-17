package de.gitzoz.commonfunctions

import org.scalatest.WordSpecLike
import org.scalatest.Matchers
import akka.testkit.TestKit
import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.scaladsl.Flow
import scala.util.Success
import scala.util.Try
import akka.stream.scaladsl.Sink
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.ActorMaterializer
import scala.util.Failure
import akka.stream.scaladsl.Keep
import akka.stream.testkit.scaladsl.TestSource

class RetryFlowSpecs extends TestKit(ActorSystem("RetryFlowSpecs")) with WordSpecLike with Matchers {
  implicit val materializer = ActorMaterializer()
  "An RetryFlow" should {
    "send result without retry if there is no failure" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => Left(item))
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 1,
        testitem => (testitem.copy((testitem.retries + 1))))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(0))
    }

    "retry a given item one time" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 1)
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => {
          testitem.retries > 10
        },
        testitem => testitem.copy((testitem.retries + 1)))

      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(1))

    }

    "retry a given item two times" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 2)
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => testitem.copy((testitem.retries + 1)))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(2))

    }

    "retry a given item 10 times" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10)
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => testitem.copy((testitem.retries + 1)))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(10)
        .expectNext(TestItem(10))

    }

    "retry a given item 100 times" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 100)
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 100,
        testitem => testitem.copy((testitem.retries + 1)))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(100)
        .expectNext(TestItem(100))

    }
    
    "retry 3 items to 10 retries" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0),TestItem(5),TestItem(8)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10)
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 100,
        testitem => testitem.copy((testitem.retries + 1)))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(10))
        .request(1)
        .expectNext(TestItem(10))
        .request(1)
        .expectNext(TestItem(10))

    }
    
    "retry 3 items to but one will compeletly fail" in {
      val failedSink = Sink.foreach[TestItem] { x => () }
      val source = Source(List(TestItem(0),TestItem(20),TestItem(8)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10 || item.retries == 20 )
          Right(item)
        else
          Left(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => testitem.copy((testitem.retries + 1)))
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(10))
        .request(1)
        .expectNext(TestItem(10))

    }
  }
}

trait Retryable {
  val retries: Int
}
final case class TestItem(retries: Int) extends Retryable
final case class GrownUpTestItem(retries: Int, what: String = "a") extends Retryable