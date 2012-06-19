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

package de.hsaugsburg.smas.services

import org.scalatest.WordSpec
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import de.hsaugsburg.smas.util.Randomizer
import de.hsaugsburg.smas.node.SmasNode
import de.hsaugsburg.smas.communication.BaseMessage
import collection.JavaConversions.JConcurrentMapWrapper
import java.util.concurrent.{ConcurrentHashMap, TimeUnit, Executors, CyclicBarrier}
import messages._
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry, NamingPlugin}


@RunWith(classOf[JUnitRunner])
class HolonManagerPluginTest extends WordSpec with ShouldMatchers with MustMatchers
{

  private val threadCount = Runtime.getRuntime.availableProcessors() + 2
  private val itemCount = 500
  private val removeItems = 100

  private var testResult = false
  private val serviceLoc = AddressBookEntryFactory.getAddressBookEntry("<Location>", "<Test>", 1337)
  private val requestSender = AddressBookEntryFactory.getAddressBookEntry("<Sender>", "<Test>", 1337)
  private val defaultAddress = AddressBookEntryFactory.getAddressBookEntry("<default>", "<Test>", 1337)
  private val memberAddress = AddressBookEntryFactory.getAddressBookEntry("Member", "<Test>", 1337)
  private val holonAddress = AddressBookEntryFactory.getAddressBookEntry("<Holon>", "<Test>", 1337)
  private val holon2Address = AddressBookEntryFactory.getAddressBookEntry("<Holon2>", "<Test>", 1337)

  private var memberHolonList: List[AddressBookEntry] = null
  
