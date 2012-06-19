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

package de.hsaugsburg.smas.startup

import collection.immutable.HashMap
import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.node.SmasNode
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.services.messages.{RegisterHolonMember, RegisterHolon}
import de.hsaugsburg.smas.services.{HolonManagerPlugin, HolonManager}
import de.hsaugsburg.smas.naming.{NamingPlugin, AddressBookEntry, AddressBookEntryFactory}
import de.hsaugsburg.smas.communication.{SmasCommunicationFactory, BaseMessage}


object BasicSystemBuilder
{
  private val itsMe = AddressBookEntryFactory.getAddressBookEntry("<BasicSystemBuilder>", "none", 1)
  private val logger = LoggerFactory.getLogger(this.getClass)
  private val communication = SmasCommunicationFactory.getInstance

  def introduceTwoHolonsToEachOther(holonOne: AddressBookEntry, holonTwo: AddressBookEntry)
  {
    sendSystemSpecificMessage(holonTwo, RegisterHolon(holonOne))
    sendSystemSpecificMessage(holonOne, RegisterHolon(holonTwo))
  }

  def sendSystemSpecificMessage(receiver: AddressBookEntry, msg: BaseMessage)
  {
    msg.sender(itsMe)
    msg.receiver(receiver)
    communication ! msg
  }

  private def registerHolonMember(node: AddressBookEntry, holonManager: AddressBookEntry)
  {
    sendSystemSpecificMessage(holonManager, RegisterHolonMember(node))
  }

  def getNode[A <: SmasNode](clazz: Class[A], plugins: Map[String, Class[_]], host: String, port: Int): AddressBookEntry =
  {
    getNode(null, clazz, plugins, host, port)
  }

  //TODO _ <: SmasPlugin?!
  def getNode[A <: SmasNode](manager: AddressBookEntry, clazz: Class[A], plugins: Map[String,  Class[_]], host: String, port: Int): AddressBookEntry =
  {
    prepareCommuniction(host, port)

    var node = clazz.newInstance()
    node = buildNodePlugins(node, plugins)

    val address: AddressBookEntry = AddressBookEntryFactory.getAddressBookEntry(node.getUniqueName, host, port)
    node.setOwnAddress(address)

    if (manager != null)
    {
      node.setManager(manager)
      registerHolonMember(address, manager)
    }

    communication makeSystemSpecificRegistering(node, host, port)

    address
  }

  def getHolonManager(host: String, port: Int): AddressBookEntry =
  {
    prepareCommuniction(host, port)

    var manager: SmasNode = classOf[HolonManager].newInstance()
    manager = buildNodePlugins(manager, HashMap("HolonManagerPlugin" -> classOf[HolonManagerPlugin]))

    val address: AddressBookEntry = AddressBookEntryFactory.getAddressBookEntry(manager.getUniqueName, host, port)
    manager.setOwnAddress(address)

    communication makeSystemSpecificRegistering(manager, host, port)
    buildInitialHolonIfNecessary(manager)

    address
  }

  //TODO [low] maybe there is a better solution for the holon entry realization
  private def buildInitialHolonIfNecessary[T <: SmasNode](initHolon: T)
  {
    communication buildInitialHolonIfNecessary(initHolon)
  }

  private def prepareCommuniction(host: String, port: Int)
  {
    communication prepareCommunication (host,  port)
  }

  private def buildNodePlugins[A <: SmasNode](node: A, plugins: Map[String, Class[_]]): A =
  {
    performPluginRegister(classOf[NamingPlugin].newInstance(), node, classOf[NamingPlugin].getName)

    if(plugins != null)
    {
      for(pluginName <- plugins.keys)
      {
        try
        {
          val pluginInstance = plugins.get(pluginName).get.newInstance().asInstanceOf[SmasPlugin]
          performPluginRegister(pluginInstance, node, pluginName)
        }
        catch
        {
          case e: Exception => logger.warn("This plugin is not a smas plugin: %s".format(pluginName))
        }
      }
    }

    node
  }

  private def performPluginRegister[A <: SmasNode](pluginInstance: SmasPlugin, node: A, pluginName: String): Boolean =
  {
    pluginInstance.setSurroundingNode(node)
    node.registerPlugin(pluginName, pluginInstance)
  }

}
