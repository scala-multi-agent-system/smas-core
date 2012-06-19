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

import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.naming.AddressBookEntry
import collection.JavaConversions.JConcurrentMapWrapper
import java.util.concurrent.ConcurrentHashMap
import messages._
import java.util.{TimerTask, Timer}
import de.hsaugsburg.smas.configuration.ConfigurationManager


class HolonManagerPlugin extends SmasPlugin
{
  private val serviceTable = new JConcurrentMapWrapper[String, List[AddressBookEntry]](new ConcurrentHashMap[String, List[AddressBookEntry]]())
  private val serviceRequestCache = JConcurrentMapWrapper[String, List[AddressBookEntry]](new ConcurrentHashMap[String, List[AddressBookEntry]]())

  private val memberTable = JConcurrentMapWrapper[String, AddressBookEntryAliveWrapper](new ConcurrentHashMap[String, AddressBookEntryAliveWrapper]())
  private val memberRequestCache = JConcurrentMapWrapper[String, List[AddressBookEntry]](new ConcurrentHashMap[String,  List[AddressBookEntry]]())

  private val holonTable = JConcurrentMapWrapper[String, AddressBookEntry](new ConcurrentHashMap[String, AddressBookEntry]())

  private val configurationManager = ConfigurationManager()
  private val cacheUpdateTimer = new Timer()
  private val isAliveMissesForTimeout = configurationManager.getIntProperty("de.hsaugsburg.smas.services.HolonManagerPlugin.missesForTimeout")
  private val updateInterval = configurationManager.getIntProperty("de.hsaugsburg.smas.services.HolonManagerPlugin.updateInterval")

  def onStop =
  {
    cacheUpdateTimer.purge()
    true
  }

  def onStart =
  {
    cacheUpdateTimer.schedule(new TimerTask
    {
      def run()
      {
        checkForNodesToBeAlive()
      }
    }, updateInterval)

    true
  }

  private [services] def getServiceCount: Int =
  {
    serviceTable.size
  }

  private[services] def getServiceAddressesCount: Int =
  {
    serviceTable.values.map(_.size).reduceLeft(_ + _)
  }

  private [services] def getHolonCount: Int =
  {
    holonTable.size
  }

  private [services] def getMemberCount: Int =
  {
    memberTable.size
  }

  private [services] def getServiceRequestCacheCount: Int =
  {
    serviceRequestCache.size
  }

  private [services] def getMemberRequestCacheCount: Int =
  {
    memberRequestCache.size
  }

  def handleGetAllKnownHolons(msg: GetAllKnownHolons)
  {
    msg.sender ! AllKnownHolons(holonTable.values.toList)
  }


  def handleGetAllKnownMembers(msg: GetAllKnownMembers)
  {
    msg.sender ! AllKnownMembers(memberTable.values.map(_.addressBookEntry).toList)
  }


  def handleServiceRequest(msg: ServiceRequest)
  {
    if(serviceTable.contains(msg.service))
    {
      takeCareOfServiceRequestWhichCanBeAnsweredFromCache(msg)
    }
    else
    {
      takeCareOfServiceRequestWhichCanNotBeAnsweredFromCache(msg)
    }
  }

  private def takeCareOfServiceRequestWhichCanNotBeAnsweredFromCache(msg: ServiceRequest)
  {
    if (!holonTable.contains(msg.sender.getId)) {
      log.info("Request can NOT be answered from cache: '%s'. Start relaying...".format(msg.service))

      updateCache(msg.service, msg.sender, serviceRequestCache)
      relayServiceRequestToAllKnownHolons(msg)
    }
    else
    {
      log.debug("A ServiceRequest from another holon was received. No relaying needed.")
    }
  }

  private def takeCareOfServiceRequestWhichCanBeAnsweredFromCache(msg: ServiceRequest)
  {
    log.info("Request can be answered from cache: '%s'".format(msg.service))
    val serviceLocations: List[AddressBookEntry] = serviceTable.get(msg.service) match
    {
      case Some(item) => item.toList
      case None => Nil
    }

    msg.sender ! ServiceResponse(msg.service, serviceLocations)
    log.debug("Sended answer to %s".format(msg.sender))
  }

  def handleMemberRequest(msg: MemberRequest)
  {
    if(memberTable.contains(msg.memberName))
    {
      takeCareOfMemberRequestWhichCanBeHandledFromCache(msg)
    }
    else
    {
      takeCareOfMemberRequestWhichCanNotBeHandledFromCache(msg)
    }
  }

