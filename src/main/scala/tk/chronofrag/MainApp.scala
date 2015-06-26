package tk.chronofrag

import java.net.InetSocketAddress

import akka.actor.ActorSystem

/**
 * MainApp will be used to test the functionality of the other classes in one off scenarios along with the unit tests.
 */
object MainApp extends App {
  val system = ActorSystem("IrcSystem")
  val props = Client.props(new InetSocketAddress("irc.coldfront.net",6667))
  val client = system.actorOf(props, name = "client")
}
