package learning.part1recap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.{Failure, Success}

object MultiThreadingRecap extends App {

  // Creating Threads on the JVM
  val aThread = new Thread(() => println("I'm running in parallel"))
  aThread.start()
  aThread.join()

  val threadHello = new Thread(() => (1 to 1000).foreach(_ => println("hello")))
  val threadBye = new Thread(() => (1 to 1000).foreach(_ => println("goodbye")))
  threadHello.start()
  threadBye.start()

  /** Problem;
   * With threads and Thread scheduling is different runs produce different results */

  class BankAccount(@volatile private var amount: Int) { // another possible @volatile - solves only "atomic read" not writes
    override def toString: String = "" + amount
    def withdraw(money: Int) = this.amount -= money // not thread safe -> not atomic

    def safeWithdraw(money: Int) = this.synchronized{ // only one thread can access this
      this.amount -= money
    }
  }

  // inter-thread communication on the JVM
  // wait-notify mechanism

  // scala futures
  val future = Future {
    // long computation - on a different thread
    42
  }

  // callbacks on complete
  future.onComplete{ // Future is a monadic construct
    case Success(42) => println("I found the meaning of life")
    case Failure(_) => println("something happened with meaning of life")
  }

  val aProcess = future.map(_ + 1) // returns a future with value 43
  val aFlatFuture = future.flatMap{value =>
    Future(value + 2)
  } // Future with 44

  val filteredFuture = future.filter(_ % 2 == 0 ) // if the value contained in the original future passes otherwise it will fail with NoSuchElementException

  // for-comprehensions
  val aNonsenseFuture = for {
    meaningOfLife <- future
    filteredMeaning <- filteredFuture
  } yield meaningOfLife + filteredMeaning

  // andThen, recover/ recoverWith
  // Promises
}
