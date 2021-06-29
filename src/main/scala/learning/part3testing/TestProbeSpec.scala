package learning.part3testing

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.testkit.{ImplicitSender, TestKit, TestProbe}
import learning.part3testing.TestProbeSpec.{Master, Register, RegisterationAck, Report, SlaveWork, Work, WorkCompleted}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class TestProbeSpec extends TestKit(ActorSystem("TestProbeSpec"))
  with ImplicitSender
  with AnyWordSpecLike
  with BeforeAndAfterAll {

  override def afterAll(): Unit = {
  TestKit.shutdownActorSystem(system)
  }

  "A Master Actor" should {
    "register a slave" in {
      val master = system.actorOf(Props[Master]) // master actor is stateful
      val slave = TestProbe("slave") // special actor with assertion capabilities

      master ! Register(slave.ref)
      expectMsg(RegisterationAck)
    }
    "Master Actor should send the work to slave actor" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegisterationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString) // sender of this is Implicit sender() -> testActor
      // interaction between master actor and slave actor
      slave.expectMsg(SlaveWork(workLoadString, testActor))
      slave.reply(WorkCompleted(3, testActor))

      expectMsg(Report(3)) // test Actor will receive Report(3)
    }
    // sending two peices of work
    "master actor should aggregate data correctly" in {
      val master = system.actorOf(Props[Master])
      val slave = TestProbe("slave")
      master ! Register(slave.ref)
      expectMsg(RegisterationAck)

      val workLoadString = "I love Akka"
      master ! Work(workLoadString)
      master ! Work(workLoadString)

      // in the meantime I don't have a slave actor
      slave.receiveWhile() {
        case SlaveWork(`workLoadString`,`testActor`) => slave.reply(WorkCompleted(3, testActor)) // `workLoadString` means it receives the exact message
      }
      expectMsg(Report(3))
      expectMsg(Report(6))
    }
  }

}

object TestProbeSpec {
  // scenario
  /*
   Word counting actor hierarchy master - slave
   master -> slave -> sends requests to and slave -> master -> outside world
   send some word to the masteer
      - master sends slave the piece of work
      - slave processes and replies to master
      - master aggregates the result
      - master sends total count to original requester
   */
  case class Register(slaveRef: ActorRef)
  case class Work(text: String)
  case class SlaveWork(text: String, originalRequester: ActorRef)
  case class WorkCompleted(count: Int, originalRequester: ActorRef)
  case class Report(totalWordCount: Int)
  case object RegisterationAck

  class Master extends Actor {
    override def receive: Receive = {
      case Register(slaveRef) =>
        sender() ! RegisterationAck
        context.become(online(slaveRef, 0))
      case _ =>
    }
    def online(slaveRef: ActorRef, totalWordCount: Int): Receive = {
      case Work(text) => slaveRef ! SlaveWork(text, sender())
      case WorkCompleted(count, originalRequester) =>
        val newTotalWordCount = totalWordCount + count
        originalRequester ! Report(newTotalWordCount)
        context.become(online(slaveRef, newTotalWordCount))
    }
  }

  // class slave

}
