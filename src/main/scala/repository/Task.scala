package repository

import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag
import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


case class Task(id: Option[Long], userId: String, text: String, status: Int)

class TaskTable(tag: Tag)  extends Table[Task](tag, "tasks"){
  val id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  val userId = column[String]("username")
  val text = column[String]("text")
  val status = column[Int]("status")

  val userIdFk = foreignKey("username_fk", userId, TableQuery[UserTable])(_.login)

  def * = (id, userId, text, status) <> (Task.apply _ tupled, Task.unapply)
}

object TaskTable{val table = TableQuery[TaskTable]}

class TaskRepository(db: Database){
  def insert(task: Task): Future[Task] =
    db.run(TaskTable.table returning TaskTable.table += task)
  def getAll(username: String): Future[Seq[Task]] = {
    val query = TaskTable.table.filter(_.userId === username).result
    db.run(query)
  }

  def getByStatus(username: String, status: Int): Future[Seq[Task]] =
    db.run(TaskTable.table.filter(_.userId === username).filter(_.status === status).result)
  def deleteByStatus(username: String, status: Int): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === username).filter(_.status === status).delete)
  def deleteById(username: String, id: Long): Future[Int] =
    db.run(TaskTable.table.filter(_.id === id).delete)
  def deleteAll(username: String): Future[Int] = db.run(TaskTable.table.filter(_.userId === username).delete)
  def setStatus(username: String, id: Long, status: Int): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === username).filter(_.id === id).map(msg => (msg.status)).update((status)))
  def setText(username: String, id: Long, text: String): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === username).filter(_.id === id).map(msg => (msg.text)).update((text)))

}