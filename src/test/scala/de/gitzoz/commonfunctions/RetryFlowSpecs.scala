package de.gitzoz.commonfunctions

import scala.Left
import scala.Right
import scala.concurrent.duration.DurationInt

import org.scalatest.BeforeAndAfterAll
import org.scalatest.Matchers
import org.scalatest.WordSpecLike

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.testkit.TestKit

class RetryFlowSpecs extends TestKit(ActorSystem("RetryFlowSpecs")) with WordSpecLike with Matchers with BeforeAndAfterAll {
  implicit val materializer = ActorMaterializer()
  override def afterAll {
    TestKit.shutdownActorSystem(system)
  }

  "An RetryFlow" should {
    "send result without retry if there is no failure" in {
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => Right(item))
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 1,
        testitem => TestItem(testitem.retries + 1), 10.millis)
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(0))
    }

    "retry a given item one time" in {
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 1)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => {
          testitem.retries > 10
        },
        testitem => TestItem(testitem.retries + 1), 10.millis)

      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(1))

    }

    "retry a given item two times" in {
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 2)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => TestItem(testitem.retries + 1), 10.millis)
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(2))

    }

    "retry a given item 10 times" in {
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => TestItem(testitem.retries + 1), 10.millis)
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(10))

    }

    "retry a given item 100 times" in {
      val source = Source(List(TestItem(0)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 100)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 100,
        testitem => TestItem(testitem.retries + 1), 10.millis)
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(1)
        .expectNext(TestItem(100))

    }

    "retry 3 items to 10 retries" in {
      val source = Source(List(TestItem(0), TestItem(5), TestItem(8)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 100,
        testitem => TestItem(testitem.retries + 1), 10.millis)
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

    "retry 3 items to but one will completely fail" in {
      val source = Source(List(TestItem(0), TestItem(20), TestItem(8)))
      val businessFlow = Flow[TestItem].map(item => {
        if (item.retries < 10 || item.retries == 20)
          Left(item)
        else
          Right(item)
      })
      val retryFlow = RetryFlow[TestItem, TestItem](businessFlow,
        testitem => testitem.retries > 10,
        testitem => TestItem(testitem.retries + 1), 10.millis)
      source
        .via(retryFlow)
        .runWith(TestSink.probe[TestItem])
        .request(3)
        .expectNext(TestItem(10), TestItem(10))

    }
  }
}

trait Retryable {
  val retries: Int
}
final case class TestItem(retries: Int) extends Retryable
final case class GrownUpTestItem(retries: Int, what: String = "a") extends Retryable