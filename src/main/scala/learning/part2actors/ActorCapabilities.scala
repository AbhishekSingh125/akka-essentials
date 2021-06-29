package learning.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.part2actors.ActorCapabilities.Counter.{Decrement, Increment, Print}
import learning.part2actors.ActorCapabilities.Person.LiveTheLife


object ActorCapabilities extends App {
  class SimpleActor extends Actor {
    /** Each Actor has a context member
     * This context is a complex data structure that has references to information regarding the enviroment this actor runs in
     *  */
//    context.self // is the reference of this actor equivalent to this
    override def receive: Receive = {
      case "Hi!" => context.sender() ! "Hello, there" // context.sender() returns an actor ref  which can be used to send a message back -> replying to a message
      case message: String => println(s"[${context.self}] I have received $message")
      case number: Int => println(s"[${context.self.path}] I have received a NUMBER: $number")
      case SpecialMessage(contents) => println(s"[$self] I have received something special: $contents")
      case SendMessageToYourself(content) => self ! content
      case SayHiTo(ref) => ref ! "Hi!" // alice is being passed as the sender
      case WirelessPhoneMessage(content, ref) => ref forward(content + "s") // i keep the original sender of WPM
    }
  }

  val actorSystem = ActorSystem("ActorCapabilitiesDemo")
  val simpleActor = actorSystem.actorOf(Props[SimpleActor],"simpleActor")

  simpleActor ! "Hello, Actor"

  // 1 - messages can be of any type
  simpleActor ! 42
  // you can define your own
  case class SpecialMessage(contents: String)
  simpleActor ! SpecialMessage("some special content")
  /** You can send any messages but under TWO CONDITIONS
   * 1 - messages must be IMMUTABLE
   * 2 - messages must be SERIALIZABLE -> means that the JVM can transform it into a bytestream and send it to another JVM
   *
   * IN PRACTICE use case classes and case objects */

  // 2 - actors have information about their context and about themselves
  // context.self -> this in OOP
  case class SendMessageToYourself(content: String)
  simpleActor ! SendMessageToYourself("I am an actor and I am proud of it")

  // 3 - actors can REPLY to messages
  val alice = actorSystem.actorOf(Props[SimpleActor],"alice")
  val bob = actorSystem.actorOf(Props[SimpleActor],"bob")

  case class SayHiTo(ref: ActorRef)
  alice ! SayHiTo(bob)
  // 4 - reply to "me" that is noSender i.e null -> reply will go to dead-letters
  alice ! "Hi!"

  // 5 - forwarding messages = sending a message with the ORIGINAL sender

  case class WirelessPhoneMessage(content: String, ref: ActorRef)
  alice ! WirelessPhoneMessage("Hi",bob) // original sender -> noSender

  /** Actor Basics
   * Every actor derives from:
   * trait Actor {
   * def receive: Receive
   * } message handler object, rendered by Akka is invoked when actor processes a message
   *
   * The receive type is an alias:
   * type Receive = PartialFunction[Any, Unit]
   *
   * Actor needs infrastructure:
   * val system = ActorSystem("AnActorSystem") <- no spaces
   *
   * Creating an Actor is not done in the traditional way:
   * val actor = system.actorOf(Props[MyActor],"myActorName")
   *
   * Sending Messages
   * actor ! "hello, Actor" <- the message can be anything immutable and serializable
   *
   * Actor Principles upheld
   * - full encapsulation*: cannot create actors manually, cannot directly call methods
   * - full parallelism
   * - non-blocking interaction via messages
   *
   * Actor references
   * can be sent
   * the self reference
   *
   * How to reply: use sender
   * */

  /** EXERCISES
   * 1. create a counter actor -> hold an internal variable
   *   - Increment
   *   - Decrement
   *   - Print
   *
   * 2. create a BankAccount as an Actor
   *    - Deposit amount
   *    - Withdraw amount
   *    - Statement
   *    - respond with success or failure for Deposit and Withdraw
   *
   *    interact with some other kind of actor */

  class Counter extends Actor {
    import Counter._
    var count = 0
    override def receive: Receive = {
      case Increment => count += 1
      case Decrement => count -= 1
      case Print => println(s"[Counter Actor] The current count is: $count")
    }
  }
  /** Create these messages in the companion objects that support them */

  /** DOMAIN of the counter */
  object Counter {
    case object Increment
    case object Decrement
    case object Print
  }

  val counter = actorSystem.actorOf(Props[Counter], "myCounter")

  (1 to 5).foreach(_ => counter ! Increment)

  (1 to 3).foreach(_ => counter ! Decrement)
  counter ! Print

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  class BankAccount extends Actor {
    import BankAccount._
    var funds = 0
    override def receive: Receive = {
      case Deposit(amount) => if (amount < 0) sender() ! TransactionFailure("Invalid amount deposited")
      else {
        funds += amount
        sender() ! TransactionSuccess(s"Funds successfully deposited: $amount")
      }
      case Withdraw(amount) => if (amount < funds && amount > 0) {
        funds -= amount
        sender() ! TransactionSuccess(s"Funds successfully withdrew: $amount")
      } else
        sender() ! TransactionFailure("Insufficient Funds")
      case Statement => println(s"[Bank Actor] Account Balance: $funds")
    }
  }

  object Person{
    case class LiveTheLife(account: ActorRef)
  }
  class Person extends Actor {
    import BankAccount._
    import Person._
    override def receive: Receive = {
      case LiveTheLife(account) => {
        account ! Deposit(10000)
        account ! Withdraw(2000)
        account ! Withdraw(100000)
        account ! Withdraw(-2)
        account ! Statement
      }
      case message => println(message.toString)
    }
  }

  val account = actorSystem.actorOf(Props[BankAccount],"bank")
  val person = actorSystem.actorOf(Props[Person],"customer")

  person ! LiveTheLife(account)

}