  "a holonmanagerplugin" should
    {
      "hold services threadsafe" in
        {
          val barrier = new CyclicBarrier(threadCount)
          val hmp = new HolonManagerPlugin()
          val workList = Randomizer.listOfListOfRandomStrings(threadCount, itemCount)
          var workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new ServiceRegTask(hmp, work, barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getServiceCount must be === threadCount*itemCount


          workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new ServiceUnRegTask(hmp, work.drop(removeItems), barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getServiceCount must be === threadCount * (itemCount-(itemCount-removeItems))

        }

      "hold other holons threadsafe" in
        {
          val barrier = new CyclicBarrier(threadCount)
          val hmp = new HolonManagerPlugin()
          val workList = Randomizer.listOfListOfRandomStrings(threadCount, itemCount)
          var workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new HolonRegTask(hmp, work, barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getHolonCount must be === threadCount*itemCount


          workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new HolonUnRegTask(hmp, work.drop(removeItems), barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getHolonCount must be === threadCount * (itemCount-(itemCount-removeItems))
        }

      "hold holon members threadsafe" in
        {
          val barrier = new CyclicBarrier(threadCount)
          val hmp = new HolonManagerPlugin()
          val workList = Randomizer.listOfListOfRandomStrings(threadCount, itemCount)
          var workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new MemberRegTask(hmp, work, barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getMemberCount must be === threadCount*itemCount


          workQueue = Executors.newFixedThreadPool(threadCount)

          for(work <- workList)
          {
            workQueue.submit(new MemberUnRegTask(hmp, work.drop(removeItems), barrier))
          }

          workQueue.shutdown()
          workQueue.awaitTermination(100, TimeUnit.DAYS)

          hmp.getMemberCount must be === threadCount * (itemCount-(itemCount-removeItems))
        }

      "handle service requests correctly if the service is cached" in
        {
          val node = new TestResultTestNode()
          node.setOwnAddress(holonAddress)
          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val holonPlugin = new HolonManagerPlugin()
          holonPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonPlugin)
          val regServMsg = RegisterService(serviceLoc.getId, serviceLoc)
          regServMsg.receiver(holonAddress)
          holonPlugin.handleRegisterService(regServMsg)

          val msg = ServiceRequest(serviceLoc.getId)
          msg.sender(requestSender)
          msg.receiver(holonAddress)
          node.receive(msg)

          Thread.sleep(1000)
          testResult must be === true

          node.stop()
          testResult = false
        }

      "handle service requests correctly if the service is not cached" in
        {
          val node = new TestResultTestNode()
          val holonPlugin = new HolonManagerPlugin()
          val regMsg = RegisterHolon(holon2Address)
          regMsg.receiver(holonAddress)
          holonPlugin.handleRegisterHolon(regMsg)

          holonPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val msg = ServiceRequest(serviceLoc.getId)
          msg.sender(requestSender)
          msg.receiver(holonAddress)
          node.receive(msg)

          Thread.sleep(1000)
          testResult must be === true

          node.stop()
          testResult = false
        }

      "update the service cache correctly" in
        {
          val node = new AdvancedTestNode()
          val holonPlugin = new HolonManagerPlugin()
          val regMsg1 = RegisterHolon(holon2Address)
          regMsg1.receiver(holonAddress)
          holonPlugin.handleRegisterHolon(regMsg1)
          holonPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)


          val serviceMsg = ServiceRequest(serviceLoc.getId)
          serviceMsg.sender(holonAddress)
          serviceMsg.receiver(holon2Address)
          node.receive(serviceMsg)

          holonPlugin.getServiceCount must be === 0

          val servResponse = ServiceResponse(serviceLoc.getId, List(serviceLoc))
          servResponse.sender(holon2Address)
          servResponse.receiver(holonAddress)
          node.receive(servResponse)

          Thread.sleep(1000)

          holonPlugin.getServiceCount must be === 1

          node.stop()
          testResult = false
        }


      "handle member requests correctly if the member is cached" in
        {
          val node = new TestResultTestNode()
          node.setOwnAddress(holonAddress)
          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val holonPlugin = new HolonManagerPlugin()
          holonPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonPlugin)
          val registerHolonMsg = RegisterHolonMember(memberAddress)
          registerHolonMsg.receiver(holonAddress)
          holonPlugin.handleRegisterHolonMember(registerHolonMsg)

          val msg = MemberRequest(memberAddress.getId)
          msg.receiver(holonAddress)
          msg.sender(requestSender)
          node.receive(msg)

          Thread.sleep(1000)
          testResult must be === true

          node.stop()
          testResult = false
        }

      "handle member requests correctly if the member is not cached" in
        {
          val node = new TestResultTestNode()
          val holonPlugin = new HolonManagerPlugin()
          val regMsg = RegisterHolon(holon2Address)
          regMsg.receiver(holonAddress)
          holonPlugin.handleRegisterHolon(regMsg)
          holonPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val memberRequest = MemberRequest(memberAddress.getId)
          memberRequest.receiver(holonAddress)
          memberRequest.sender(requestSender)
          node.receive(memberRequest)

          Thread.sleep(1000)
          testResult must be === true

          node.stop()
          testResult = false
        }

      "update the member cache correctly" in
        {
          val holonManager = new AdvancedTestNode()

          val holonPlugin = new HolonManagerPlugin()
          val regMsg1 = RegisterHolon(holon2Address)
          regMsg1.receiver(holonAddress)
          holonPlugin.handleRegisterHolon(regMsg1)
          holonPlugin.setSurroundingNode(holonManager)
          holonManager.registerPlugin("holon", holonPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(holonManager)
          holonManager.registerPlugin("naming", naming)


          val regMsg2 = MemberRequest(memberAddress.getId)
          regMsg2.receiver(holon2Address)
          regMsg2.sender(holonAddress)
          holonManager.receive(regMsg2)

          holonPlugin.getMemberCount must be === 0

          val responseMsg = MemberResponse(memberAddress.getId, memberAddress)
          responseMsg.sender(holon2Address)
          responseMsg.receiver(holonAddress)
          holonManager.receive(responseMsg)

          // must be so high due to my weak CI machine
          Thread.sleep(3000)

          holonPlugin.getMemberCount must be === 1

          holonManager.stop()
          testResult = false
        }

      "update a cache threadsafe" in
        {
          val plugin = new HolonManagerPlugin()
          val cache = new JConcurrentMapWrapper[String, List[AddressBookEntry]](new ConcurrentHashMap[String, List[AddressBookEntry]]())
          val barrier = new CyclicBarrier(threadCount)
          val pool = Executors.newFixedThreadPool(threadCount)
          val allWork = Randomizer.listOfListOfRandomStrings(threadCount, itemCount)

          for(work <- allWork)
          {
            pool.submit(new Runnable
            {
              def run()
              {
                barrier.await()
                for(item <- work)
                {
                  plugin.updateCache(item, defaultAddress, cache)
                }
              }
            })

          }

          pool.shutdown()
          pool.awaitTermination(100, TimeUnit.DAYS)

          cache.size must be === threadCount*itemCount
        }


      "send all known members and holons" in
        {
          memberHolonList = null
          val holonManager = new MemberHolonTestNode()

          val holonPlugin = new HolonManagerPlugin()
          val anotherRegMsg = RegisterHolon(holon2Address)
          anotherRegMsg.receiver(holonAddress)
          holonPlugin.handleRegisterHolon(anotherRegMsg)

          holonPlugin.setSurroundingNode(holonManager)
          holonManager.registerPlugin("holon", holonPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(holonManager)
          holonManager.registerPlugin("naming", naming)

          val anotherRegMsg2 = RegisterHolonMember(memberAddress)
          anotherRegMsg2.receiver(holonAddress)
          anotherRegMsg2.sender(defaultAddress)
          holonManager.receive(anotherRegMsg2)

          val getMsg = GetAllKnownHolons()
          getMsg.receiver(holonAddress)
          getMsg.sender(defaultAddress)
          holonManager.receive(getMsg)
          Thread.sleep(1500)

          memberHolonList != null must be === true
          memberHolonList.size must be === 1
          memberHolonList must be === List(holon2Address)

          memberHolonList = null

          val getMsg2 = GetAllKnownMembers()
          getMsg2.receiver(holonAddress)
          getMsg2.sender(defaultAddress)
          holonManager.receive(getMsg2)
          Thread.sleep(1500)

          memberHolonList != null must be === true
          memberHolonList.size must be === 1
          memberHolonList must be === List(memberAddress)
         
          holonManager.stop()
        }

      "test purge nodes from plugin manager if they do not respond" in
        {
          val holonManagerPlugin = new HolonManagerPlugin()

          val node = new TestResultTestNode()
          val regMsg = RegisterHolon(holon2Address)
          regMsg.receiver(holonAddress)
          holonManagerPlugin.handleRegisterHolon(regMsg)
          holonManagerPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonManagerPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val registrationMessage = RegisterHolonMember(holon2Address)
          registrationMessage.receiver(holonAddress)
          registrationMessage.sender(defaultAddress)

          holonManagerPlugin.handleRegisterHolonMember(registrationMessage)
          holonManagerPlugin.getMemberCount must be === 1

          (0 until 20).foreach(i => holonManagerPlugin.checkForNodesToBeAlive())

          holonManagerPlugin.getMemberCount must be === 0

          testResult must be === true

          testResult = false
        }

      "test purge services from plugin manager if the node does not respond" in
        {
          val holonManagerPlugin = new HolonManagerPlugin()

          val node = new TestResultTestNode()
          val regMsg = RegisterHolon(holon2Address)
          regMsg.receiver(holonAddress)
          holonManagerPlugin.handleRegisterHolon(regMsg)
          holonManagerPlugin.setSurroundingNode(node)
          node.registerPlugin("holon", holonManagerPlugin)

          val naming = new NamingPlugin()
          naming.setSurroundingNode(node)
          node.registerPlugin("naming", naming)

          val registrationMessage = RegisterHolonMember(holon2Address)
          registrationMessage.receiver(holonAddress)
          registrationMessage.sender(defaultAddress)
          holonManagerPlugin.handleRegisterHolonMember(registrationMessage)

          val serviceMessage = RegisterService("service1", holon2Address)
          serviceMessage.receiver(holonAddress)
          serviceMessage.sender(defaultAddress)
          holonManagerPlugin.handleRegisterService(serviceMessage)

          val serviceMessage2 = RegisterService("service1", defaultAddress)
          serviceMessage2.receiver(holonAddress)
          serviceMessage2.sender(defaultAddress)
          holonManagerPlugin.handleRegisterService(serviceMessage2)

          holonManagerPlugin.getServiceAddressesCount must be === 2

          (0 until 20).foreach(i => holonManagerPlugin.checkForNodesToBeAlive())

          holonManagerPlugin.getServiceAddressesCount must be === 1

          testResult = false
        }
    }

  private class TestResultTestNode extends SmasNode
  {
    def onStart()
    {

    }

    def onStop()
    {

    }

    override def !(msg: BaseMessage)
    {
      msg match
      {
        case castedMessage: ServiceResponse =>
        {
          if(castedMessage.receiver == requestSender &&
            castedMessage.service == serviceLoc.getId &&
            castedMessage.serviceLocations == List(serviceLoc))
          {
            testResult = true
          }
        }

        case castedMessage: MemberResponse =>
        {
          if(castedMessage.receiver == requestSender &&
            castedMessage.memberName == memberAddress.getId &&
            castedMessage.memberLocation == memberAddress)
          {
            testResult = true
          }
        }

        case castedMessage: MemberRequest =>
        {
          if(castedMessage.memberName == memberAddress.getId &&
            castedMessage.receiver == holon2Address)
          {
            testResult = true
          }
        }

        case castedMessage: ServiceRequest =>
        {
          if(castedMessage.service == serviceLoc.getId &&
            castedMessage.receiver == holon2Address)
          {
            testResult = true
          }
        }

        case isAliveRequest: IsAliveRequest => testResult = true
      }


    }
  }

  private class AdvancedTestNode extends SmasNode
  {
    def onStart()
    {}

    def onStop()
    {}

    // filter request so we can inject a response
    override def !(msg: BaseMessage)
    {
      if(!msg.isInstanceOf[ServiceRequest] && !msg.isInstanceOf[MemberRequest])
      {
        super.!(msg)
      }
    }
  }

  private class MemberHolonTestNode extends SmasNode
  {
    def onStart()
    {}

    def onStop()
    {}
    
    override def ! (msg: BaseMessage)
    {
      if(msg.isInstanceOf[AllKnownHolons])
      {
        memberHolonList = msg.asInstanceOf[AllKnownHolons].holons
      }
      else if(msg.isInstanceOf[AllKnownMembers])
      {
        memberHolonList = msg.asInstanceOf[AllKnownMembers].members
      }
    }
  }


  private class ServiceRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val msg = RegisterService(item, defaultEntry)
        msg.receiver(defaultEntry)
        holonManager.handleRegisterService(msg)
      }
    }
  }

  private class ServiceUnRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val msg = UnRegisterService(item, defaultEntry)
        msg.receiver(defaultEntry)
        holonManager.handleUnRegisterService(msg)
      }
    }
  }

  private class HolonRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val address = AddressBookEntryFactory.getAddressBookEntry(item, "<test>", 1337)
        val msg = RegisterHolon(address)
        msg.receiver(defaultEntry)
        holonManager.handleRegisterHolon(msg)
      }
    }
  }

  private class HolonUnRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val address = AddressBookEntryFactory.getAddressBookEntry(item, "<test>", 1337)
        val msg = UnRegisterHolon(address)
        msg.receiver(defaultEntry)
        holonManager.handleUnRegisterHolon(msg)
      }
    }
  }

  private class MemberRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val address = AddressBookEntryFactory.getAddressBookEntry(item, "<test>", 1337)
        val msg = RegisterHolonMember(address)
        msg.receiver(defaultEntry)
        holonManager.handleRegisterHolonMember(msg)
      }
    }
  }

  private class MemberUnRegTask(holonManager: HolonManagerPlugin, work: List[String], barrier: CyclicBarrier) extends Runnable
  {
    def run()
    {
      barrier.await()

      val defaultEntry = AddressBookEntryFactory.getAddressBookEntry("<test>", "<test>", 0)

      for(item <- work)
      {
        val address = AddressBookEntryFactory.getAddressBookEntry(item, "<test>", 1337)
        val msg = UnRegisterHolonMember(address)
        msg.receiver(defaultEntry)
        holonManager.handleUnRegisterHolonMember(msg)
      }
    }
  }

}

