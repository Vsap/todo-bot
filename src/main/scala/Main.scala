import akka.actor.ActorSystem
import repository._
import slick.jdbc.PostgresProfile.api._

import scala.concurrent.Await
import scala.concurrent.duration.Duration

object Main {

  val db = Database.forConfig("connect")
  val userRepository = new UserRepository(db)
  val taskRepository = new TaskRepository(db)

  def main(args: Array[String]): Unit = {
    println("Hello")
    Await.result(db.run(UserTable.table.schema.create),Duration.Inf)
    Await.result(db.run(TaskTable.table.schema.create),Duration.Inf)
    val actorSystem = ActorSystem("my-actor-system")
    actorSystem.actorOf(EchoBotActor.props(), "echo-bot-actor")
  }
}
