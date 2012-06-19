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

package de.hsaugsburg.smas.communication

import akka.actor.Actor._
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.util.StringHolder
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry}
import de.hsaugsburg.smas.node.SmasNode
import wrapping.{TakeNode, AkkaCommunicationWrapper}

object SmasCommunicationAkka extends SmasCommunication
{
  val log = LoggerFactory.getLogger(this.getClass)

  def performPreCommunication()
  {
    performPreCommunication("", 0)
  }


  
  def performPreCommunication(host: String, port: Int)
  {
    if(!remote.isRunning)
    {
      if(host == "" || port == 0)
      {
        remote.start()
      }
      else
      {
        remote.start(host, port)
      }
    }
  }

  def sendMessage(msg: BaseMessage)
  {
    if(!validateMessage(msg))
    {
      log.warn("Message (%s) can not be handled: %s".format(msg.getClass.getName, msg))
    }
    else
    {
      log.debug("Message (%s) will be sent".format(msg))
      val actor = remote.actorFor(msg.receiver.getId, msg.receiver.getHost, msg.receiver.getPort)
      if(actor != null && actor.isRunning)
      {
        actor ! msg
      }
    }
  }

  def !(msg: BaseMessage)
  {
    sendMessage(msg)
  }

  private def validateMessage(msg: BaseMessage): Boolean =
  {
    var result = false

    if(msg != null && msg.receiver != null && msg.sender != null
      && (msg.receiver.isComplete || msg.receiver.isInternal)
      && (msg.sender.isComplete || msg.sender.isInternal))
    {
      result = true
    }

    result
  }

  def makeSystemSpecificRegistering[T <: SmasNode](node: T, host: String, port: Int)
  {
    remote.register(node.getUniqueName, actorOf[AkkaCommunicationWrapper])
    val nodeWrapping = remote.actorFor(node.getUniqueName, remote.address.getHostName, remote.address.getPort)
    nodeWrapping ! TakeNode(node)
  }

  def prepareCommunication(host: String, port: Int)
  {
    performPreCommunication(host, port)
  }

  //TODO [low] maybe there is a better solution for the holon entry realization
  def buildInitialHolonIfNecessary[T <: SmasNode](initHolon: T)
  {
    val init = remote.actorFor(StringHolder.INITIAL_HOLON_SERVICE, remote.address.getHostName, remote.address.getPort)

    if(init == null || init.toString().contains(StringHolder.INITIAL_HOLON_SERVICE))
    {
      val holon = remote.actorFor(initHolon.getUniqueName, remote.address.getHostName, remote.address.getPort)

      if(holon != null)
      {
        remote.register(StringHolder.INITIAL_HOLON_SERVICE, holon)
      }
    }
  }

  def getInitialHolonService(host: String, port: Int): AddressBookEntry =
  {
    var result: AddressBookEntry = null
    val initial = remote.actorFor(StringHolder.INITIAL_HOLON_SERVICE, host, port)

    if(initial != null)
    {
      result = AddressBookEntryFactory.getAddressBookEntry(StringHolder.INITIAL_HOLON_SERVICE, host, port)
    }

    result
  }
}