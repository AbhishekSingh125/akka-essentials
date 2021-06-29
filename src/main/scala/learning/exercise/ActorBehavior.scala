package learning.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}

object ActorBehavior extends App {

  /** Exercises
   * 1 - recreate the counter actor with context become and no mutable state
   */

  class CounterActor extends Actor {
    import CounterActor._
    override def receive: Receive = { register(0)
    }
    def register(counter: Int): Receive = {
      case Increment =>
        println(s"[Counter] -> currently incrementing $counter")
        context.become(register(counter + 1))
      case Decrement =>
        println(s"[Counter] -> currently decrementing $counter")
        context.become(register(counter - 1))
      case Print => println(s"My Current Count is $counter")

    }
  }

  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }


  val system = ActorSystem("actorBehavior")
  val counter = system.actorOf(Props[CounterActor],"myCounter")

//  (1 to 5).foreach(_ => counter ! Increment)
//  (1 to 5).foreach(_ => counter ! Decrement)
//  counter ! Print

  /** 2 - simplified voting system
   * 1 - two actors in the voting system
   * */
  case class Vote(candidate: String) // -> once the candidate votes his state will be changed to "having voted"
  case object VoteStatusRequest
  case class VoteStatusReply(candidate: Option[String])

  class Citizen extends Actor {
    // TODO
    override def receive: Receive = {
      case Vote(candidate) => context.become(voted(candidate))
      case VoteStatusRequest => sender() ! VoteStatusReply(None)
    }

    def voted(candidate: String): Receive = {
      case VoteStatusRequest => sender() ! VoteStatusReply(Some(candidate))
    }

  }
  // will be able to send msgs to the Citizens to ask who they have voted for
  case class AggregateVotes(citizens: Set[ActorRef])
  class VoteAggregator extends Actor {
    override def receive: Receive = awaitingCommand


    def awaitingCommand: Receive = {
      case AggregateVotes(citizens) =>
        citizens.foreach(citizen => citizen ! VoteStatusRequest)
        context.become(awaitingStatuses(citizens, Map()))
    }

    def awaitingStatuses(stillWaiting: Set[ActorRef], currentStats: Map[String, Int]):Receive = {
      case VoteStatusReply(None) => sender() ! VoteStatusRequest
      case VoteStatusReply(Some(candidate)) =>
        val newStillWaiting = stillWaiting - sender()
        val currentVotesOfCandidate = currentStats.getOrElse(candidate, 0)
        val newStats = currentStats + (candidate -> (currentVotesOfCandidate + 1))
        if (newStillWaiting.isEmpty){
          println(s"[aggregator] poll stats: $newStats")
        } else context.become(awaitingStatuses(newStillWaiting,newStats))
    }
  }

  val alice = system.actorOf(Props[Citizen])
  val bob = system.actorOf(Props[Citizen])
  val charlie = system.actorOf(Props[Citizen])
  val daniel = system.actorOf(Props[Citizen])

  alice ! Vote("Martin")
  bob ! Vote("Jonas")
  charlie ! Vote("Roland")
  daniel ! Vote("Roland")

  val voteAggregator = system.actorOf(Props[VoteAggregator])
  voteAggregator ! AggregateVotes(Set(alice,bob,charlie,daniel))
  /* print the status of the votes
  Map(martin -> 1, Jonas -> 1, Roland -> 2)
   */
}
