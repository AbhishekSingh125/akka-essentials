package learning.part1recap

import scala.concurrent.ExecutionContext.Implicits.global // implicit value
import scala.concurrent.Future

object ScalaAdvanced extends App {
  /** Partial Functions
   * Only operate on a subset of a given domain */
  val partialFunction: PartialFunction[Int, Int] = {
    case 1 => 42
    case 2 => 55
    case 5 => 999
  }

  val pf = (x: Int) => x match { // equivalent partial function based on pattern matching
    case 1 => 42
    case 2 => 55
    case 5 => 999
  }

  val aModifiedList = List(1,2,3).map { // we will make use of this syntax
    case 1 => 42
    case 2 => 55
    case _ => 999
  }

  // lifting
  val lifted = partialFunction.lift // total function from Int => Option[2]
  println(lifted(2)) // returns Some
  println(lifted(999)) // returns None

  // orElse to chain partial functions
  val pfChain = partialFunction.orElse[Int, Int] {
    case 60 => 9000
  }

  pfChain(5) // returns 999 from partial function
  pfChain(60) // returns 9000
  pfChain(457) // throw a match error because neither partial function has any case for 457

  // type aliases
  type ReceiveFunction = PartialFunction[Any, Unit]

  def receive: ReceiveFunction = {
    case 1 => println("hello")
    case _ => println("bye bye")
  }

  // implicits
  implicit val timeout = 3000
  def setTimeout(f: () => Unit)(implicit timeout: Int) = f()

  setTimeout(() => println("timeout")) // extra parameter list omitted

  // implicit conversions
  // 1) implicit defs
  case class Person(name: String) {
    def greet = s"Hi, my name is $name"
  }
  implicit def fromStringToPerson(string: String): Person = Person(string)

  "Joe".greet

  // implicit classes
  implicit class Dog(name: String) {
    def bark = println("bark!")
  }
  "Lassie".bark // new Dog("Lassie").bark

  // organize implicits
  // local scope
  implicit val inverseOrdering: Ordering[Int] = Ordering.fromLessThan(_ > _)
  List(1,2,3).sorted // return List(3,2,1)

  // imported scope

  val future = Future{
    println("Hello, Future")
  }

  // companion objects of the types included in the call
  object Person {
    implicit val personOrdering: Ordering[Person] = Ordering.fromLessThan((a,b) => a.name.compareTo(b.name) < 0) // alphabetic ordering by name
  }

  List(Person("Bob"),Person("Alice")).sorted // returns -> List(Person(Alice), Person(Bob))



}
