package learning.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.exercise.ChildActorsExercise.WordCounterMaster.{Initialize, WordCountReply, WordCountTask}

object ChildActorsExercise extends App {

  val system = ActorSystem("ChildActorsExercise")

  /** Exercise:
   * Distributed word counting
   * - two kinds of actors in this problem
   * - */

  object WordCounterMaster {
    case class Initialize(nChildren: Int) // receive Initialize(nChildren) message and as response will create nChildren of WordCounterWorker
    case class WordCountTask(id: Int, text: String)  // WordCountWorker will receive
    case class WordCountReply(id: Int, count: Int) // reply

  }

  /** create a wordCounterMaster
   * send Initialize(10) to wordCounterMaster
   * creates 10 wordCounterWorkers
   * send "Akka is awesome" to wordCounterMaster
   *  wcm will send a WordCountTask("...") to one of its children
   *      Child replies with a wordCountReply(3) to the master
   *     master replies with 3 to the sender
   *
   *  requester -> text to wcm -> task to wcw */

  // use round robin logic for task balancing -> task will be sent to each child in turn

  class WordCounterMaster extends Actor {
    import WordCounterMaster._

    override def receive: Receive = {
      case Initialize(nChildren) =>
        println("[Master] I am creating workers")
        val childRefs = for (i <- 1 to nChildren) yield context.actorOf(Props[WordCounterWorker], s"wcw_$i")
        context.become(withChildren(childRefs, 0, 0, Map()))
    }

    def withChildren(childrenRefs: Seq[ActorRef], currentChildIndex: Int = 0, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {
      case text: String =>
        val originalSender = sender()
        println(s"[Master] I have received $text - sending to $currentChildIndex")
        val task = WordCountTask(currentTaskId,text)
        val childRef = childrenRefs(currentChildIndex)
        childRef ! task
        val nextChildIndex = (currentChildIndex + 1) % childrenRefs.length
        val newTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(withChildren(childrenRefs, nextChildIndex, newTaskId, newRequestMap))
      case WordCountReply(id, count) => // problem -> who is the original requester of the text?
        val originalSender = requestMap(id)
        println(s"[Master] I have received a reply for task id $id with $count")
        originalSender ! count
        context.become(withChildren(childrenRefs, currentChildIndex, currentTaskId, requestMap - id))
    }
  }

  class WordCounterWorker extends Actor {
    override def receive: Receive = {

      case WordCountTask(id, text) =>
        println(s"${self.path} I have received task $id with $text")
        sender() ! WordCountReply(id, text.split(" ").length)
    }
  }

  class TestActor extends Actor {
    override def receive: Receive = {
      case "go" =>
        val master = system.actorOf(Props[WordCounterMaster],"master")
        master ! Initialize(5)
        val texts = List("I am testing as this is first task","If this works it will be great","Akka is awesome but tough","Will this work","Wow this is amazing","Well hope it is not too late")
        texts.foreach(i => master ! i)
      case count: Int =>
        println(s"[Test Actor] I have received a reply $count")
    }
  }

  val testActor = system.actorOf(Props[TestActor],"testActor")
  testActor ! "go"

}
