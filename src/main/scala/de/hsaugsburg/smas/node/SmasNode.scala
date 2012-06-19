/*
 * SMAS - Scala Multi Agent System
 * Copyright (C) 2012  Rico Lieback, Matthias Klass
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package de.hsaugsburg.smas.node

import de.hsaugsburg.smas.plugin.base.{PluginManager, SmasPlugin}
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.services.messages.{MemberRequest, ServiceRequest, ServiceManagementMessage}
import de.hsaugsburg.smas.configuration.ConfigurationManager
import collection.JavaConversions.JConcurrentMapWrapper
import java.util.concurrent.{ConcurrentHashMap, Executors}
import de.hsaugsburg.smas.communication.{SignedMessage, SmasCommunicationFactory, SmasCommunication, BaseMessage}
import de.hsaugsburg.smas.util.ByteObjectHelper
import de.hsaugsburg.smas.cryptographie.{SignedBytes, CryptoEngine}
import java.security.{KeyPair, PublicKey}
import collection.mutable.ListBuffer
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry}
import de.hsaugsburg.smas.services._
import de.hsaugsburg.smas.plugin.{MethodWrapper, SmasPluginManager}

abstract class SmasNode extends Serializable
{
  protected val log = LoggerFactory.getLogger(this.getClass)
  protected val pluginManager: PluginManager = new SmasPluginManager
  protected var me: AddressBookEntry = AddressBookEntryFactory.getAddressBookEntry(genUniqueName, null, 0)
  protected var manager: AddressBookEntry = null
  protected val cfgManager = ConfigurationManager()
  protected val nodeKeys: KeyPair = generateKeys

  private val asynchronousJobs = Executors.newFixedThreadPool(cfgManager.getIntProperty("de.hsaugsburg.smas.nodeconfig.threadPoolSize"))
  private val asynchronousManagementJobs = Executors.newFixedThreadPool(1)
  private val namingRequests = new JConcurrentMapWrapper[String, RequestAndRequestSenderInformation](new ConcurrentHashMap[String, RequestAndRequestSenderInformation]())
  private val trustedSenders = new JConcurrentMapWrapper[String, PublicKey](new ConcurrentHashMap[String, PublicKey]())

  protected val node = this

  private def generateKeys: KeyPair =
  {
    var result: KeyPair = null

    if(cfgManager.getBoolProperty("de.hsaugsburg.smas.nodeconfig.signEnabled"))
    {
      result = CryptoEngine.generateAsyncKeyPair(cfgManager.getIntProperty("de.hsaugsburg.smas.nodeconfig.signKeySize"))
    }

    result
  }

  private def genUniqueName: String =
  {
    getClassNameWithoutPackage + "_" + CryptoEngine.generateUniqueName
  }

  private def getClassNameWithoutPackage: String =
  {
    val splitted = this.getClass.getName.split('.')

    splitted(splitted.length - 1)
  }

  def getAddress: AddressBookEntry = this.me

  def receive(msg: AnyRef)
  {
    log.debug("%s received a message!".format(node))
    msg match
    {
      case msg: SignedMessage =>
      {
        asynchronousJobs.submit(new SignedMessageHandler(msg))
      }
      case msg: BaseMessage =>
      {
        asynchronousJobs.submit(new BaseMessageHandler(msg))
      }
    }
  }

  private def checkIfSenderIsKnownAndTrusted(sender: AddressBookEntry, publicKey: PublicKey): Boolean =
  {
    val formerSenderKey: PublicKey = trustedSenders.putIfAbsent(sender.getId, publicKey) match
    {
      case Some(item) => item
      case None => null
    }

    (formerSenderKey != null && formerSenderKey.equals(publicKey)) || formerSenderKey == null
  }

  //TODO better validation
  private def validateMessage(msg: BaseMessage): Boolean =
  {
    var result = false

    if(msg != null && msg.receiver != null && msg.sender != null
      && (msg.receiver.isComplete || msg.receiver.isInternal)
      && (msg.sender.isComplete || msg.sender.isInternal))
    {
      result = true
    }

    result
  }

  def getManager: AddressBookEntry =
  {
    manager
  }

  def getUniqueName: String =
  {
    me.getId
  }

  def registerPlugin(pluginName: String, plugin: SmasPlugin) =
  {
    pluginManager.registerPlugin(plugin, pluginName)
  }

  def unregisterPlugin(plugin: String) =
  {
    pluginManager.unRegisterPlugin(plugin)
  }

  def setOwnAddress(ownAddress: AddressBookEntry)
  {
    if((this.me == null || !this.me.isComplete) && ownAddress.getId.equals(me.getId))
    {
      val publicKey = if(nodeKeys == null) null else nodeKeys.getPublic
      val newMe = AddressBookEntryFactory.getAddressBookEntry(ownAddress.getId, ownAddress.getHost,
        ownAddress.getPort, ownAddress.isInternal, publicKey)

      this.me = newMe
    }
  }

  def setManager(managerAddress: AddressBookEntry)
  {
    if(this.manager == null || !this.manager.isComplete)
      this.manager = managerAddress
  }

  private def signMessage(msg: BaseMessage): SignedMessage =
  {
    val msgBytes = ByteObjectHelper.getBytesOfAnyRef(msg)
    val signed = CryptoEngine.signBytes(msgBytes, nodeKeys.getPrivate)
    val signedMessage = SignedMessage(msg.receiver, me, msg, signed.sign)

    signedMessage
  }

  implicit def combineNodeAndMessage(msg: BaseMessage): (SmasNode, BaseMessage) =
  {
    (node, msg)
  }

  private def sendExternalMessage(msg: BaseMessage)
  {
    var toSend: BaseMessage = null

    msg.sender(me)

    if (msg.sender == me)
    {
      if (cfgManager.getBoolProperty("de.hsaugsburg.smas.nodeconfig.signEnabled"))
      {
        toSend = signMessage(msg)
      }
      else
      {
        toSend = msg
      }

      if(validateMessage(toSend))
      {
        val communication: SmasCommunication = SmasCommunicationFactory.getInstance
        communication.sendMessage(toSend)
      }
      else
      {
        log.warn("Invalid message was not send: %s".format(toSend))
      }
    }
    else {
      log.warn("Message was dropped due to possible security breach: %s".format(msg))
    }
  }

  def !(msg: BaseMessage)
  {
    if(msg.receiver.isInternal)
    {
      msg.sender(me)
      if(msg.sender == me)
      {
        this.receive(msg)
      }
      else
      {
        log.warn("Internal message was dropped due to possible security breach: %s".format(msg))
      }
    }
    else
    {
      sendExternalMessage(msg)
    }
  }

  def ??(request: Requests): List[AddressBookEntry] =
  {
    var result = List[AddressBookEntry]()
    request match
    {
      case req: RequestForMember => {result = getMemberAddressForName(req.memberName)}
      case req: RequestForService => {result = getServiceAddressForName(req.serviceName)}
      case req: RequestForPlugin => {result = getPluginAddressForName(req.pluginName)}
    }

    result
  }

  private def getMemberAddressForName(memberName: String): List[AddressBookEntry] =
  {
    buildRequestAndWaitForResult(memberName, fireMemberRequest)
  }

  private def fireMemberRequest(request: RequestAndRequestSenderInformation)
  {
    if(manager != null && !request.isFired)
    {
      manager ! MemberRequest(request.getRequestName)
    }
  }

  private def getPluginAddressForName(pluginName: String): List[AddressBookEntry] =
  {
    List(pluginManager.getAddressForPluginName(pluginName))
  }


  def getServiceAddressForName(serviceName: String): List[AddressBookEntry] =
  {
    buildRequestAndWaitForResult(serviceName, fireServiceRequest)
  }

  private def fireServiceRequest(request: RequestAndRequestSenderInformation)
  {
    if(manager != null && !request.isFired)
    {
      manager ! ServiceRequest(request.getRequestName)
      request.fire()
    }
  }

  private def buildRequestAndWaitForResult(requestName: String, fireTheRequest: (RequestAndRequestSenderInformation => Unit)): List[AddressBookEntry] =
  {
    val request = RequestAndRequestSenderInformation.createRequestIfNecessary(namingRequests, requestName)
    fireTheRequest(request)

    log.debug("Request for '%s' was builded and fired, waiting for response".format(requestName))

    request.synchronized
    {
      request.wait(cfgManager.getIntProperty("de.hsaugsburg.smas.nodeconfig.syncOperationTimeout"))
    }

    val response: List[AddressBookEntry] = request.getRequestResult
    var result = ListBuffer[AddressBookEntry]()

    if(response != null && response.size > 0)
    {
      log.debug("Response for '%s' was successful".format(requestName))
      result ++= response
    }
    else
    {
      log.debug("Response for '%s' was NOT successful".format(requestName))
    }

    result.toList
  }

  def handleRequestResponse(responseName: String, responseLocations: List[AddressBookEntry])
  {
    log.debug("Response will be handled: %s @ %s".format(responseName, responseLocations))

    namingRequests.remove(responseName) match
    {
      case Some(request) =>
      {
        request.synchronized
        {
          request.setRequestResult(responseLocations)
          request.notifyAll()
        }
      }

      case None => log.debug("Not demanded response was ignored due to not existing request: %s".format(responseName))
    }
  }

  def start()
  {
    log.debug("%s of type %s will be started right now!".format(getUniqueName, this.getClass.getName))

    asynchronousJobs.submit(new Runnable
    {
      def run() {pluginManager.startAllPlugins}
    })

    asynchronousJobs.submit(new Runnable
    {
      def run() {onStart()}
    })

  }

  def stop()
  {
    onStop()

    asynchronousJobs.shutdown()
    pluginManager.stopAllPlugins
  }

  def onStart()
  def onStop()

  private trait MessageHandler extends Runnable
  {
    def handleMessage(message: BaseMessage)
    {
      if(!validateMessage(message))
      {
        log.warn("Received an invalid message: %s".format(message))
        return
      }

      var possiblePluginMethods: List[MethodWrapper] = null
      if(message.receiver.isInternal)
      {
        possiblePluginMethods = pluginManager.findMethodsForMessage(message, message.receiver.getId)
      }
      else
      {
        possiblePluginMethods = pluginManager.findMethodsForMessage(message)
      }

      for (method <- possiblePluginMethods)
      {
        if(message.isInstanceOf[ServiceManagementMessage])
        {
          log.debug("Method for handling management message: %s".format(method.toString))
          asynchronousManagementJobs.submit(method)
        }
        else
        {
          log.debug("Method for handling message: %s".format(method.toString))
          asynchronousJobs.submit(method)
        }
      }
    }
  }

  private class BaseMessageHandler(message: BaseMessage) extends MessageHandler
  {
    def run()
    {
      handleMessage(message)
    }
  }

  private class SignedMessageHandler(signedMessage: SignedMessage) extends MessageHandler
  {
    override def run()
    {
      handleSignedMessage()
    }

    def handleSignedMessage()
    {
      log.debug("Signed message received, checking...")
      if(validateMessage(signedMessage) &&
        checkIfSenderIsKnownAndTrusted(signedMessage.sender, signedMessage.sender.getPublicKey))
      {
        log.debug("message is valid")
        val senderPublicKey = signedMessage.sender.getPublicKey
        val signedBytes = SignedBytes(ByteObjectHelper.getBytesOfAnyRef(signedMessage.wrappedMessage), signedMessage.sign)

        if(CryptoEngine.verifyBytes(signedBytes, senderPublicKey) && signedMessage.wrappedMessage.sender == signedMessage.sender)
        {
          handleMessage(signedMessage.wrappedMessage)
        }
      }
      else
      {
        log.debug("message was discarded as not being valid")
      }
    }
  }
}

