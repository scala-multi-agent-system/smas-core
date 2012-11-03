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

package de.hsaugsburg.smas.playground

import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry}
import de.hsaugsburg.smas.communication.{SmasCommunicationFactory, BaseMessage}
import de.hsaugsburg.smas.startup.XmlSystemBuilder

object HelloWorldNodeExample
{
  def main(args: Array[String])
  {
    val configFile = "/examples/helloWorld/helloWorldNode.xml"
    val helloWorldNode = XmlSystemBuilder.runOverXmlFileAndBuildSystem(configFile)
    println(helloWorldNode)
  }
}

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
object HelloWorldExample
{
  def main(args: Array[String])
  {
    val configFile = "/examples/helloWorld/helloWorld.xml"
    val initialNode = XmlSystemBuilder.runOverXmlFileAndBuildSystem(configFile).last
    println(initialNode)

    val msg = InitialMessage(initialNode, initialNode)
    msg.sender(initialNode)
    SmasCommunicationFactory.getInstance.sendMessage(msg)
  }
}

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
class HelloWorldPlugin extends SmasPlugin
{
  def onStop = true
  def onStart = true

  def handleHelloWorldMessage(msg: HelloWorldMessage)
  {
    println("HelloWorld received from %s".format(msg.sender))
    node ! HelloWorldMessage(msg.sender)
  }
}

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
class InitialPlugin extends SmasPlugin
{
  def onStop = true
  def onStart = true

  def handleInitialMessage(msg: InitialMessage)
  {
    println("Initial received...")
    node ! HelloWorldMessage(msg.target)
  }

  def handleHelloWorldMessage(msg: HelloWorldMessage)
  {
    println("HelloWorld received...")
  }
}


case class HelloWorldMessage(rx: AddressBookEntry) extends BaseMessage(rx)
case class InitialMessage(rx: AddressBookEntry, target: AddressBookEntry) extends BaseMessage(rx)