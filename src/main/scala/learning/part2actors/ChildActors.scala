package learning.part2actors

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.part2actors.ChildActors.CreditCard.{AttachedToAccount, CheckStatus}
import learning.part2actors.ChildActors.NaiveBankAccount.{Deposit, InitializeAccount}
import learning.part2actors.ChildActors.Parent.{CreateChild, TellChild}

object ChildActors extends App {

  /** Actors creating other actors
   */
  val system = ActorSystem("childActors")

  object Parent {
    case class CreateChild(name: String)
    case class TellChild(message: String)
  }

  class Parent extends Actor {
    import Parent._

    override def receive: Receive = {
      case CreateChild(name) =>
        println(s"${self.path} creating child")
        // create a new actor right Here
        val childRef = context.actorOf(Props[Child],name)
        context.become(withChild(childRef))
    }
    def withChild(ref: ActorRef): Receive = {
      case TellChild(message) => ref forward message
    }
  }

  class Child extends Actor {
    override def receive: Receive = {
      case message => println(s"${self.path} I got: $message")
    }
  }

  val parent = system.actorOf(Props[Parent])

  parent ! CreateChild("Bib")
  parent ! TellChild("hey Kid!")

  /** Actor Hierarchies
   * parent -> child -> grandChild
   *        -> child2
   * */

  // Child owns parent but who owns the parent??

  /*
    Guardian actors (Top-level)
    - /system = system guardian
    - /user = user-level guardian
    - / = root guardian -> manages both system and user-level guardian => sits at the level of actor system itself
   */

  /**
   * Actor Selection
   */
  val childSelection = system.actorSelection("/user/$a/Bib")
  // returns Actor selection is a wrapper of ActorRef that we can use to send a message
  childSelection ! "I found you"
  // If the path is wrong than the ActorSelection object will contain no Actor and the message will be dropped
  /** NOTE:
   * DANGER!
   * NEVER PASS MUTABLE ACTOR STATE, OR THE 'THIS' REFERENCE, TO CHILD ACTORS
   *
   * This has the danger of breaking Actor encapsulation, as the child actor will suddenly have access to the parent actor
   * SO IT CAN MUTATE THE STATE OR DIRECTLY call METHODS OF PARENT ACTOR WITHOUT SENDING A MESSAGE */

  /** DO NOT DO THIS! */

  object NaiveBankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object InitializeAccount
  }

  class NaiveBankAccount extends Actor {
    import CreditCard._
    import NaiveBankAccount._
    var amount = 0

    override def receive: Receive = {
      case InitializeAccount =>
        val creditCardRef = context.actorOf(Props[CreditCard],"card")
        creditCardRef ! AttachedToAccount(this)
      case Deposit(funds) => deposit(funds)
      case Withdraw(funds) => withdraw(funds)

    }
    def deposit(funds: Int) = {
      println(s"${self.path} Depositing funds on top of $funds I have $amount")
      amount += funds
    }
    def withdraw(funds: Int) = {
      println(s"${self.path} Withdrawing $funds from $amount")
      amount -= funds
    }
  }
  object CreditCard {
    case class AttachedToAccount(bankAccount: NaiveBankAccount) // <- WRONG!
    case object CheckStatus
  }
  class CreditCard extends Actor {
    override def receive: Receive = {
      case AttachedToAccount(account) => context.become(attachedToAccount(account))
    }
    def attachedToAccount(account: NaiveBankAccount): Receive = {
      case CheckStatus =>
        println(s"${self.path} your message has been processed")
        account.withdraw(1) // because i can and THAT'S THE PROBLEM
    }
  }

  val bankAccountRef = system.actorOf(Props[NaiveBankAccount],"account")
  bankAccountRef ! InitializeAccount
  bankAccountRef ! Deposit(200)

  Thread.sleep(500)
  val ccSelection = system.actorSelection("/user/account/card")
  ccSelection ! CheckStatus

  // WRONG!!!!!!

  /** THIS IS CALLED CLOSING OVER MUTABLE STATE */

  /**
   * Actors can create other Actors -> context.actorOf(Props[MyActor],"child")
   * Top-Level supervisors (guardians)
   * - /system
   * - /user
   * - / (root)
   *
   * Actor Paths
   * /user/parent/child
   *
   * Actor Selections
   * system.actorSelection("path") works with context as well
   *
   * Actor encapsulation dangers */
}
