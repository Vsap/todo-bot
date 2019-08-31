package repository

import java.time.LocalDateTime

import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global


case class Task(id: Option[Long], userId: String, taskNumber: Int, text: String, status: Int, deadline: Option[LocalDateTime])

class TaskTable(tag: Tag)  extends Table[Task](tag, "tasks"){
  val id = column[Option[Long]]("id", O.PrimaryKey, O.AutoInc)
  val userId = column[String]("username")
  val taskNumber = column[Int]("number")
  val text = column[String]("text")
  val status = column[Int]("status")
  val deadline = column[Option[LocalDateTime]]("deadline")

  val userIdFk = foreignKey("username_fk", userId, TableQuery[UserTable])(_.login)

  def * = (id, userId, taskNumber, text, status, deadline) <> (Task.apply _ tupled, Task.unapply)
}

object TaskTable{val table = TableQuery[TaskTable]}

class TaskRepository(db: Database){
  type BotUser = info.mukel.telegrambot4s.models.User

  def insert(task: Task): Future[Task] =
    db.run(TaskTable.table returning TaskTable.table += task)

  def getAll(implicit user: BotUser): Future[Seq[Task]] =
    db.run(TaskTable.table.filter(_.userId === user.username).result)

  def getLast(implicit user: BotUser):Future[Int] =
    db.run(TaskTable.table.filter(_.userId === user.username).length.result)

  def getByStatus(status: Int)(implicit user: BotUser): Future[Seq[Task]] =
    db.run(TaskTable.table.filter(_.userId === user.username).filter(_.status === status).result)

  def deleteByStatus(status: Int)(implicit user: BotUser): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === user.username).filter(_.status === status).delete)

  def deleteById(id: Long)(implicit user: BotUser): Future[Int] =
    db.run(TaskTable.table.filter(_.id === id).delete)

  def deleteByNum(num: Int)(implicit user: BotUser): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === user.username).filter(_.taskNumber === num).delete)

  def deleteAll(implicit user: BotUser): Future[Int] = db.run(TaskTable.table.filter(_.userId === user.username).delete)

  def setStatus(taskNum: Int, status: Int)(implicit user: BotUser): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === user.username).filter(_.taskNumber === taskNum).map(_.status).update(status))

  def setText(taskNum: Int, text: String)(implicit user: BotUser): Future[Int] =
    db.run(TaskTable.table.filter(_.userId === user.username).filter(_.taskNumber === taskNum).map(_.text).update(text))
}