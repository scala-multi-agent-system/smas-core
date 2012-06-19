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
import pingpong.{PongPlugin, PingPlugin, PingNode}

object SystemBuilderEnhanchment
{
  def main(args: Array[String])
  {
    val plugin1 = new PingPlugin().getClass
    val plugin2 = new PongPlugin().getClass
    val plugin3 = new PingNode().getClass

    test(List(plugin1, plugin2, plugin3))

  }

  def test(plugins: List[Class[_]])
  {
    
    for(item <- plugins)
    {
      try
      {
        val pluginClass = item.asInstanceOf[Class[SmasPlugin]]
        val plugin = pluginClass.newInstance()
        println(plugin.getClass)
      }
      catch
      {
        case e: Exception => println("this class is not a plugin class %s".format(item.getClass))
      }
    }
  }
}