package learning.part2actors

object HowActorsWork {

  /** Valid Questions
   * - Can we assume any ordering of messages?
   * - aren't we causing race conditions?
   * - What does "asynchronous" mean for actors?
   * - How does this all work?"
   * */

  /** How does AKKA works
   * Akka has a thread pool that it shares with actors.
   * Thread that is active can run code
   * Actor has
   *          1. message handler -> defined in the receive method of the actor
   *          2. message queue(mail box) -> just a data structure which is passive that needs a thread to run
   *
   * how akka works is by spawning a thread (100s) can handle a LOTS of actors (10000000s per GB heap)
   * the way akka manages to do that is by scheduling actors for execution
   *
   * Communication
   * Sending a message
   *  * message is enqueued in the actor's mailbox
   *  * (Threadsafe!)
   *
   * Processing a message
   *  * a Thread is scheduled to run this actor
   *  * Thread takes control of the actor and starts de-queueing the message queue
   *  * messages are extracted from the mailbox in order
   *  * the thread invokes the handler on each message
   *
   *  Guarantees
   *  Only one thread operates on an actor at any time
   *  - actors are effectively single threaded
   *  - no locks needed!
   *  - processing messages is atomic
   *
   *  message delivery guarantees
   *  - at most once delivery
   *  - for any sender-receiver pair, the message order is maintained
   *  */

}
