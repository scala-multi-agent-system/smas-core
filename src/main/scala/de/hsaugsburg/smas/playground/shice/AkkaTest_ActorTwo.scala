/*
 * SMAS - Scala Multi Agent System
 * Copyright (C) 2012  Rico Lieback
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package de.hsaugsburg.smas.playground.shice

import akka.actor.Actor._
import akka.actor.Actor
import java.util.concurrent.Executors
import de.hsaugsburg.smas.communication.wrapping.{TakeNode, AkkaCommunicationWrapper}
import de.hsaugsburg.smas.playground.pingpong.{Pong, PingPlugin, PingNode}
import de.hsaugsburg.smas.naming.AddressBookEntryFactory

object AkkaTest_ActorTwo
{
  def main(args: Array[String])
  {
    remote.start("localhost", 2552)
    remote.register("ActorTwoService", actorOf[ActorTwo])
  }
}

object AkkaTest_ActorOne
{
  def main(args: Array[String])
  {
    remote.start("localhost", 2553)
    remote.register("ActorOneService", actorOf[ActorOne])
    val one = remote.actorFor("ActorOneService", remote.address.getHostName, 2553)
    one ! Message("ActorTwoService", "ActorOneService")
  }
}

case class Message(sender: String, receiver: String)

class ActorOne extends Actor
{
  val pool = Executors.newFixedThreadPool(1)
  protected def receive =
  {
    case msg: Message =>
    {
      val actorTwoService = remote.actorFor(msg.sender, remote.address.getHostName, 2552)
      println(msg)
      Thread.sleep(1000)
      actorTwoService ! Message("ActorOneService", msg.sender)
    }
  }
}

class ActorTwo extends Actor
{
  protected def receive =
  {
    case msg: Message =>
    {
      val actorOneService = remote.actorFor(msg.sender, remote.address.getHostName, 2553)
      println(msg)
      Thread.sleep(1000)
      actorOneService ! Message("ActorTwoService", msg.sender)
    }
  }
}

object AkkaTest_WrapperOne
{
  def main(args: Array[String])
  {
    remote.start("localhost", 2553)
    remote.register("WrapperOne", actorOf[AkkaCommunicationWrapper])
    val one = remote.actorFor("WrapperOne", remote.address.getHostName, 2553)
    val node = new PingNode()

    val plugin = new PingPlugin()
    plugin.setSurroundingNode(node)
    node.registerPlugin("PingPlugin", plugin)
    node.start()
    one ! TakeNode(node)
  }
}

object AkkaTest_WrapperUse
{
  def main(args: Array[String])
  {
    remote.start("localhost", 2552)
    val one = remote.actorFor("WrapperOne", remote.address.getHostName, 2553)
    one ! Pong()
  }
}


