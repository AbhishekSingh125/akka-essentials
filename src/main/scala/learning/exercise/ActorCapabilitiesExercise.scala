package learning.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.exercise.ActorCapabilitiesExercise.CounterActor.{Decrement, Increment, Print}
import learning.exercise.ActorCapabilitiesExercise.Person.LiveTheLife

object ActorCapabilitiesExercise extends App {
  val actorSystem = ActorSystem("ActorCapabilitiesExercise")


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

  class CounterActor extends Actor {
    import CounterActor._
    var counter = 0
    override def receive: Receive = {
      case Increment => counter += 1
      case Decrement => counter -= 1
      case Print => println(s"[Count Actor]: Current counter is $counter")
    }
  }

  object CounterActor {
    case object Increment
    case object Decrement
    case object Print
  }

  val operation = actorSystem.actorOf(Props[CounterActor],"operator")

  (1 to 5).foreach(_ => operation ! Increment)
  (1 to 3).foreach(_ => operation ! Decrement)
  operation ! Print

  class BankAccount extends Actor {
    var balance = 0
    import BankAccount._
    override def receive: Receive = {
      case Deposit(amount) => if (amount < 0) {
        sender() ! TransactionFailure("Invalid Amount")
      } else {
        balance += amount
        sender() ! TransactionSuccess(s"Depositing: $amount")
      }
      case Withdraw(amount) => if (balance > amount && amount > 0) {
          balance -= amount
          sender() ! TransactionSuccess(s"Withdrawing: $amount")
        } else if (amount < 0) {
        sender() ! TransactionFailure("Invalid amount must be positive")
      } else sender() ! TransactionFailure("Insufficient Funds")
      case Statement => sender() ! s"[Bank Teller] Current account balance: $balance"

    }
  }

  object BankAccount {
    case class Deposit(amount: Int)
    case class Withdraw(amount: Int)
    case object Statement

    case class TransactionSuccess(message: String)
    case class TransactionFailure(reason: String)
  }

  object Person {
    case class LiveTheLife(account: ActorRef)
  }

  class Person extends Actor {
    import BankAccount._
    import Person._
    override def receive: Receive = {
      case LiveTheLife(account) => {
        account ! Deposit(10000)
        account ! Withdraw(-10)
        account ! Deposit(1000)
        account ! Withdraw(10)
        account ! Withdraw(500)
        account ! Statement
      }
      case TransactionSuccess(msg) => println(s"[Bank Teller] I have Succeeded in $msg")
      case TransactionFailure(msg) => println(s"[Bank Teller] I have failed due to $msg")
      case message => println(message.toString)
    }
  }

  val bankActor = actorSystem.actorOf(Props[BankAccount],"bank")
  val accountOwner = actorSystem.actorOf(Props[Person],"customer")

  accountOwner ! LiveTheLife(bankActor)
}
