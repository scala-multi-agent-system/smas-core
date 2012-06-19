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

import scala.xml._
import de.hsaugsburg.smas.node.SmasNode
import collection.mutable.{ListBuffer, HashMap}
import de.hsaugsburg.smas.naming.AddressBookEntry
import de.hsaugsburg.smas.util.FileUtil

object XmlSystemBuilder
{
  def runOverXmlFileAndBuildSystem(path: String): List[AddressBookEntry] =
  {
    val xml = XML.load(FileUtil.getFileInputStream(path))
    val builtNodes = ListBuffer[AddressBookEntry]()

    assert((xml \ "version" text) == "0.1", "This version of smas xml definition is not supported!")


    for(holon <- xml \ "holons" \ "holon")
    {
      val host = (holon \ "host" text)
      val port = (holon \ "port" text).toInt

      val manager = BasicSystemBuilder.getHolonManager(host, port)
      builtNodes += manager

      for(node <- holon \ "nodes" \ "node")
      {
        val plugins = HashMap[String, Class[_]]()
        for(plugin <- node \ "plugins" \ "plugin")
        {
          plugins += (plugin \ "name" text) -> Class.forName((plugin \ "class" text))
        }

        val agentClass: Class[SmasNode] = Class.forName(node \ "class" text).asInstanceOf[Class[SmasNode]]
        builtNodes += BasicSystemBuilder.getNode(manager, agentClass, plugins.toMap, host, port)
      }
    }
    builtNodes.toList
  }

  def main(args: Array[String])
  {
    require(args.length == 1)
    //"/examples/PingPong.xml"
    runOverXmlFileAndBuildSystem(args(0))
  }

}