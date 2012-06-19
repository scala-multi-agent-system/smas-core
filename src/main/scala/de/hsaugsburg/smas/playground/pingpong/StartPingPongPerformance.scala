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

package de.hsaugsburg.smas.playground.pingpong

import de.hsaugsburg.smas.startup.XmlSystemBuilder

object StartPingPongPerformance
{
  val configFile = "/examples/pingPong/pong.xml"

  def main(args: Array[String])
  {
    var pingPongCount = 0
    println("current memory: " + Runtime.getRuntime.freeMemory())
    while (pingPongCount < 1500)
    {
      XmlSystemBuilder.runOverXmlFileAndBuildSystem(configFile)

      // TODO [low] create PingNode to simulate more load
      // Thread.sleep(100L)
      //BasicSystemBuilder.getNode(manager, classOf[PingNode], HashMap("PingPlugin" -> classOf[PingPlugin]), "localhost", 2552)
      pingPongCount += 1
      println(pingPongCount)

      println("current memory: " + Runtime.getRuntime.freeMemory())
    }
    println("current memory: " + Runtime.getRuntime.freeMemory())

  }

}
