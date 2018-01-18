import java.time.LocalDateTime

import UserActor.UserMessage
import info.mukel.telegrambot4s.api.ChatActions
import akka.actor.{Actor, Props}
import info.mukel.telegrambot4s.api.declarative.{Callbacks, Commands, Help, InlineQueries}
import info.mukel.telegrambot4s.api.{Extractors, Polling, TelegramBot}
import info.mukel.telegrambot4s.models.Message
import slick.jdbc.PostgresProfile.api._

import scala.io.Source
import repository._
import java.time.Instant

import info.mukel.telegrambot4s.Implicits._
import info.mukel.telegrambot4s.api._
import info.mukel.telegrambot4s.api.declarative.{Callbacks, InlineQueries}
import info.mukel.telegrambot4s.methods._
import info.mukel.telegrambot4s.models.UpdateType.Filters._
import info.mukel.telegrambot4s.models._

import scala.concurrent.Await
import scala.concurrent.duration._




class TodoBotActor extends TelegramBot
                    with Polling
                    with Commands
                    with InlineQueries
                    with Actor {

  lazy val token =
    scala.util.Properties
    .envOrNone("BOT_TOKEN")
    .getOrElse(Source.fromFile("bot.token").getLines().mkString)


  val db = Database.forConfig("connect")
  val userRepository = new UserRepository(db)
  val taskRepository = new TaskRepository(db)



  onCommand("/hallo", "/bonjour", "/ciao", "/hola") {
    implicit msg =>
      using(_.from) { // sender
        user =>
          reply(s"Hello ${user.firstName} from Europe?")
      }
  }


  onCommand("/start"){ implicit msg =>
    println("inside")
    Await.result(db.run(UserTable.table.schema.create),Duration.Inf)
    Await.result(db.run(TaskTable.table.schema.create),Duration.Inf)
    println(" schema created")
    using(_.from){
     user =>
       println(user.username.get + "extracted")
       val u = repository.User(user.username.get, user.id.toString)
       userRepository.contains(u).foreach{ contains =>
         if(contains == 0){
           userRepository insert u
           println("Inserted")
           reply("Welcome " + user.firstName)
         }else reply("Nice to meet u again!" + user.firstName)
       }
    }
  }
//  onCommand("info"){ implicit msg =>
//  }
 onCommand("/add"){ implicit msg =>
   withArgs{args =>
     using(_.from){
       implicit user =>
         taskRepository.getLast foreach { last =>
           taskRepository.insert(Task(Some(0), user.username.get, last + 1, args.mkString(" "), 1, None))
           reply("done!")
         }
     }
   }
  }
  onCommand("/all"){ implicit msg =>
    using(_.from) { // sender
      implicit user =>
        taskRepository.getAll foreach{
           tasks => tasks.map{
             case Task(_, _, num, text, 1, _) =>
               reply("#" + num + "\n " + text + ";\n status: todo")
             case Task(_, _, num, text, 0, _) =>
               reply("#" + num + ";\n " + text + ";\n status: done")
             case _ => reply("unknown error with status presentation! ヽ(ಠ_ಠ)ノ")
           }
        }
    }
  }
  onCommand("/todo"){ implicit msg =>
     using(_.from) { // sender
       implicit user =>
        taskRepository.getByStatus(1).foreach{ tasks =>
          tasks.toList.map(t => reply("#" + t.taskNumber + " " + t.text))
        }
    }
  }
  onCommand("/done"){ implicit msg =>
    using(_.from) { // sender
      implicit user =>
        taskRepository.getByStatus(0).foreach{ tasks =>
          tasks.toList.map(t => reply("#" + t.taskNumber + " " + t.text))
        }
    }
  }
  onCommand("/remove"){implicit  msg =>
    withArgs{
      case Seq(Extractors.Long(n)) => using(_.from){implicit user => taskRepository deleteById n
        reply("removed!")}
      case _ => reply("unknown index for /remove! ヽ(ಠ_ಠ)ノ")
    }
  }
  onCommand("/mark"){ implicit msg =>
    withArgs{
      case Seq(Extractors.Int(n)) =>
        using(_.from){implicit user =>
          taskRepository.setStatus(n, 0)
          reply("marked!")}
      case _ => reply("unknown error with /mark! ヽ(ಠ_ಠ)ノ")
    }
  }
  onCommand("/unmark"){ implicit msg =>
    withArgs{
      case Seq(Extractors.Int(n)) => using(_.from){implicit user => taskRepository.setStatus(n, 1)
        reply("unmarked!")}
      case _ => reply("unknown error with /unmark! ヽ(ಠ_ಠ)ノ")
    }
  }
//  onCommand("/edit"){ implicit msg => //////////REMAKE!!
//    withArgs{ args =>
//      withArgs{ newText =>
//        using(_.from){
//          user =>
//            taskRepository.setText(user.username.get, args.mkString(" ").toInt, newText.mkString(" "))
//            reply("task changed!")
//        }
//      }
//    }
//  }

//  onMessage { implicit message =>
//    message.from.map(_.id.toString).foreach { userId =>
//      val userActor = context.child(userId).getOrElse {
//        context.actorOf(UserActor.props(userId), userId)
//      }
//      userActor ! UserMessage(message.text.getOrElse(":)"))
//    }
//  }


//  val timeouts = Seq(3, 5, 10, 30)
//
//  override def allowedUpdates = InlineUpdates ++ CallbackUpdates
//
//  def buildResult(timeout: Int, msg: String): InlineQueryResult = {
//    InlineQueryResultArticle(s"$timeout", s"$timeout seconds",
//      inputMessageContent = InputTextMessageContent(msg),
//      description = s"Message will be deleted in $timeout seconds",
//      replyMarkup = InlineKeyboardMarkup.singleButton(button(now)))
//  }
//
//  def now = Instant.now().getEpochSecond
//
//  def button(t: Long) = InlineKeyboardButton.callbackData("⏳ left?", t + "")
//
//  onCallbackQuery {
//    implicit cbq =>
//      val left = cbq.data.map(_.toLong - now).getOrElse(-1L)
//      ackCallback(s"$left seconds remaining.", cacheTime = 0)
//  }

//  onChosenInlineResult { implicit result =>
//    val delay = result.resultId.toInt
//    request(EditMessageReplyMarkup(
//      inlineMessageId = result.inlineMessageId,
//      replyMarkup = InlineKeyboardMarkup.singleButton(button(now + delay))))
//
//    system.scheduler.scheduleOnce(delay.seconds) {
//      request(EditMessageText(
//        text = "⌛ Expired",
//        inlineMessageId = result.inlineMessageId))
//    }
//  }

//  onInlineQuery { implicit q =>
//    val results = if (q.query.isEmpty)
//      Seq.empty
//    else
//      timeouts.map(buildResult(_, q.query))
//    answerInlineQuery(results, 5)
//  }

  onInlineQuery { implicit q =>
    val u = repository.User(q.from.username.get, q.from.id.toString)
    userRepository.contains(u).foreach{ contains =>
      if(contains == 0){
        userRepository insert u
        answerInlineQuery(
          Seq(
            InlineQueryResultArticle(
              q.id,
              "Third variant!",
              InputTextMessageContent("Hello from the other side!!"),
              InlineKeyboardMarkup(
                Seq(
                  Seq(
                    InlineKeyboardButton.url("Shake it of baby", "https://www.youtube.com/watch?v=nfWlot6h_JM")))))))
      }else {
        answerInlineQuery(
          Seq(
            InlineQueryResultArticle(
              q.id,
              "Rolling in the deep!",
              InputTextMessageContent("Try it!!"),
              InlineKeyboardMarkup(
                Seq(
                  Seq(
                    InlineKeyboardButton.url("Rolling in the deep, baby!", "https://www.youtube.com/watch?v=rYEDA3JcQqw")
                  )
                )
              )
            ),
        InlineQueryResultArticle(
              q.id,
              "Shake it off",
              InputTextMessageContent("Try it!!"),
              InlineKeyboardMarkup(
                Seq(
                  Seq(
                    InlineKeyboardButton.url("Shake it off, baby!", "https://www.youtube.com/watch?v=nfWlot6h_JM")))))))
      }
    }
  }


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

object TodoBotActor{
  def props(): Props = Props(new TodoBotActor())
}
