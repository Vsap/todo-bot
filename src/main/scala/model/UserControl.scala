package model

import repository._
import scala.io._
import slick.jdbc.PostgresProfile.api._
import scala.concurrent.{Await, Future}
import scala.concurrent.duration.Duration

object Interface{
  val db = Database.forConfig("connect")
  val taskRepository = new TaskRepository(db)
  val userRepository = new UserRepository(db)

  case class UserControl(login: String = "data", password: String = "data"){
    def getTasks = taskRepository.getAll(login)
    def getByStatus(status: Int) = taskRepository.getByStatus(login, status)
    def changeStatus(id: Long, status: Int) = taskRepository.setStatus(login,id,status)
    def changeText(id: Long, text: String) = taskRepository.setText(login,id,text)
    def add(text: String) =
      taskRepository.insert(Task(Some(0), login, text, 1))
    def remove = taskRepository.deleteAll(login)
    def removeById(id: Long) =
      taskRepository.deleteById(login, id)
    def removeByStatus(status: Int) =
      taskRepository.deleteByStatus(login, status)
  }

  object Authorization{
    def signUp(login: String, password: String):Option[UserControl] =
      Await.result(userRepository.contains(User(login, password)), Duration.Inf) == 1 match {
        case false => {userRepository.insert(User(login, password));Some(UserControl(login, password))}
        case _ => {println("This username already exists");None}
      }
    def signIn(login: String, password: String): Option[UserControl] =
      Await.result(userRepository.ifMatches(User(login, password)) ,Duration.Inf) == 1 match{
        case true => Some(UserControl(login,password))
        case _ => None
      }

  }
  object InputParser{
    val cmdPattern = """(-[-a-zA-Z]*) ?([0-9a-zA-Z]*) ?([0-9a-zA-Z]*)""".r
    def input: Option[(String, Option[String], Option[String])] = cmdPattern.findFirstIn(StdIn.readLine())
      .map{case cmdPattern(command, login, password) => (command, Option(login), Option(password))}
    val authorizationCommands: List[String] = List("-help", "-signIn", "-signUp","-exit")
    val authCmdsDescription: List[String] = List(
      "input \"--help\" with no args to get all the commands with the description",
      "input \"-help\" with no args to get all the commands without the description",
      "input \"-signIn [login] [password]\" to log in if already registered",
      "input \"-signUp [login] [password]\" to register",
      "input \"-exit\" to quit the program")
    val userControlCommands: List[String] = List("-getAll", "-todo", "-done", "-removeAll",
      "-removeDone", "-remove", "-mark", "-unmark", "-add", "-prev")
    val usrCtrlCmdsDescription: List[String] = List(
      "input \"-getAll\" to look through all the tasks",
      "input \"-todo\" to look through the tasks to do",
      "input \"-done\" to look through the done tasks",
      "input \"-mark [task_id]\" to mark task as done",
      "input \"-unmark [task_id]\" to mark task as undone",
      "input \"-removeAll\" to delete all your tasks",
      "input \"-removeDone\" to delete all your done tasks",
      "input \"-remove [task_id]\" to delete a specific task",
      "input \"-add\" to add a new task, then start typing from a new line",
      "input \"-edit [task_id]\" to edit the text of the task, then start typing from a new line",
      "input \"-prev\" to return into previous menu")
  }
  def OptionToString(o:Option[Long]):String = o match{
    case Some(p) => p.toString
    case None => ""
  }
  object Menu{
    def init: Unit = {
      println("Welcome to Thomson's TODO List program.")
      println("Use commands for exploration: ")
      InputParser.authCmdsDescription.map(println)
      InputParser.usrCtrlCmdsDescription.map(println)
      auth
    }
    def auth: Unit = {
      println("U are in authorization menu.")
      InputParser.input match {
        case Some(("-signIn", Some(login), Some(password))) => {controller(Authorization.signIn(login, password))} ///  <--HERE!!!
        case Some(("-signUp", Some(login), Some(password))) => {controller(Authorization.signUp(login, password))} ///  <--HERE!!!
        case Some(("--help", Some(""), Some(""))) => init
        case Some(("-help", Some(""), Some(""))) => {InputParser.authorizationCommands.map(println)
          InputParser.userControlCommands.map(println);auth}
        case Some(("-exit", Some(""), Some(""))) => {println("GoodBye!");()}
        case Some(_) => {println("In authorization mode \"Unknown command\" exception!"); auth}
        case None => {println("In authorization mode \"None Input\" exception!"); auth}
      }
    }
    def controller(Ouc: Option[UserControl]):Unit = Ouc match{
      case None => {println("Incorrect user data!");auth}
      case Some(uc) => {
        println("U are logged in as "+ uc.login + ". Welcome!")
        println("Control mode: ")
        println("Input the commands to manipulate the tasks")
        InputParser.input match {
          case Some(("-getAll", Some(""), Some(""))) => {Await.result(uc.getTasks, Duration.Inf)
            .map( p => println("id: "+OptionToString(p.id)+"; text: "+p.text));controller(Some(uc))}
          case Some(("-todo", Some(""), Some(""))) => {Await.result(uc.getByStatus(1), Duration.Inf)
            .map( p => println("id: "+OptionToString(p.id)+"; todo: "+p.text));controller(Some(uc))}
          case Some(("-done", Some(""), Some(""))) => {Await.result(uc.getByStatus(0),Duration.Inf)
            .map( p => println("id: "+OptionToString(p.id)+"; done: "+p.text));controller(Some(uc))}
          case Some(("-mark", Some(id), Some(""))) => {uc.changeStatus(id.toString.toLong,0);controller(Some(uc))}
          case Some(("-unmark", Some(id), Some(""))) => {uc.changeStatus(id.toString.toLong,1);controller(Some(uc))}
          case Some(("-edit", Some(id), Some(""))) => {uc.changeText(id.toString.toLong,StdIn.readLine());controller(Some(uc))}
          case Some(("-removeAll", Some(""), Some(""))) => {uc.remove;controller(Some(uc))}
          case Some(("-remove", Some(id), Some(""))) => {uc.removeById(id.toString.toLong);controller(Some(uc))}
          case Some(("-removeDone", Some(""), Some(""))) => {uc.removeByStatus(0);controller(Some(uc))}
          case Some(("-add", Some(""), Some(""))) => {uc.add(StdIn.readLine());controller(Some(uc))}
          case Some(("-prev", Some(""), Some(""))) => auth
          case Some(("-help", Some(""), Some(""))) => {InputParser.authorizationCommands.map(println)
            InputParser.authorizationCommands.map(println);controller(Some(uc))}
          case Some(("--help", Some(""), Some(""))) => {InputParser.authCmdsDescription.map(println)
            InputParser.usrCtrlCmdsDescription.map(println); controller(Some(uc))}
          case Some(("-exit", Some(""), Some(""))) => {println("GoodBye!");()}
          case Some(_) => {println("In control mode \"Unknown command\" exception!"); controller(Some(uc))}
          case None => {println("In control mode \"None Input\" exception!"); controller(Some(uc))}
        }
      }
    }
  }

}
