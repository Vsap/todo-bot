import akka.actor.ActorSystem
import repository._
import slick.jdbc.PostgresProfile.api._

object Main {

  val db = Database.forConfig("connect")
  def main(args: Array[String]): Unit = {
    println("Hello")
    val actorSystem = ActorSystem("my-actor-system")
    actorSystem.actorOf(EchoBotActor.props(), "echo-bot-actor")
  }
}
