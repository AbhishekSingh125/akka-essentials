package learning.exercise

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import learning.exercise.RoundRobin.Player.InitializeWorkers

object RoundRobin extends App {

  val system = ActorSystem("RoundRobin")

  object Player {
    case class InitializeWorkers(nWorkers: Int)
    case class WorkerTask(workerId: Int, task: String)
    case class WorkerReply(workerId: Int, reply: String)
  }

  class Player extends Actor {
    import Player._
    override def receive: Receive = {
      case InitializeWorkers(nWorkers) =>
        val waitingWorkers = for (i <- 1 to nWorkers) yield context.actorOf(Props[Worker],s"wkr_$i")
        context.become(taskAssigner(waitingWorkers,0,0,Map()))
    }

    def taskAssigner(nWorkers: Seq[ActorRef],currentWorkerId: Int, currentTaskId: Int, requestMap: Map[Int, ActorRef]): Receive = {

      case text: String =>
        val originalSender = sender()
        println(s"[Master] I have received $text - sending it to $currentWorkerId")
        val task = WorkerTask(currentTaskId, text)
        val workerRef = nWorkers(currentWorkerId)
        workerRef ! task
        val nextWorkerId = (currentWorkerId + 1) % nWorkers.length
        val nextTaskId = currentTaskId + 1
        val newRequestMap = requestMap + (currentTaskId -> originalSender)
        context.become(taskAssigner(nWorkers, nextWorkerId, nextTaskId, newRequestMap))

      case WorkerReply(id, reply) =>
        val originalSender = requestMap(id)
        println(s"[Worker_$id] I have finished the task: $reply")
        originalSender ! reply
        context.become(taskAssigner(nWorkers, currentWorkerId, currentTaskId, requestMap - id))
    }
  }

  class Worker extends Actor {
    import Player._
    override def receive: Receive = {
      case WorkerTask(workerId, task) =>
        println(s"[Worker_$workerId] I am assigned: $task")
        sender() ! WorkerReply(workerId, task)
    }
  }

  class Tester extends Actor {
    override def receive: Receive = {
      case "go" =>
        val player = system.actorOf(Props[Player],"player")
        player ! InitializeWorkers(5)
        val tasks = List("Clean","Gather resources","Farm","till fields","Guard Posts","Hunt","repair")
        tasks.foreach(message => player ! message)
      case reply: String => println(s"[Tester] I have received response: $reply")
    }
  }

  val testActor = system.actorOf(Props[Tester],"tester")

  testActor ! "go"


}
