package learning.part3testing

import akka.actor.{Actor, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit}
import learning.part3testing.BasicSpec.{BlackHole, LabTestActor, SimpleActor}
import org.scalatest.BeforeAndAfterAll

import scala.concurrent.duration._
import org.scalatest.wordspec.AnyWordSpecLike

import scala.language.postfixOps
import scala.util.Random

class BasicSpec extends TestKit(ActorSystem("BasicSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  // setup
  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  "A Simple Actor" should {
    "send back the same message" in {
      // testing scenario
      val echoActor = system.actorOf(Props[SimpleActor])
      val aMessage = "hello, test"
      echoActor ! aMessage

      expectMsg(aMessage) // akka.test.single-expect-default

    }
  }


  "A BlackHole Actor" should{
    "send back some message" in {
      val blackHoleActor = system.actorOf(Props[BlackHole]) // stateful actor clears after every test
      val aMessage = "Will this return?"
      blackHoleActor ! aMessage

      expectNoMessage(1 second)
      // testActor receives the messages from actors in test because we mixed in ImplicitSender trait
    }
  }

  // Message Assertions

  "A Lab Test Actor" should{
    val labTestActor = system.actorOf(Props[LabTestActor]) // doesn't clear after every test
    "turn a String to uppercase" in {
      labTestActor ! "I love Akka"
      val reply = expectMsgType[String] // obtain the message

      assert(reply == "I LOVE AKKA") // complex assertions
    }

    "reply to a greeting" in {
      labTestActor ! "greeting"
      expectMsgAnyOf("hi","hello")
    }

    "reply with favorite tech" in {
      labTestActor ! "favoriteTech"
      expectMsgAllOf("Scala","Akka") // expects all the messages
    }
    "reply with cool tech in a different way" in {
      labTestActor ! "favoriteTech"
      val messages = receiveN(2) // Seq[Any] -> If messages received in 3 seconds are less that 2 than it will fail

      // free to do more complicated assertions
    }
    "reply with cool tech in a fancy way" in {
      labTestActor ! "favoriteTech"
      expectMsgPF(){
        case "Scala" => // we only care if PF is defined
        case "Akka" =>
      }
    }


  }
}
object BasicSpec {
  // Store all methods or values that are used in the test
  class SimpleActor extends Actor {
    override def receive: Receive = {
      case message => sender() ! message
    }
  }

  class BlackHole extends Actor {
    override def receive: Receive = Actor.emptyBehavior
  }

  class LabTestActor extends Actor {
    val random = new Random()
    override def receive: Receive = {
      case "greeting" => if (random.nextBoolean()) sender() ! "hi" else sender() ! "hello"
      case "favoriteTech" => // replying two messages
        sender() ! "Scala"
        sender() ! "Akka"
      case message: String => sender() ! message.toUpperCase
    }
  }
}
