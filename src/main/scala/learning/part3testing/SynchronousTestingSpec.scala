package learning.part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{CallingThreadDispatcher, TestActorRef, TestProbe}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration.Duration

class SynchronousTestingSpec extends AnyWordSpecLike with BeforeAndAfterAll {

  implicit val system = ActorSystem("SynchronousTestingSpec")
  override def afterAll(): Unit = {
    system.terminate()
  }

  /** Unit tests: All about predictability -> increasing actor complexity the async tests might become frustrating
   *  Synchronous testing i.e. when we send a msg to an actor we are sure that an actor has already received that msg */
  import SynchronousTestingSpec._
  "A Counter" should {
    "synchronously increase its counter" in {
      val counter = TestActorRef[Counter](Props[Counter]) // TestActorRefs: only work in the calling threads
      counter ! Inc // counter has ALREADY received the message
      assert(counter.underlyingActor.count == 1)
    }

    "synchronously increase its counter at the call of the receive function" in {
      val counter = TestActorRef[Counter](Props[Counter])
      counter.receive(Inc) // same as counter ! Inc
      assert(counter.underlyingActor.count == 1)
    }
    "work on the calling thread dispatcher" in {
      val counter = system.actorOf(Props[Counter].withDispatcher(CallingThreadDispatcher.Id)) // runs on calling thread dispatcher
      val probe = TestProbe()
      probe.send(counter, Read)
      // Due to the fact that counter operates on the calling thread dispatcher - after the above line the probe has already received reply
      // Because every single interaction that happens on probes calling thread
      probe.expectMsg(Duration.Zero,0) // ALREADY has it
    }
  }
}

object SynchronousTestingSpec {
  case object Inc
  case object Read

  class Counter extends Actor {
    var count = 0
    override def receive: Receive = {
      case Inc => count += 1
      case Read => sender() ! count
    }
  }
}
