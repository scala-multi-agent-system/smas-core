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

import PluginState._
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.configuration.ConfigurationManager
import de.hsaugsburg.smas.node.SmasNode
import de.hsaugsburg.smas.communication.BaseMessage

trait SmasPlugin extends Serializable
{

  protected var state: PluginState = PluginState.Created;
  protected var node: SmasNode = null
  protected val log = LoggerFactory.getLogger(this.getClass)

  def getPluginState: PluginState = {state}
  def isReady: Boolean = {state == PluginState.Started}
  def onStop: Boolean
  def onStart: Boolean

  def start: Boolean =
  {
    if(onStart)
    {
      startDone
    }
    else
    {
      false
    }
  }

  def stop: Boolean =
  {
    if(onStop)
    {
      stopDone
    }
    else
    {
      false
    }
  }

  implicit def combineNodeAndMessage(msg: BaseMessage): (SmasNode, BaseMessage) =
  {
    (node, msg)
  }

  def restart: Boolean = {stop && start}
  def setSurroundingNode(surroundingNode: SmasNode) {this.node = surroundingNode}

  //protected def !(msg: BaseMessage) {node !(msg)}
  //protected def ??(request: Requests): List[AddressBookEntry] = {node ??(request)}

  protected def getConfig(key: String): String = {ConfigurationManager().getProperty(this.getClass.getName+"."+key)}
  protected def getIntConfig(key: String): Int = {ConfigurationManager().getIntProperty(this.getClass.getName+"."+key)}

  private def startDone: Boolean = {state = PluginState.Started; true}
  private def stopDone: Boolean = {state = PluginState.Stopped; true}
}