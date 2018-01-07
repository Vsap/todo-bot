import UserActor.UserMessage
import akka.actor.{Actor, Props}
import info.mukel.telegrambot4s.api.declarative.Commands
import info.mukel.telegrambot4s.api.{Extractors, Polling, TelegramBot}
import info.mukel.telegrambot4s.models.Message
import model.Interface._

import scala.concurrent.Future._
import scala.concurrent._
import scala.concurrent.Future
import scala.io.Source
import scala.util.Random
import model._
import repository.Task

class EchoBotActor() extends TelegramBot with Polling with Commands with Actor {

  lazy val token =
    scala.util.Properties
    .envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromFile("bot.token").getLines().mkString)

  lazy val uc = UserControl()

//  onCommand("hello") { implicit msg =>
//    reply("My token is SAFE!")
//  }
//
//  val rng = new Random(System.currentTimeMillis())
//  onCommand("random", "rand") { implicit msg =>
//    withArgs {
//      case Seq(Extractors.Int(n)) if n > 0 =>
//        reply(rng.nextInt(n).toString)
//      case _ =>
//        reply("Invalid argumentヽ(ಠ_ಠ)ノ")
//    }
//  }
  onCommand("start"){ implicit msg =>
  }
  onCommand("help"){ implicit msg =>
    InputParser.usrCtrlCmdsDescription.map(reply(_))
  }
//  onCommand("info"){ implicit msg =>
//  }
 onCommand("add"){ implicit msg =>
   withArgs{args =>
     uc.add(args.mkString(" "))
     reply("done!")
   }
  }
  onCommand("tasks"){ implicit msg =>
    uc.getTasks onSuccess{
      case tasks => tasks.map{case Task(Some(id), _, text, 1) =>
        reply("#"+id +"\n "+ text +";\n status: todo")
      case Task(Some(id), _, text, 0) =>
      reply("#"+id +";\n "+ text +";\n status: done")
      }
    }
  }
  onCommand("mark"){ implicit msg =>
    withArgs{
      case Seq(Extractors.Long(n)) => uc.changeStatus(n, 0)
        reply("marked!")
      case _ => reply("unknown error with mark! ヽ(ಠ_ಠ)ノ")
    }
  }
  onCommand("unmark"){ implicit msg =>
    withArgs{
      case Seq(Extractors.Long(n)) => uc.changeStatus(n, 1)
        reply("unmarked!")
      case _ => reply("unknown error with mark! ヽ(ಠ_ಠ)ノ")
    }
  }
  onCommand("edit"){ implicit msg =>
    withArgs{ args =>
      withArgs{ newText =>
        uc.changeText(args.mkString("").toInt, newText.mkString(""))
        reply("task changed!")
      }
    }
  }

//  onMessage { implicit message =>
//    message.from.map(_.id.toString).foreach { userId =>
//      val userActor = context.child(userId).getOrElse {
//        context.actorOf(UserActor.props(userId), userId)
//      }
//      userActor ! UserMessage(message.text.getOrElse(":)"))
//    }
//  }


  override def preStart(): Unit = {
    run()
  }

  def receive: Receive = {
    case sm@SendMessage(UserMessage(userMessage)) =>
      reply(userMessage)(sm.message)
    case _ => println("command not found")
  }
}

case class SendMessage(userMessage: UserMessage)(implicit val message: Message)

object EchoBotActor{
  def props(): Props = Props(new EchoBotActor())
}
