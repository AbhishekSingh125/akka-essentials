package learning.part4faultTolerance

import akka.actor.SupervisorStrategy.Stop
import akka.actor.{Actor, ActorLogging, ActorSystem, OneForOneStrategy, Props}
import akka.pattern.{BackoffOpts, BackoffSupervisor}

import java.io.File
import scala.concurrent.duration._
import scala.io.Source
import scala.language.postfixOps

object BackoffSupervisorPattern extends App {


  case object ReadFile
  class FileBasedPersistentActor extends Actor with ActorLogging {
    var dataSource: Source = null

    override def preStart(): Unit = log.info("Persistent actor starting")

    override def postStop(): Unit = log.warning("Persistent actor has stopped")

    override def preRestart(reason: Throwable, message: Option[Any]): Unit =
      log.warning("Persistent actor restarting")

    override def receive: Receive = {
      case ReadFile =>
        if (dataSource == null) {
          dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
          log.info("I have just read some important data: " + dataSource.getLines().toList)
        }
    }
  }

  val system = ActorSystem("BackoffActorSystem")
//  val simpleActor = system.actorOf(Props[FileBasedPersistentActor],"simpleActor")
//  simpleActor ! ReadFile

  // depreceated
//  val simpleSupervisorProps = BackoffSupervisor.props(
//    Backoff.onFailure(
//      Props[FileBasedPersistentActor],
//      "simpleBackoffActor",
//      3 seconds,
//      30 seconds,
//      0.2
//    )
//  )

  val updatesSimpleSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onFailure(
    Props[FileBasedPersistentActor],
    "simpleBackoffActor",
    3 seconds, // 6s, 12s, 24s, 30s
    30 seconds,
    0.2 // adds noise so not all actors are starting at that same moment
  ))

//  val simpleBackoffSupervisor = system.actorOf(updatesSimpleSupervisorProps,"simpleSupervisor")
//  simpleBackoffSupervisor ! ReadFile

  /** Simple supervisor
   * creates a child called -> simpleBackoffActor (Props type FileBasedPersistantActor)
   * Simple supervisor can receive any message and can forward them to its child
   * - supervision strategy is default one that is restarting on everything
   *    - first attempt after 3 seconds
   *    - next attempt is 2X the previous attempt
   *    - */

  /** BackoffSupervisor that acts on stop and customizes the supervision strategy */
  val stopSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[FileBasedPersistentActor],
      "stopBackoffActor",
      3 seconds,
      30 seconds,
      0.2
    ).withSupervisorStrategy(
      OneForOneStrategy() {
        case _ => Stop
      }
    )
  )

//  val simpleStopSupervisor = system.actorOf(stopSupervisorProps,"stopSupervisor")
//  simpleStopSupervisor ! ReadFile

  class EagerFBPActor extends FileBasedPersistentActor {
    override def preStart(): Unit = {
      log.info("Eager actor starting")
      dataSource = Source.fromFile(new File("src/main/resources/testFiles/important_data.txt"))
    }
  }

//  val eagerActor = system.actorOf(Props[EagerFBPActor])
  // ActorInitializationException => Stop

  val repeatedSupervisorProps = BackoffSupervisor.props(
    BackoffOpts.onStop(
      Props[EagerFBPActor],
      "eagerActor",
      1 second,
      30 seconds,
      0.1
    )
  )
  val repeatedSupervisor = system.actorOf(repeatedSupervisorProps,"eagerSupervisor")

  /** creates Eager Supervisor and child EagerActor
   * - Eager Actor will die on start with ActorInitializationException
   * - trigger the supervision strategy in eagerSupervisor => stop eagerActor
   * -> backoff kicks in after 1 second, 2s, 4s, 8s, ..*/

  repeatedSupervisor ! ReadFile

  /** Backoff Recap
   * Pain: the repeated restarts of actors
   * - restarting immediately might be useless
   * - everyone attempting at the same time can kill resources again
   * Create a backoff supervision for exponential delays between attempts*/
}
