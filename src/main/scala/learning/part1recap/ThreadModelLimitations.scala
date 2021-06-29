package learning.part1recap

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object ThreadModelLimitations extends App {

  /**
   * #1 OOP encapsulation is only valid in the single threaded model */

  class BankAccount(private var amount: Int) { // another possible @volatile
    override def toString: String = "" + amount
    
    def withdraw(money: Int) = this.amount -= money // not thread safe -> not atomic
//    def deposit(money: Int) = this.amount += money
    def deposit(money: Int) = this.synchronized{
      this.amount -= money
    }
    def getAmount = amount
  }

//  val account = new BankAccount(2000)
//  for (_ <- 1 to 1000) {
//    new Thread(() => account.withdraw(1)).start()
//  }
//
//  for (_ <- 1 to 1000) {
//    new Thread(() => account.deposit(1)).start()
//  }

//  println(account.getAmount)

  /** Object Oriented Encapsulation is broken in a multithreaded env
   * we can solve that by synchronizing everything involved i.e. locks */

  // Locks solve this problem but introduce more problems
  /*
  * Deadlocks
  * Livelock
  * */

  /** we would need a data structure
   * - fully encapsulated
   * - with no locks
   * */

  /** #2
   * delegating something to a thread is a PAIN
   * */

  // we have a running thread and we want to pass a runnable to that thread.

  var task: Runnable = null

  val runningThread: Thread = new Thread(() => {
    while (true){
      while (task == null) {
        runningThread.synchronized{
          println("[background] waiting for a task...")
          runningThread.wait() // requires to acquire running thread lock so we need it synchronized
        }
      }
      task.synchronized{
        println("[background] I have a task!")
        task.run()
        task = null
      }
    }
  })

  def delegateToBackgroundThread(r: Runnable) = {
    if (task == null) task = r

    runningThread.synchronized{
      runningThread.notify()
    }
  }

  runningThread.start()
  Thread.sleep(500)
  delegateToBackgroundThread(() => println(42))
  Thread.sleep(1000)
  delegateToBackgroundThread(() => println("This should run in the background"))

  /** #3
   * Tracing and dealing with errors in a multithreaded environment is a PAIN IN THE ASS */

  // 1M numbers in between 10 Threads
  val futures = (0 to 9).map(i => 100000 * i until(100000 * (i + 1))) // 0 - 9999, 100000 - 199999 etc
    .map(range => Future{
      if (range.contains(546735)) throw new RuntimeException("invalid Number")
      range.sum
    })

  val sumFuture = Future.reduceLeft(futures)(_ + _)
  sumFuture.onComplete(println)
}
