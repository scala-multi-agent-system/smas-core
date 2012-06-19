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

import base.{SmasPlugin, PluginManager}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.WordSpec
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import collection.mutable.ListBuffer
import java.util.concurrent.{TimeUnit, CyclicBarrier, Executors}
import de.hsaugsburg.smas.naming.AddressBookEntry
import de.hsaugsburg.smas.communication.BaseMessage
import de.hsaugsburg.smas.util.Randomizer


@RunWith(classOf[JUnitRunner])
class PluginManagerTest extends WordSpec with ShouldMatchers with MustMatchers
{
  private val itemCount = 150

  "A PluginManager" should
    {

      "register and unregister plugins correct and thread save" in
        {

          val threadCount = Runtime.getRuntime.availableProcessors() + 2
          val pluginManager = new SmasPluginManager()
          val pluginNames = Randomizer.listOfListOfRandomStrings(threadCount, itemCount)

          //put plugins
          var pool = Executors.newFixedThreadPool(threadCount)
          val barrier = new CyclicBarrier(threadCount)

          for(list <- pluginNames)
          {
            pool.submit(new RegisterTask(barrier, list, pluginManager))
          }

          pool.shutdown()
          pool.awaitTermination(15, TimeUnit.SECONDS)
          pool.shutdownNow()


          //tests
          var pluginList = pluginManager.getAllRegisteredPluginNames

          pluginList.size must be === itemCount*threadCount

          for(list <- pluginNames)
          {
            for(pluginName <- list)
            {
              pluginList.contains(pluginName) must be === true
            }
          }


          //remove plugins
          pool = Executors.newFixedThreadPool(threadCount)
          val reducedLists = new ListBuffer[List[String]]()

          for(list <- pluginNames)
          {
            val reduced = list.drop(50)
            println(reduced.size)
            reducedLists += reduced
            pool.submit(new UnRegisterTask(barrier, reduced, pluginManager))
          }

          pool.shutdown()
          pool.awaitTermination(15, TimeUnit.SECONDS)
          pool.shutdownNow()

          //tests
          pluginList = pluginManager.getAllRegisteredPluginNames
          pluginList.size must be === (50) * threadCount

          for(list <- reducedLists)
          {
            for(pluginName <- list)
            {
              pluginList.contains(pluginName) must be === false
            }
          }
        }

      "get the correct address for a plugin" in
        {
          val pluginManager = new SmasPluginManager()
          pluginManager.registerPlugin("Test", new TestPlugin())
          
          val plugin = pluginManager.getAddressForPluginName("Test")
          plugin.getId must be === "Test"
          plugin.getHost must be === null
          plugin.getPort must be === 0
          plugin.isInternal must be === true
        }
      
      "find the methods for a message" in
        {
          val pluginManager = new SmasPluginManager()
          pluginManager.registerPlugin("Test", new TestPlugin())
          pluginManager.registerPlugin("Test2", new TestPlugin())

          var methods = pluginManager.findMethodsForMessage(TestMessage("Hello World", null))
          methods.size must be === 4


          methods = pluginManager.findMethodsForMessage(BadTestMessage("Goodbye World", null))
          methods.size must be === 2

        }
      
      "handle inner classes correct" in
      {
        val pluginManager = new SmasPluginManager()
        pluginManager.registerPlugin("Test", new TestPlugin())
        
        val methods = pluginManager.findMethodsForMessage(InnerMessage(null))

        methods.size must be === 2
      }
    }




  private class RegisterTask(barrier: CyclicBarrier, list: List[String], pluginManager: PluginManager) extends Runnable
  {
    def run()
    {
      barrier.await()

      val pluginInstance = new TestPlugin

      for(plugin <- list)
        pluginManager.registerPlugin(plugin, pluginInstance)
    }
  }

  private class UnRegisterTask(barrier: CyclicBarrier, list: List[String], pluginManager: PluginManager) extends Runnable
  {
    def run()
    {
      barrier.await()

      for(plugin <- list)
        pluginManager.unRegisterPlugin(plugin)
    }
  }

  private class TestPlugin extends SmasPlugin
  {
    def onStop = true

    def onStart = true
    
    def handleTestMessage(msg: TestMessage)
    {
      // nothing
    }

    def handleInnerMessage(msg: InnerMessage)
    {
      // nothing
    }

    def handleAll(msg: BaseMessage)
    {
      // nothing
    }
  }

  case class InnerMessage(rx: AddressBookEntry) extends BaseMessage(rx)
}
case class TestMessage(name: String, private val rx: AddressBookEntry) extends BaseMessage(rx)
case class BadTestMessage(name: String, private val rx: AddressBookEntry) extends BaseMessage(rx)
