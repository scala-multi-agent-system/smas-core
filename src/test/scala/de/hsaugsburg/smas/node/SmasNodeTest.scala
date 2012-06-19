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

package de.hsaugsburg.smas.node

import org.scalatest.WordSpec
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.communication.BaseMessage
import java.lang.Thread
import de.hsaugsburg.smas.services.messages.{MemberResponse, ServiceResponse}
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry, NamingPlugin}
import de.hsaugsburg.smas.services.{RequestForMember, RequestForService, RequestForPlugin}

@RunWith(classOf[JUnitRunner])
class SmasNodeTest extends WordSpec with ShouldMatchers with MustMatchers
{
  val testerAddress = AddressBookEntryFactory.getAddressBookEntry("tester", "testerHost", 10000)
  val probandAddress = AddressBookEntryFactory.getAddressBookEntry("proband", "probandHost", 20000)

  var testResult = false
  val waitTime = 100

  "A SmasNode" should
    {
      "handle a message correct" in
        {
          val node = new TestNode()
          node.setOwnAddress(probandAddress)
          node.registerPlugin("TestPlugin", new TestPlugin())
          val msg = TestMessage(probandAddress)
          msg.sender(testerAddress)
          node.receive(msg)

          Thread.sleep(waitTime)

          testResult must be === true

          testResult = false
          node.stop()
        }

      "set the correct own location if not a correct was set" in
        {
          val node = new TestNode()
          val newMe = AddressBookEntryFactory.getAddressBookEntry(node.getAddress.getId, probandAddress.getHost, probandAddress.getPort)
          node.setOwnAddress(newMe)

          node.getAddress.getId must be === newMe.getId
          node.getAddress.getHost must be === newMe.getHost
          node.getAddress.getPort must be === newMe.getPort

          if(node.getAddress.getPublicKey != null)
            node.getAddress.getPublicKey != newMe.getPublicKey must be === true

          val oldMe = node.getAddress
          node.setOwnAddress(AddressBookEntryFactory.getAddressBookEntry("jdldjk", "fkdjfkdjfksl", 1192))
          node.getAddress must be === oldMe

          node.stop()
        }

      "set the correct manager" in
        {
          val manager = testerAddress
          val node = new TestNode()
          node.setManager(manager)

          node.getManager must be === manager

          node.stop()
        }

      "handle a plugin request and a internal message correct" in
        {
          val node = new TestNode()
          val testPlugin = new TestPlugin()
          testResult = false
          node.registerPlugin("TestPlugin", testPlugin)

          val pluginAddress = (node ?? RequestForPlugin("TestPlugin"))(0)

          pluginAddress.getId must be === "TestPlugin"
          pluginAddress.getHost must be === null
          pluginAddress.getPort must be === 0
          pluginAddress.isInternal must be === true
          pluginAddress.isComplete must be === true

          val msg = TestMessage(pluginAddress)
          msg.sender(testerAddress)
          node.receive(msg)

          Thread.sleep(waitTime)
          testResult must be === true

          testResult = false
          node.stop()

        }

      "handle a responseName request correct" in
        {
          val node = new TestNode()
          val namingPlugin = new NamingPlugin()
          namingPlugin.setSurroundingNode(node)
          node.registerPlugin("NamingPlugin", namingPlugin)

          var externalRequest: AddressBookEntry = null
          val serviceLoc = AddressBookEntryFactory.getAddressBookEntry("<ServiceLoc>", "<Test>", 42)
          val default = AddressBookEntryFactory.getAddressBookEntry("<Test>", "<Test>", 1337)

          new Thread(new Runnable {
            def run()
            {
              externalRequest = (node ?? RequestForService("TestService"))(0)
            }
          }).start()


          Thread.sleep(waitTime)
          val response = ServiceResponse("TestService", List(serviceLoc))
          response.sender(testerAddress)
          response.receiver(default)
          node.receive(response)

          Thread.sleep(waitTime)
          externalRequest must be === serviceLoc

          node.stop()
        }

      "handle a member request correct" in
        {
          val node = new TestNode()
          val namingPlugin = new NamingPlugin()
          namingPlugin.setSurroundingNode(node)
          node.registerPlugin("NamingPlugin", namingPlugin)

          var externalRequest: AddressBookEntry = null
          val memberLoc = AddressBookEntryFactory.getAddressBookEntry("<ServiceLoc>", "<Test>", 42)
          val default = AddressBookEntryFactory.getAddressBookEntry("<Test>", "<Test>", 1337)

          new Thread(new Runnable {
            def run()
            {
              externalRequest = (node ?? RequestForMember("Member"))(0)
            }
          }).start()


          Thread.sleep(waitTime)
          val response = MemberResponse("Member", memberLoc)
          response.sender(testerAddress)
          response.receiver(default)
          node.receive(response)

          Thread.sleep(waitTime)
          externalRequest must be === memberLoc

          node.stop()
        }

      "has a lifecycle" in
        {
          val node = new TestNode()
          node.setOwnAddress(probandAddress)
          node.start()

          testResult must be === true
          testResult = false

          node.stop()
          testResult must be === true
          testResult = false
        }
    }

  class TestNode extends SmasNode
  {
    def onStart() {testResult = true}

    def onStop() {testResult = true}
  }

  class TestPlugin extends SmasPlugin
  {
    def onStop = true

    def onStart = true

    def handleTestMessage(msg: TestMessage)
    {
      testResult = true
    }
  }
}

case class TestMessage(rx: AddressBookEntry) extends BaseMessage(rx)
