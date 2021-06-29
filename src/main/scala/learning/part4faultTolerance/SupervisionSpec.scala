package learning.part4faultTolerance

import akka.actor.SupervisorStrategy.{Escalate, Restart, Resume, Stop}
import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, AllForOneStrategy, OneForOneStrategy, Props, SupervisorStrategy, Terminated}
import akka.testkit.{EventFilter, ImplicitSender, TestKit}
import org.scalatest.BeforeAndAfterAll
import org.scalatest.wordspec.AnyWordSpecLike

class SupervisionSpec extends TestKit(ActorSystem("SupervisionSpec"))
with ImplicitSender with AnyWordSpecLike with BeforeAndAfterAll {

  override def afterAll(): Unit = {
    TestKit.shutdownActorSystem(system)
  }

  import SupervisionSpec._

  "A Supervisor" should{
    "resume a child in case of minor fault" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! "akka is awesome bfc as daksd akw jhasd aw g ik as we gd waes wegda ry asd glweg awasdg awe w"
      child ! Report
      expectMsg(3)
    }

    "restart its child in case of empty string" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "I love Akka"
      child ! Report
      expectMsg(3)

      child ! ""
      child ! Report
      expectMsg(0)
    }

    "terminate its child in case of a major error" in {
      val supervisor = system.actorOf(Props[Supervisor])
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! "akka hello"
      child ! Report
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
    "escalate an error when it doesnt know what to do" in {
      val supervisor = system.actorOf(Props[Supervisor], "supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      watch(child)

      child ! 2
      child ! Report
      val terminatedMessage = expectMsgType[Terminated]
      assert(terminatedMessage.actor == child)
    }
  }

  "A kinder supervisor" should {
    "not kill children in case it's restarted or escalates failures" in {
      val supervisor = system.actorOf(Props[NoDeathOnRestartSuperVisor],"supervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]

      child ! "Akka is cool"
      child ! Report
      expectMsg(3)

      child ! 45
      watch(child)
      child ! Report
      expectMsg(0)
    }
  }

  "An all-for-one supervisor" should {
    "apply the all-for-one strategy" in {
      val supervisor = system.actorOf(Props[AllForOneSupervisor],"allForOneSupervisor")
      supervisor ! Props[FussyWordCounter]
      val child = expectMsgType[ActorRef]
      supervisor ! Props[FussyWordCounter]
      val anotherChild = expectMsgType[ActorRef]

      EventFilter[NullPointerException]() intercept {
        child ! ""
      }

      anotherChild ! Report
      expectMsg(0)

    }
  }
}

object SupervisionSpec {

  /* Core Philosophy of Akka
   A Parent's Duty
   - its fine if actors crash
   - Parents must decide upon their children's failure.

   When an actor fails, it
   - suspends its children
   - sends a special message to its parent

   The parent can decide to
   - resume the actor
   - restart the actor (default)
   - stop the actor
   - escalate the failure
   */
  class Supervisor extends Actor {

    override val supervisorStrategy: SupervisorStrategy = OneForOneStrategy() {
          /** Applies this to the actor that throws an exception */
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }

    override def receive: Receive = {
      case props: Props =>
        val child = context.actorOf(props)
        sender() ! child
    }
  }

   class NoDeathOnRestartSuperVisor extends Supervisor {
     override def preRestart(reason: Throwable, message: Option[Any]): Unit = {
       // EMPTY
     }
   }

  class AllForOneSupervisor extends Supervisor {
    override val supervisorStrategy: SupervisorStrategy = AllForOneStrategy() {
          /** Applies this on all actors regardless of whether they threw the exception */
      case _: NullPointerException => Restart
      case _: IllegalArgumentException => Stop
      case _: RuntimeException => Resume
      case _: Exception => Escalate
    }
  }

  case object CountWords

  case object Report

  class FussyWordCounter extends Actor with ActorLogging {

    var words = 0

    override def receive: Receive = {
      case "" => throw new NullPointerException("sentence is empty")
      case sentence: String =>
        if (sentence.length > 20) throw new RuntimeException("sentence too big")
        else if (!Character.isUpperCase(sentence(0))) throw new IllegalArgumentException("sentence must start with uppercase")
        //        else context.become(wordCountRegister(sentence,0, sender()))
        else words += sentence.split(" ").length
      case Report => sender() ! words
      case _ => throw new Exception("can only receive strings")
    }
    //
    //    def wordCountRegister(words: String, count: Int, originalSender: ActorRef): Receive = {
    //      case CountWords =>
    //        val currentCount = count + words.split(" ").length
    //        context.become(wordCountRegister(words,currentCount, originalSender))
    //      case Report => count
    //  }

  }
}
