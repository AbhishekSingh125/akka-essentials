package learning.part4faultTolerance

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, PoisonPill, Props, Terminated}

object StoppingActors extends App {
  val system = ActorSystem("StoppingActorDemo")

  object Parent {
    case class StartChild(name: String)
    case class StopChild(name: String)
    case object AllChildren
    case object Stop
  }

  class Parent extends Actor with ActorLogging {
    override def receive: Receive = withChildren(Map())
    import Parent._

    def withChildren(children: Map[String, ActorRef]): Receive = {
      case StartChild(name) =>
        log.info(s"Creating Child with name: $name")
        context.become(withChildren(children + (name -> context.actorOf(Props[Child], name))))
      case StopChild(name) =>
        log.info(s"Stopping child: $name")
        val childOption = children.get(name)
        childOption.foreach(childRef => context.stop(childRef))
        context.become(withChildren(children - name))
      case AllChildren =>
        log.info("Fetching children")
        children.foreach(name => println(s"child -> ${name._1}"))
      case Stop =>
        log.info("Stopping myself")
        context.stop(self) // -> asynchronous and also stops its child actors
      case message => log.info(message.toString)
    }

  }

  class Child extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)

    }
  }

  /** Method #1 - using context.stop
   **/
//
  import Parent._
//  val parentActor = system.actorOf(Props[Parent],"parent")
//  parentActor ! StartChild("child1")
//  val child = system.actorSelection("/user/parent/child1")
//  child ! "hi kid"
//  parentActor ! StartChild("child2")
//  parentActor ! StopChild("child2")
//  parentActor ! AllChildren
//
//  parentActor ! StartChild("child3")
//  val child3 = system.actorSelection("/user/parent/child3")
//  child3 ! "Hi child"
//  parentActor ! Stop
//  for (_ <- 1 to 10) parentActor ! "parent are you still there?" // should not be received
//  for (i <- 1 to 100) child3 ! s"[$i] Kid 3 are you there?"
//

  /**
   * method #2 -> using special messages
   */

//  // PoisonPill
//  val looseActor = system.actorOf(Props[Child])
//  looseActor ! "hello, loose actor"
//  looseActor ! PoisonPill // invokes the stopping procedure
//  looseActor ! "are you still there?"
//  // Kill
//  val abruptlyTerminatedActor = system.actorOf(Props[Child])
//  abruptlyTerminatedActor ! "you are about to be terminated"
//  abruptlyTerminatedActor ! Kill
//  abruptlyTerminatedActor ! "you are dead"

  /**
   * Death watch
   */

  class Watcher extends Actor with ActorLogging {
    import Parent._
    override def receive: Receive = {
      case StartChild(name) =>
        val child = context.actorOf(Props[Child],name)
        log.info(s"Started and watching child $name")
        context.watch(child) // registers this actor for the death of the child when it dies it received from akka Terminated
      case Terminated(ref) => // akka sends this terminated message
        log.info(s"The reference that I am watching ${ref.path.name} has been Stopped")
    }
  }

  val watcher = system.actorOf(Props[Watcher],"watcher")
  watcher ! StartChild("watchedChild")
  val watchedChild = system.actorSelection("/user/watcher/watchedChild")
  Thread.sleep(500)
  watchedChild ! PoisonPill

}
