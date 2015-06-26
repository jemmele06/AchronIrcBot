package tk.chronofrag

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.Write
import akka.util.ByteString


class CommandActor(connection: ActorRef) extends Actor {
  val (userMsg, nickMsg, joinMsg) = (
    "tyrname tyrrealname localhost :JRaccoon",
    "tyrbot5",
    "#achrontest")

  def receive = {
    case IncomingRawMessage(_, "PING", _, message) =>
      pong(message)

    case IncomingRawMessage(_, "NOTICE", destination, message) => destination match {
      case "AUTH" => message match {
        case s: String if s.contains("Looking up your hostname") =>
          user(userMsg)
          nick(nickMsg)
        case _ => //Silently ignore other messages from AUTH destination
      }
      case _ => //Silently ignore other destinations from NOTICE commands
    }

    case IncomingRawMessage(_, "376", _, _) => //End of MOTD, safe to send
      join(joinMsg)

    case IncomingRawMessage(user, "PRIVMSG", dest, msg) =>
      if(dest == nickMsg)
        say(msg, user.nick)
      else
        say(msg, dest)

    case _ => //Silently ignore other commands
  }

  def sendRAW(message: String): Unit = {
    connection ! Write(ByteString(message + "\n"))
  }

  def pong(message: String): Unit = {
    sendRAW(s"PONG :$message")
  }

  def user(user: String): Unit = {
    sendRAW(s"USER $user")
  }

  def nick(nick: String): Unit = {
    sendRAW(s"NICK $nick")
  }

  def join(channel: String): Unit = {
    sendRAW(s"JOIN $channel")
  }

  def say(msg: String, dest: String): Unit = {
    sendRAW(s"PRIVMSG $dest :$msg")
  }
}