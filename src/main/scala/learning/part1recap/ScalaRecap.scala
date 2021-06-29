package learning.part1recap

import scala.annotation.tailrec
import scala.util.Try

object ScalaRecap extends App {

  val aCondition: Boolean = false

  var aVariable = 42
  aVariable += 1 // variable = 43

  /** Expression */
  val aConditionedVal = if (aCondition) 42 else 64

  /** Code block */
  val aCodeBlock = {
    if (aCondition) 74
    56
  }

  /** Types */
  //Unit -> type of expressions that have side effects
  val aUnit = println("hello, Scala")

  def aFunction(x: Int) = x + 1
  // recursion - TAIL Recursion
  @tailrec
  def factorial(n: Int, acc: Int): Int = {
    if (n <= 1 ) acc
    else factorial(n-1, acc * n)
  }

  /** OOP */

  class Animal
  class Dog extends Animal
  val aDog: Animal = new Dog // Valid

  trait Carnivore {
    def eat(animal: Animal): Unit // Abstract
  }

  class Crocodile extends Animal with Carnivore {
    override def eat(animal: Animal): Unit = println("Chow Chow")
  }

  // Method notations
  // infex notation
  val aCroc = new Crocodile
  aCroc.eat(aDog)
  //or
  aCroc eat aDog

  // anonymous classes
  val aCarnivore = new Carnivore { // Anonymous class
    override def eat(animal: Animal): Unit = println("chew chew")
  }

  aCarnivore eat aDog

  // generics
  abstract class MyList[+A] // covariant
  // companion objects
  object MyList

  // case classes
  case class Person(name: String, age: Int) // A LOT in this course

  // Exceptions
  val aPotentialFailure = try {
    throw new RuntimeException("I am in try") // Nothing
  } catch {
    case e: Exception => "I caught an Exception"
  } finally {
    // side effects
    println("some logs")
  }

  // Functional programming

  val incrementer = (x: Int) => x + 1

  val incremented12 = incrementer(12) // incrementer.apply(12)

  // FP is all about working with functions as first-class
  List(1,2,4).map(incrementer)
  // map is called a HOF -> takes function as a parameter or returns a function as a result

  // for comprehensions
  val pairs = for {
    num <- List(1,2,3,4)
    char <- List('a','b','c','d')
  } yield (num, char)

  // Seq, Array, List, Vector, Map, Tuples, Sets

  // collections
  // Option and Try
  val anOption = Some(2)
  val aTry = Try {
    throw new RuntimeException
  }

  // Pattern Matching
  val unknown = 2
  val order = unknown match {
    case 1 => "first"
    case 2 => "second"
    case _ => "unknown"
  }

  val bob = Person("Bob", 22)
  val greeting = bob match {
    case Person(n, _) => s"Hi my name is $n"
    case _ => "I dont know you"
  }

  // All the patterns

}
