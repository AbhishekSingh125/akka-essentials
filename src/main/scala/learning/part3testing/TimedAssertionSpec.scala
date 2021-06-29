package learning.part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import com.typesafe.config.ConfigFactory
import learning.part3testing.TimedAssertionSpec.{WorkResult, WorkerActor}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps
import scala.util.Random

class TimedAssertionSpec extends TestKit(ActorSystem("TimedAssertionsSpec", ConfigFactory.load().getConfig("specialTimedAssertionsConfig")))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A worker actor" should {
    val workerActor = system.actorOf(Props[WorkerActor])

    "reply the meaning of life in a timely manner" in {
      within(500 millis, 1 second) { // A time box test
        workerActor ! "work"
        expectMsg(WorkResult(42))
      }
    }

    "reply with valid work at a reasonable cadence" in {
      within(1 second){
        workerActor ! "workSequence"
        val results = receiveWhile[Int] (max = 2 second, idle = 500 millis, messages = 10) { // result will be Seq of Ints
          case WorkResult(result) => result
        }
        assert(results.sum > 5)
      }
    }

    "reply to a test probe in a timely manner" in {
      within(1 second) {
        val probe = TestProbe()
        probe.send(workerActor, "work")
        probe.expectMsg(WorkResult(42)) // timeout of expect message does not conform to the within 1 second -> using config allows the timeout of 0.3 seconds
      }
    }
  }
}

object TimedAssertionSpec {
  // testing scenario
  case class WorkResult(result: Int)
  class WorkerActor extends Actor {
    override def receive: Receive = {
      case "work" =>
        // long computation
        Thread.sleep(500)
        sender() ! WorkResult(42)
      case "workSequence" =>
        // replying in rapid fire succession in smaller chunks
        val r = new Random()
        for (_ <- 1 to 10) {
          Thread.sleep(r.nextInt(50))
          sender() ! WorkResult(1)
        }
    }
  }
}
