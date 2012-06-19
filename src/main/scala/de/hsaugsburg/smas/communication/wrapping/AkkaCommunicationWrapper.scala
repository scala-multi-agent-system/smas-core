/*
 * SMAS - Scala Multi Agent System
 * Copyright (C) 2012  Christian Ego, Rico Lieback
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package de.hsaugsburg.smas.communication.wrapping

import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.communication.BaseMessage
import de.hsaugsburg.smas.node.SmasNode
import akka.actor.{PoisonPill, Actor}


class AkkaCommunicationWrapper extends Actor
{
  private var nodeInstance: SmasNode = null
  private val log = LoggerFactory.getLogger(this.getClass)

  protected def receive =
  {
    case TakeNode(instance) =>
    {
      log.debug("Node instance will be stored & started...")
      this.nodeInstance = instance;
      this.nodeInstance.start()
      become(ready)
    }
    case msg: AnyRef => log.warn("There was a message (%s) which can not be handled at the moment".format(msg))
  }

  protected def ready: Receive =
  {
    case KillNode =>
    {
      log.debug("Node instance will be stopped...")
      this.nodeInstance.stop()
      this.nodeInstance = null
      self ! PoisonPill
    }
    case msg: BaseMessage =>
    {
      log.debug("Message (%s) form %s to %s will be handled...".format(msg, msg.sender, msg.receiver))
      if (nodeInstance != null)
      {
        nodeInstance.receive(msg)
        log.debug("Message (%s) form %s to %s was delivered".format(msg, msg.sender, msg.receiver))
      }
      else
      {
        log.warn("Node instance is <null>!")
      }
    }

    case msg: AnyRef => log.warn("There was a message (%s) which can not be handled".format(msg))
  }
}