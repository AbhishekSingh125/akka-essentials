package learning.part5infrastructure

import akka.actor.{Actor, ActorLogging, ActorSystem, Cancellable, Props, Timers}

import scala.concurrent.duration._
import scala.language.postfixOps

object SchedulersAndTimers extends App {
  /** Goal
   * be able to run some code at a defined point in the future
   * maybe repeatedly */

  class SimpleActor extends Actor with ActorLogging {
    override def receive: Receive = {
      case message => log.info(message.toString)
    }
  }

  val system = ActorSystem("SchedulersTimersDemo")
//  val simpleActor = system.actorOf(Props[SimpleActor])
//
//  system.log.info("Scheduling Reminder for simpleActor")
//
  import system.dispatcher
//
//  system.scheduler.scheduleOnce(1 second) {
//    simpleActor ! "reminder"
//  }
////  } (system.dispatcher)
//
//  // need some thread to run the scheduler implicitly
//
////  implicit val executionContext = system.dispatcher // #2 way of passing the dispatcher
//
//  val routine: Cancellable = system.scheduler.schedule(1 second, 2 seconds) { // repeated schedule that an be cancelled
//    simpleActor ! "HeartBeat"
//  } // result is a cancellable
//
//  system.scheduler.scheduleOnce(5 seconds) {
//    routine.cancel()
//  }

  /** Note
   * Don't use unstable references inside scheduled actions for example -> if an actor terminates while a scheduler is running
   * all scheduled tasks execute when the system is terminated regardless of the initial delay
   * Schedulers are not the best at precision and long-term planning
   * */

  /** Exercise
   * implement a self closing actor
   *
   * - if the actor receives a message (anything), you have 1 second to send it another message
   * - if the time window expires, the actor will stop itself
   * - if you send another message the time window is reset and you have one more second to send another message */

  class SelfClosingActor extends Actor with ActorLogging {
      var timeOutSchedule: Cancellable = context.system.scheduler.scheduleOnce(1 second) {
        self ! "timeout"
      }

    def createNewTimeoutWindow(): Cancellable = {
      context.system.scheduler.scheduleOnce(1 second){
        self ! "timeout"
      }
    }
    override def receive: Receive = {
      case "timeout" =>
        log.info("Stopping myself")
        context.stop(self)
      case message =>
        log.info(s"Received $message Staying Alive")
        timeOutSchedule.cancel()
        timeOutSchedule = createNewTimeoutWindow()
    }
  }
//
//  val selfClosingActor = system.actorOf(Props[SelfClosingActor])
//  system.scheduler.scheduleOnce(700 millis) {
//    selfClosingActor ! "ping"
//  }
//
//  system.scheduler.scheduleOnce(2 second) {
//    system.log.info("sending pong to the self closing actor")
//    selfClosingActor ! "pong"
//  }

  /** Utility akka provides to send messages to your self called
   * Timer
   * Lifecycle of schedule messages is difficult to maintain if the actor is restarted or killed
   * Timer is safer way to schedule messages to yourself from WITHIN an actor */

  case object TimerKey
  case object Start
  case object Stop
  case object Reminder
  class TimerBasedHeartbeatActor extends Actor with ActorLogging with Timers {
    timers.startSingleTimer(TimerKey, Start, 500 millis)

    override def receive: Receive = {
      case Start =>
        log.info("Bootstrapping")
        /** When startTimer is used on a key of a previous timer the previous timer key is automatically cancelled
         *  */
        timers.startTimerWithFixedDelay(TimerKey,Reminder, 1 second) // startPeriodicTimer deprecated
      case Reminder => log.info("I AM ALIVE")
      case Stop =>
        log.warning("Stopping")
        timers.cancel(TimerKey)
        context.stop(self)
    }
  }

  val timerBasedHeartbeatActor = system.actorOf(Props[TimerBasedHeartbeatActor],"timerActor")
  system.scheduler.scheduleOnce(5 seconds) {
    timerBasedHeartbeatActor ! Stop
  }

}
