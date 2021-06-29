package learning.part4faultTolerance

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}

object ActorLifecycle extends App {

  /** Actor Instance
   * has methods
   * may have internal state
   *
   * Actor reference aka incarnation
   * created with actorOf
   * has mailbox and can receive messages
   * contains one actor instance
   * contains a UUID
   *
   * Actor Path
   * may or may not have an ActorRef inside */

  /** Actor Lifecycle
   * Actors can be
   * - started
   * - suspended
   * - resumed
   * - restarted
   * - stopped
   *
   * start = create a new ActorRef with a UUID at a given path
   * suspend = the actor ref will enqueue but not process more messages
   * resume = the actor ref will continue processing more messages
   * restarting is trickier
   * - suspend - actor ref is suspend i.e. may enqueue but cannot process any messages
   * - Swap the actor instance:
   *    - old instance calls preRestart
   *    - replace actor instance with a new one
   *    - new instance calls postRestart
   *   Resumed
   *
   * Note: As an effect of restarting, any internal state in the actor is destroyed
   *
   * Stopping frees the actorRef within a path
   * - call postStop
   * - all watching actors receive Terminated(ref)
   * After stopping, another may be created at the same path
   * - different UUID, so different ActorRef: which means that as a result of stopping messages currently enqueued in that actor reference are lost */

  object StartChild

  class LifecycleActor extends Actor with ActorLogging {

    override def preStart(): Unit = log.info("I am Starting")

    override def postStop(): Unit = log.info("I have Stopped")

    override def receive: Receive = {
      case StartChild =>
        context.actorOf(Props[LifecycleActor], "child")
    }
  }

  /**
   * Restart */

  object Fail
  object FailChild
  object Check
  object CheckChild

    class Parent extends Actor {
      private val child = context.actorOf(Props[Child], "supervisedChild")

      override def receive: Receive = {
        case FailChild => child ! Fail
        case CheckChild => child ! Check
      }
    }


    val system = ActorSystem("LifecycleDemo")
//    val parent = system.actorOf(Props[LifecycleActor], "parent")
//
//    parent ! StartChild
//    parent ! PoisonPill

    class Child extends Actor with ActorLogging {
      override def preStart(): Unit = log.info("Supervised Child Started")

      override def postStop(): Unit = log.info("Supervised Child Stopped")

      override def preRestart(reason: Throwable, message: Option[Any]): Unit =
        log.info(s"Supervised actor restarting because of ${reason.getMessage}")

      override def postRestart(reason: Throwable): Unit =
        log.info("Supervised actor restarted")

      override def receive: Receive = {
        case Fail =>
          log.warning("I will fail now")
          throw new RuntimeException("I Failed")
        case Check =>
          log.info("Alive and kicking")
      }
    }
  val supervisor = system.actorOf(Props[Parent],"supervisor")
  supervisor ! FailChild
  supervisor ! CheckChild
  // Even if the child actor threw an exception previously will get restarted and be able to process message -> part of default Supervision strategy

  /** Supervision strategy -
   * if an actor threw exception while processing a message,
   * this message that caused the message to be thrown will be removed from the queue and not put back in the mail box again; the actor will be restarted  */
}
