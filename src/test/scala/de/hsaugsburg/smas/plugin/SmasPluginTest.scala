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

import base.{PluginState, SmasPlugin}
import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import de.hsaugsburg.smas.node.SmasNode


@RunWith(classOf[JUnitRunner])
class SmasPluginTest extends WordSpec with ShouldMatchers with MustMatchers
{

  var testResult = false

  "A SmasPlugin" should
    {

      " change his state while lifecycle" in
        {
          val plugin = new TestPlugin()

          plugin.getPluginState == PluginState.Created must be === true

          plugin.start must be === true
          plugin.getPluginState == PluginState.Started must be === true
          testResult must be === true
          plugin.isReady must be === true
          
          testResult = false

          plugin.stop must be === true
          plugin.getPluginState == PluginState.Stopped must be === true
          testResult must be === true
        }

      " has the correct surrounding node" in
        {
          val plugin = new TestPlugin()

          val node = new TestNode()
          plugin.setSurroundingNode(node)

          plugin.getSurroundingNode == node must be === true
        }
    }

  class TestNode extends SmasNode
  {
    def onStart() {true}

    def onStop() {true}
  }

  class TestPlugin extends SmasPlugin
  {
    def onStop: Boolean = {testResult = true; true}

    def onStart = {testResult = true; true}

    def getSurroundingNode = node
  }
}