  private def takeCareOfMemberRequestWhichCanBeHandledFromCache(msg: MemberRequest)
  {
    log.info("Member Request can be answered from cache: '%s'".format(msg.memberName))
    val memberLocation = memberTable.get(msg.memberName) match
    {
      case Some(item) => item.addressBookEntry
      case None => null
    }

    if(memberLocation != null)
    {
      msg.sender ! MemberResponse(msg.memberName, memberLocation)
      log.debug("Request answerd: %s".format(memberLocation))
    }

  }

  private def takeCareOfMemberRequestWhichCanNotBeHandledFromCache(msg: MemberRequest)
  {
    if(!holonTable.contains(msg.sender.getId))
    {
      log.info("Member request can NOT be answered from cache: '%s'. Start relaying...".format(msg.memberName))
      updateCache(msg.memberName, msg.sender, memberRequestCache)
      relayMemberRequestToAllKnownHolons(msg)
    }
    else
    {
      log.debug("Member request from another holon received. No need for relaying.")
    }
  }

  private def relayMemberRequestToAllKnownHolons(msg: MemberRequest)
  {
    for(holon <- holonTable.values)
    {
      if (holon != msg.sender)
      {
        log.debug("Relaying member request to other holon: %s".format(holon))
        holon ! MemberRequest(msg.memberName)
      }
    }
  }

  private def relayServiceRequestToAllKnownHolons(msg: ServiceRequest)
  {
    for(holon <- holonTable.values)
    {
      if(holon != msg.sender)
      {
        log.debug("Relaying responseName request to holon %s".format(holon))
        holon ! ServiceRequest(msg.service)
      }
    }
  }

  private[services] def updateCache[T](index: String, newItem: T, cache: JConcurrentMapWrapper[String, List[T]])
  {
    val newItemList = newItem :: Nil
    if(cache.putIfAbsent(index, newItemList) != None)
    {
      cache.synchronized
      {
        val existingElements = cache.get(index) match
        {
          case Some(item) => item
          case None => Nil
        }

        val newList: List[T] = newItem :: existingElements
        cache.put(index, newList)
      }
    }
  }



  def handleServiceResponse(msg: ServiceResponse)
  {
    if(serviceRequestCache.contains(msg.service))
    {
      for(location <- msg.serviceLocations)
      {
        updateCache(msg.service, location, serviceTable)
      }

      satisfyAllServiceRequestsAndUpdateServiceRequestCache(msg.service, msg.serviceLocations)
      log.debug("Demanded ServiceRequest answered for responseName: %s".format(msg.service))
    }
    else
    {
      log.warn("Not demanded ServiceResponse ignored: %s".format(msg))
    }

  }

  def handleMemberResponse(msg: MemberResponse)
  {
    if(memberRequestCache.contains(msg.memberName))
    {
      updateMemberTable(msg.memberName, msg.memberLocation)
      satisfyAllMemberRequestsAndUpdateMemberRequestCache(msg.memberName, msg.memberLocation)
      log.debug("Demanded MemberRequest answered for member: %s".format(msg.memberName))
    }
    else
    {
      log.warn("Not demanded MemberResponse ignored: %s".format(msg))
    }
  }

  private def updateMemberTable(memberName: String, location: AddressBookEntry): Boolean =
  {
    if (memberTable.putIfAbsent(memberName, AddressBookEntryAliveWrapper(location)) == None && memberTable.contains(memberName))
    {
      log.debug("New member stored in cache: %s".format(location))
      true
    }
    else
    {
      log.warn("Member with this name already exists: %s".format(memberName))
      false
    }
  }

  private def satisfyAllMemberRequestsAndUpdateMemberRequestCache(memberName: String, location: AddressBookEntry)
  {
    val interestedMembers = memberRequestCache.remove(memberName) match
    {
      case Some(item) => item
      case None => Nil
    }

    for(member <- interestedMembers)
    {
      member ! MemberResponse(memberName, location)
    }
  }

  private def satisfyAllServiceRequestsAndUpdateServiceRequestCache(serviceName: String, locations: List[AddressBookEntry])
  {
    val interestedHolonMembers = serviceRequestCache.remove(serviceName) match
    {
      case Some(item) => item
      case None => Nil
    }

    for(member <- interestedHolonMembers)
    {
      member ! ServiceResponse(serviceName, locations)
    }
  }

