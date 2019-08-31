import akka.actor.ActorSystem
import repository._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {


  def main(args: Array[String]): Unit = {
    println("Hello")

    val actorSystem = ActorSystem("my-actor-system")
    actorSystem.actorOf(TodoBotActor.props(), "todo-bot-actor")
  }
}
