package repository

import slick.lifted.TableQuery
import slick.jdbc.PostgresProfile.api._
import slick.lifted.Tag

import scala.concurrent.Future

case class User(login: String, password: String){
}

class UserTable(tag: Tag)  extends Table[User](tag, "users"){
  val login = column[String]("login", O.PrimaryKey)
  val password = column[String]("password")

  def * = (login, password) <> (User.apply _ tupled, User.unapply)
}

object UserTable{val table = TableQuery[UserTable]}


class UserRepository(db: Database){
  def insert(user: User): Future[User] =
    db.run(UserTable.table returning UserTable.table += user)
  def ifMatches(user: User): Future[Int] =
    db.run(UserTable.table.filter(_.login === user.login)
      .filter(_.password === user.password).length.result)
  def contains(user: User): Future[Int] =
    db.run(UserTable.table.filter(_.login === user.login).length.result)
  def getByLogin(login: String): Future[Option[User]] =
    db.run(UserTable.table.filter(_.login === login).result.headOption)
  def delete(user: User): Future[Int] =
    db.run(UserTable.table.filter(_.login === user.login).delete)
}