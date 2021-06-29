package learning.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.part2actors.ChangingActorBehavior.Mom.MomStart

object ChangingActorBehavior extends App {

  /** Problem: Changing actor behavior with time and
   * only way to do that is by keeping track of the current state and employ IF and Else checks to react different to handlers */


  class FussyKid extends Actor {
    import FussyKid._
    import Mom._
    var state = HAPPY // BAD
    override def receive: Receive = { /** Bad as it can blow up for more complex states */
      case Food(VEGETABLE) => state = SAD
      case Food(CHOCOLATE) => state = HAPPY
      case Ask(_) =>
        if (state == HAPPY) sender() ! KidAccept
        else sender() ! KidReject
    }
  }

  class StateLessFussyKid extends Actor {
    import FussyKid._
    import Mom._
    override def receive: Receive = happyReceive

    def happyReceive: Receive = {
      case Food(VEGETABLE) => context become(sadReceive, false) // change my receive handler to sadReceive
      // -> true means discard the old message handler
      // -> false means instead of replacing or discarding the old handler we will simply stack the new message handler into a stack of message handlers
      case Food(CHOCOLATE) => // stay happy
      case Ask(_) => sender() ! KidAccept
    }
    def sadReceive: Receive = {
      case Food(VEGETABLE) => context.become(sadReceive, false) // stay sad
      case Food(CHOCOLATE) => context.unbecome() // change my receive handler to happyReceive
      case Ask(_) => sender() ! KidReject
    }
  }

  object FussyKid {
    case object KidAccept
    case object KidReject
    val HAPPY = "happy"
    val SAD = "sad"
  }

  object Mom {
    case class MomStart(kidRef: ActorRef)
    case class Food(food: String)
    case class Ask(message: String) // Question like do you want to play?
    val VEGETABLE = "veggies"
    val CHOCOLATE = "chocolate"
  }

  class Mom extends Actor {
    import FussyKid._
    import Mom._
    override def receive: Receive = {
      case MomStart(kidRef) =>
        // test Interaction
        // if Stack becomes empty akka will call receive and fill the stack
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(VEGETABLE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Food(CHOCOLATE)
        kidRef ! Ask("do you want to play")
      case KidAccept => println("Yay, my kid is HAPPY")
      case KidReject => println("My kid is SAD, but at least he is healthy")
    }
  }

  val system = ActorSystem("changingActorBehaviorDemo")
  val fussyKid = system.actorOf(Props[FussyKid])
  val mom = system.actorOf(Props[Mom])

//  mom ! MomStart(fussyKid)

  val stateLessFussyKid = system.actorOf(Props[StateLessFussyKid])

  /** behavior
   * - Mom receives MomStart
   * - kid receives Food(veg)
   * - kid becomes sad
   * - Mom asks to play
   * - Kid rejects
   * - Mom prints */

  mom ! MomStart(stateLessFussyKid)

  /** Causality
   * Mom receives MomStart
   *  kid receives Food(VEG) -> kid will change the handler to sadReceive
   *  kid receives Ask(play?) -> kid replies with sadReceive handler =>
   * Mom receives KidReject
   * */

  /*
  - Food(VEG -> message handler turns to sadReceive
  - Food(Choco) -> become happy receive, false -> stack push happyReceive
  context.become()
  Stack:
  1. happyReceive
  - Food(VEG) -> stack.push(sadReceive)
  Stack:
  1. happyReceive <- Akka calls the top of stack and pops it out with context . unbecome
  2. sadReceive
  3. happyReceive
   */
  /*
  new behavior
  food(veg)
  food(veg)
  food(chocolate)

  Stack:
  Initial               Receives food(Veg)                Receives another food(veg)         Receives food(chocolate) -> pops the stack aka becomes little happy
  [happyReceive] -> [sadReceive, happyReceive] -> [sadReceive, sadReceive, happyReceive] -> [sadReceive, happyReceive]
   */
}
