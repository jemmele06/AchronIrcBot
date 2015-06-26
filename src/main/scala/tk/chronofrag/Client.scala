package tk.chronofrag

import java.net.InetSocketAddress

import akka.actor.{Actor, ActorRef, Props}
import akka.io.{IO, Tcp}

case class RawMessage(prefix: UserMask, command: String, destination: String, message: String)
case class UserMask(nick: String, name: String, hostmask: String)

/**
 * Client companion object to declare props with remote server InetSocketAddress
 */
object Client {
  def props(remote: InetSocketAddress) =
    Props(classOf[Client], remote)
}

/**
 * Client actor establishes connection to IRC server and launches subsystems to handle incoming messages. Initial
 * implementation taken from Akka TCP documentation.
 * @param remote InetSocketAddress to the IRC server
 */
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

  /**
   * Processes incoming messages from the server that match RFC 1459 and 2812 for IRC messages. Only to be used after a 
   * successful connection to the server is established.
   * @param connection ActorRef to the TCP actor.
   * @param data String that contains a single line from the IRC server.
   * @return RawMessage instance
   */
  def process(connection: ActorRef, data: String): RawMessage = {
    //TODO - Regex allows null values for unmatched groups. Possible fixes are: (1) wrap in Option (2)remake Regex (3) let null leak
    //Regex taken from: http://www.mybuddymichael.com/writings/a-regular-expression-for-irc-messages.html
    val IrcRegex = "^(?:[:](\\S+) )?(\\S+)(?: (?!:)(.+?))?(?: [:](.+))?$".r
    val IrcRegex(prefix, command, destination, message) = data

    val UserMaskRegex = "([^!]*)!?([^@]*)?@?(.*)?".r
    val UserMaskRegex(nickname, username, dns) = Option(prefix) getOrElse "" //if prefix null, replace with empty string
    val usermask = UserMask(nickname, username, dns)

    println(s"U($usermask)C($command)D($destination)M($message)")
    new RawMessage(usermask, command, destination, message)
  }
}



