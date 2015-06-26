package tk.chronofrag

import akka.actor.{Actor, ActorRef}
import akka.io.Tcp.Write
import akka.util.ByteString

/**
 * CommandActor receives RawMessages and pattern matches over the command.
 * @param connection ActorRef to the TCP actor.
 */
class CommandActor(connection: ActorRef) extends Actor {
  //TODO - Add support for user defined values. Get rid of initial debugging strings!
  val (userMsg, nickMsg, joinMsg) = (
    "tyrname tyrrealname localhost :JRaccoon",
    "tyrbot5",
    "#achrontest")

  def receive = {
    /**
     * PING - Each PING should be responded to with a matching PONG
     */
    case RawMessage(_, "PING", _, message) =>
      pong(message)

    /**
     * NOTICE - Used to determine when the server begins communication
      */
    case RawMessage(_, "NOTICE", destination, message) => destination match {
      case "AUTH" => message match {
        case s: String if s.contains("Looking up your hostname") =>
          user(userMsg)
          nick(nickMsg)
        case _ => //Silently ignore other messages from AUTH destination
      }
      case _ => //Silently ignore other destinations from NOTICE commands
    }

    /**
     * 376 - Numeric command response signaling the end of MOTD. After this it should be safe to send JOIN messages.
     */
    case RawMessage(_, "376", _, _) =>
      join(joinMsg)

    /**
     * PRIVMSG - If message is received, echo the message back to the user or channel that sent it.
     */
    case RawMessage(user, "PRIVMSG", dest, msg) =>
      if(dest == nickMsg) //If sent directly to this bot, reply to directly to the sending user
        say(msg, user.nick)
      else //Else it must have been sent to a channel the bot is in; reply to that channel.
        say(msg, dest)

    case _ => //Silently ignore other commands
  }

  /**
   * sendRaw wraps a String in a ByteString which is wrapped in a TCP.Write and finally sent to the TCP connection
   * actor.
   * @param message String that should be sent to the server
   */
  def sendRAW(message: String): Unit = {
    //TODO - String should be escaped before sending
    connection ! Write(ByteString(message + "\n"))
  }

  /**
   * pong sends a raw PONG IRC message
   * @param message Message that should be sent as part of PONG response.
   */
  def pong(message: String): Unit = {
    sendRAW(s"PONG :$message")
  }

  /**
   * user sends a raw USER IRC message
   * @param user Message that should be sent as part of USER message. Should be in format "<username> <hostname> <servername> <realname>"
   */
  def user(user: String): Unit = {
    sendRAW(s"USER $user")
  }

  /**
   * nick sends a raw NICK IRC message
   * @param nick Message that should be sent as part of NICK message. Should conform to IRC nickname creation.
   */
  def nick(nick: String): Unit = {
    sendRAW(s"NICK $nick")
  }

  /**
   * join sends a raw JOIN IRC message
   * @param channel Message that should be sent as part of JOIN message. Represents the channel list to join.
   */
  def join(channel: String): Unit = {
    sendRAW(s"JOIN $channel")
  }

  /**
   * say sends a raw PRIVMSG IRC message to the given destination
   * @param msg Message that should be sent as part of PRIVMSG message.
   * @param dest Nick of the user or channel to send message.
   */
  def say(msg: String, dest: String): Unit = {
    sendRAW(s"PRIVMSG $dest :$msg")
  }
}