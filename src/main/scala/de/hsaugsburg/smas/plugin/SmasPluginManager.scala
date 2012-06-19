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

package de.hsaugsburg.smas.plugin

import base.{PluginState, SmasPlugin, PluginManager}
import collection.mutable.ListBuffer
import de.hsaugsburg.smas.communication.BaseMessage
import java.lang.reflect.{Modifier, Method}
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry}

class SmasPluginManager extends PluginManager
{
  def findMethodsForMessage(msg: BaseMessage, pluginName: String = null): List[MethodWrapper] =
  {
    log.debug("Searching methods for %s".format(msg))
    val result = new ListBuffer[MethodWrapper]()
    val msgName = getClassNameWithOutPackage(msg.getClass.getName)

    if(msg != null)
    {
      if(pluginName == null)
      {
        log.debug("Handling external message...")
        for(plugin <- pluginList.values)
        {
           result ++= gatherPluginMethods (plugin, msg, msgName)
        }
      }
      else
      {
        log.debug("Handling internal message...")
        if(pluginList.contains(pluginName))
        {
          val pluginOption = pluginList.get(pluginName)
          if (pluginOption != None)
          {
            result ++= gatherPluginMethods (pluginOption.get, msg, msgName)
          }
        }
      
      }
    }

    result.toList
  }

  def gatherPluginMethods(plugin: SmasPlugin, msg: BaseMessage, msgName: String): List[MethodWrapper] =
  {
    var result = new ListBuffer[MethodWrapper]()

    log.debug("Possible plugin: %s for msg %s".format(plugin, msgName))
    for(pluginMethod <- checkPluginForHandleMethod(msgName, plugin))
    {
      log.debug("Method found: %s".format(pluginMethod))
      result += MethodWrapper(plugin, pluginMethod, msg)
    }

    result.toList
  }

  def getAllRegisteredPluginNames: List[String] =
  {
    val result = new ListBuffer[String]()

    for (key <- pluginList.keys)
    {
      result += key
    }

    result.toList
  }

  private def getClassNameWithOutPackage(name: String): String =
  {
    var result = splitStringAndReturnLastPart('.', name)

    if(result.contains('$'))
    {
      // PluginManagerTest$InnerMessage
      result = splitStringAndReturnLastPart('$', result)
    }

    result
  }

  private def splitStringAndReturnLastPart(splitChar: Char, toSplit: String): String =
  {
    val splitted = toSplit.split(splitChar)

    splitted(splitted.length - 1)
  }

  def getAddressForPluginName(name: String): AddressBookEntry =
  {
    var result: AddressBookEntry = null
    if(pluginList.contains(name))
    {
      result = AddressBookEntryFactory.getAddressBookEntry(name, null, 0, true)
    }

    result
  }

  private def checkPluginForHandleMethod(nameOfMessage: String, plugin: SmasPlugin): List[Method] =
  {
    val result = new ListBuffer[Method]()
    val pluginMethods = plugin.getClass.getMethods

    for(method <- pluginMethods)
    {
      if(method.getModifiers == Modifier.PUBLIC && (method.getName == "handle" + nameOfMessage || method.getName == "handleAll"))
      {
        result += method
      }
    }

    result.toList
  }

  def registerPlugin(pluginName: String, plugin: SmasPlugin): Boolean =
  {
    registerPlugin(plugin, pluginName)
  }

  def registerPlugin(plugin: SmasPlugin, pluginName: String): Boolean =
  {
    log.debug("Try to register Plugin: " + pluginName + ", " + plugin)

    if(pluginList.putIfAbsent(pluginName, plugin) == None)
    {
      log.info("Plugin was registered: " + pluginName + ", " + plugin)
      return true
    }

    log.warn("Plugin was not registered: " + pluginName + ", " + plugin)
    false
  }

  def startAllPlugins: Boolean =
  {
    log.debug("Starting all plugins...")

    var result = true

    for(plugin: SmasPlugin <- pluginList.values)
    {
      if(!plugin.start)
      {
        log.warn("Plugin was not started: %s".format(plugin))
        result = false
      }
    }

    result
  }

  def stopAllPlugins: Boolean =
  {
    log.debug("Stopping all plugins...")

    var result = true

    for(plugin: SmasPlugin <- pluginList.values.filter(p => p.getPluginState == PluginState.Created))
    {
      if(!plugin.stop)
      {
        log.warn("Plugin was not stopped: %s".format(plugin))
        result = false
      }
    }

    result
  }

  def unRegisterPlugin(pluginName: String): Boolean =
  {
    log.debug("Try to unregister Plugin: " + pluginName)
    var result = false

    val plugin = pluginList.remove(pluginName) match
    {
      case Some(item) => {log.debug("Plugin removed"); item}
      case None => null
    }

    if(plugin != null && plugin.stop && plugin.getPluginState == PluginState.Stopped)
    {
      log.info("Plugin was unregistered: " + pluginName)
      result = true
    }
    else
    {
      log.warn("Plugin was not unregistered: " + pluginName)
    }

    result
  }
}

case class MethodWrapper(private val obj: AnyRef, private val method: Method, message: BaseMessage) extends Runnable
{
  def handle = {method.invoke(obj, message)}

  def run()
  {
    handle
  }
}