package learning.part2actors

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.event.Logging

object ActorLoggingDemo extends App {

  class SimpleActorWithExplicitLogger extends Actor {

    /** 1# - Explicit Logging */
    val logger = Logging(context.system,this)

    override def receive: Receive = {
      /** Logging is done on 4 levels
       * 1 - DEBUG
       * 2 - INFO
       * 3 - WARNING
       * 4 - ERROR */
      case message => // LOG IT
        logger.info(message.toString)
    }
  }

  val system = ActorSystem("LoggingDemo")
  val actor = system.actorOf(Props[SimpleActorWithExplicitLogger])
  actor ! "Logging a simple message"

  /** #2 - Actor Logging */
  class ActorWithLogging extends Actor with ActorLogging {
    override def receive: Receive = {
      case (a, b) => log.info("Two things: {} and {}",a,b) // interpolate into the {} the values of a and b
      case message => log.info(message.toString)
    }
  }

  val actorWithLogging = system.actorOf(Props[ActorWithLogging])
  actorWithLogging ! "Working with an Actor with Logging"

  /* Interpolating Parameters */

  actorWithLogging ! (2,6)

  /** Logging is asynchronous
   * Akka logging is done with actors
   *
   * You can change the logger, e.g. SLF4J */

}