  def handleRegisterService(msg: RegisterService)
  {
    updateCache(msg.service, msg.location, serviceTable)
  }

  def handleRegisterHolonMember(msg: RegisterHolonMember)
  {
    log.debug("Try to register a new holon member!")
    if(msg.newMember != null && msg.newMember.isComplete && updateMemberTable(msg.newMember.getId, msg.newMember))
    {
      log.info("A new holon member was registered: %s".format(msg.newMember))
    }
    else
    {
      log.warn("This holon member was not registered: %s".format(msg.newMember))
    }
  }

  def handleUnRegisterHolonMember(msg: UnRegisterHolonMember)
  {
    if(msg != null && msg.member.isComplete)
    {
      if (memberTable.remove(msg.member.getId, AddressBookEntryAliveWrapper(msg.member)))
      {
        log.info("A holon member was unregistered: %s".format(msg.member))
      }
      else
      {
        log.warn("This holon member id was not present: %s".format(msg.member))
      }
    }

  }

  def handleRegisterHolon(msg: RegisterHolon)
  {
    val holon = msg.holon
    if(holon != null && msg.holon.isComplete && holonTable.putIfAbsent(msg.holon.getId, holon) == None && holonTable.contains(msg.holon.getId))
    {
      log.debug("New holon registered: %s".format(holon))
    }
    else
    {
      log.warn("Holon was not registered: %s".format(holon))
    }
  }

  def handleUnRegisterHolon(msg: UnRegisterHolon)
  {
    if(holonTable.remove(msg.holon.getId, msg.holon))
    {
      log.debug("Holon was unregistered: %s".format(msg.holon))
    }
    else
    {
      log.warn("Holon was not unregistered: %s".format(msg.holon))
    }
  }

  def handleUnRegisterService(msg: UnRegisterService)
  {
    if(serviceTable.contains(msg.service))
    {
      serviceTable.synchronized
      {
        val serviceLocations = serviceTable.get(msg.service) match
        {
          case Some(item) => item
          case None => Nil
        }

        if(serviceLocations.size == 1)
        {
          serviceTable.remove(msg.service)
        }
        else
        {
          val newServiceLocations = serviceLocations.filter(_ != msg.location)
          serviceTable.put(msg.service, newServiceLocations.toList)
        }
        log.debug("ServiceLocation %s was unregistered".format(msg.service))
      }
    }
  }

  def handleIsAliveResponse(msg: IsAliveResponse)
  {
    memberTable.get(msg.name) match
    {
      case Some(wrapper) => wrapper.receivedIsAliveResponse()
      case None => log.debug("cannot handle isAlive response from member %s, as it is not in the member map".format(msg.name))
    }
  }

  private[services] def checkForNodesToBeAlive()
  {
    checkNodesForTimeout()
    sendNodeIsAliveMessages()
  }

  private[services] def sendNodeIsAliveMessages()
  {
    memberTable.keys.foreach(key =>
    {
      val member = memberTable.get(key).get
      member.sendIsAliveRequest(key)
    })
  }

  private[services] def checkNodesForTimeout()
  {
    serviceTable.synchronized
    {
      memberTable.synchronized
      {
        val timedOut = memberTable.keys.filter(memberTable.get(_).get.isTimedOut)
        val timedOutAddressBookEntries = timedOut.map(memberTable.get(_).get.addressBookEntry).toList

        timedOut.foreach(memberTable.remove(_))
        serviceTable.keys.foreach(key =>
        {
          val services = serviceTable.get(key).get
          val newServices = services.filter(service => ! timedOutAddressBookEntries.contains(service))
          serviceTable.put(key, newServices)
        })
      }
    }
  }

  private case class AddressBookEntryAliveWrapper(addressBookEntry: AddressBookEntry)
  {
    private var missedIsAliveRequests = 0

    def sendIsAliveRequest(name: String)
    {
      addressBookEntry ! IsAliveRequest(name)
      missedIsAliveRequests += 1
    }

    def receivedIsAliveResponse()
    {
      missedIsAliveRequests = 0
    }

    def isTimedOut: Boolean =
    {
      missedIsAliveRequests > isAliveMissesForTimeout
    }

    override def equals(that: Any): Boolean =
    {
      that match
      {
        case wrapper: AddressBookEntryAliveWrapper => wrapper.addressBookEntry == addressBookEntry
        case _ => false
      }
    }
  }
}