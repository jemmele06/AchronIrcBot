package tk.chronofrag

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}

case class IncomingRawMessage(prefix: UserMask, command: String, destination: String, message: String)
case class UserMask(nick: String, name: String, hostmask: String)

/**
* Client actor establishes connection to IRC server and launches subsystems to handle incoming messages.
*/
object Client {
  def props(remote: InetSocketAddress) =
    Props(classOf[Client], remote)
}

class Client(remote: InetSocketAddress) extends Actor {

  import Tcp._
  import context.system

  IO(Tcp) ! Connect(remote)

  def receive = {
    case CommandFailed(_: Connect) =>
      context stop self

    case Connected(remoteAddr, local) =>
      val connection = sender()
      connection ! Register(self)

      context become {
        case CommandFailed(w: Write) =>
          // O/S buffer was full
        case Received(data) =>
          val incoming = data.utf8String.split("\r?\n|\r").toList
          for(line <- incoming) {
            val ircMessage = process(connection, line)
            context.actorOf(Props(classOf[CommandActor], connection)) ! ircMessage
          }
        case "close" =>
          connection ! Close
        case _: ConnectionClosed =>
          context stop self
      }
  }

  def process(connection: ActorRef, data: String): IncomingRawMessage = {
    //Regex taken from: http://www.mybuddymichael.com/writings/a-regular-expression-for-irc-messages.html
    val IrcRegex = "^(?:[:](\\S+) )?(\\S+)(?: (?!:)(.+?))?(?: [:](.+))?$".r
    val IrcRegex(prefix, command, destination, message) = data

    val UserMaskRegex = "([^!]*)!?([^@]*)?@?(.*)?".r
    val UserMaskRegex(nickname, username, dns) = Option(prefix) getOrElse ""
    val usermask = UserMask(nickname, username, dns)

    println(s"p($usermask) c($command) d($destination) m($message)")
    new IncomingRawMessage(usermask, command, destination, message)
  }
}



