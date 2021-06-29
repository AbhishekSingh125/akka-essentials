package learning.part2actors

import akka.actor.{Actor, ActorSystem, Props}

object ActorsIntro extends App {

  /** Actors
   * With Traditional objects:
   * we can store their state as data
   * we call their methods
   *
   * With actors:
   * we can store their state as data
   * we send messages to them, asynchronously
   * Actors are objects we can't access directly, but also send messages to */

  // part-1 actor System
  /** ActorSystem:
   * Is a heavy weight data structure that controls a number of threads under the hood
   * which then allocate to running actors */
  val actorSystem = ActorSystem("firstActorSystem") // name: only alphanumeric characters
  println(actorSystem.name)

  // part-2 create actors
  /** Actors are uniquely identified within an ActorSystem
   * Messages are asynchronous
   * Each actor may respond differently
   * Actors are really encapsulated */

  // word count actor
  class WordCountActor extends Actor {
    // Internal data
    var totalWords = 0

    def wordCount(words: String) = words.split(" ").length

    // behavior
    def receive: PartialFunction[Any, Unit] = {
      case message: String =>
        println(s"[word counter] I have received: $message")
        totalWords += wordCount(message)
        println(s"[Word Counter]: I have counted $totalWords")
      case msg => println(s"[Word Counter] I cannot understand ${msg.toString}")
    }
  }

  // part 3 - Instantiate our actor
  val wordCounter = actorSystem.actorOf(Props[WordCountActor], "wordCounter")
  // ActorRef: Data structure akka exposes to us so that we cannot poke into actual wordcountActor
  val anotherWordCounter = actorSystem.actorOf(Props[WordCountActor], "anotherWordCounter")
  // part 4 - communicate
  wordCounter ! "I am Learning Akka and its pretty damn cool" // ! method also known as "tell"
  /** Sending a message is completely asynchronous */
  anotherWordCounter ! "This is an asynchronous message with a lot of words"

  class Person(name: String) extends Actor {
    override def receive: Receive = {
      case "hi" => println(s"Hi, my name is ${name}")
      case _ =>
    }
  }

  val person = actorSystem.actorOf(Props(new Person("Bob"))) // legal but discouraged

  person ! "hi"

  // best practice is to declare a companion object for constructor arguments

  object Person {
    /** we need methods that return props object */
    def props(name: String) = Props(new Person(name))
  }

  val betterPerson = actorSystem.actorOf(Person.props("Bobby"))
  betterPerson ! "hi"
}
