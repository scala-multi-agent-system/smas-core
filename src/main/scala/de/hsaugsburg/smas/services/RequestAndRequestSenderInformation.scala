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

import collection.JavaConversions.JConcurrentMapWrapper
import de.hsaugsburg.smas.naming.AddressBookEntry

class RequestAndRequestSenderInformation(service: String) extends Serializable
{
  private var result: List[AddressBookEntry] = null
  private val serviceName = service
  private var fired = false

  def setRequestResult(address: List[AddressBookEntry]) {result = address}
  def getRequestResult: List[AddressBookEntry] = {result}
  def getRequestName: String = {serviceName}
  def isFired: Boolean = {fired}
  def fire() {fired = true}

}

object RequestAndRequestSenderInformation
{
  def createRequestIfNecessary(requests: JConcurrentMapWrapper[String, RequestAndRequestSenderInformation], serviceName: String): RequestAndRequestSenderInformation =
  {
    var request = new RequestAndRequestSenderInformation(serviceName)

    requests.putIfAbsent(serviceName, request) match
    {
      case Some(item) => request = item
      case None => null
    }

    request
  }
}