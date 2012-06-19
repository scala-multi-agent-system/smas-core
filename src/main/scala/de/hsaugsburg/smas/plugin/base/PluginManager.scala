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

package de.hsaugsburg.smas.plugin.base

import de.hsaugsburg.smas.naming.AddressBookEntry
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.communication.BaseMessage
import de.hsaugsburg.smas.plugin.MethodWrapper
import collection.JavaConversions.JConcurrentMapWrapper
import java.util.concurrent.ConcurrentHashMap


trait PluginManager extends Serializable
{
  protected val log = LoggerFactory.getLogger(this.getClass)
  protected val pluginList = new JConcurrentMapWrapper[String, SmasPlugin](new ConcurrentHashMap[String, SmasPlugin]())

  def findMethodsForMessage(msg: BaseMessage, pluginName: String = null): List[MethodWrapper]
  def registerPlugin(plugin: SmasPlugin, pluginName: String): Boolean
  def registerPlugin(pluginName: String, plugin: SmasPlugin): Boolean
  def unRegisterPlugin(pluginName: String): Boolean
  def getAmountOfPlugins: Int = pluginList.size
  def getAddressForPluginName(name: String): AddressBookEntry
  def startAllPlugins: Boolean
  def stopAllPlugins: Boolean
}