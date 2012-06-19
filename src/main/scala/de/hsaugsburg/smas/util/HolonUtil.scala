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

package de.hsaugsburg.smas.util

import de.hsaugsburg.smas.naming.AddressBookEntry
import xml.XML
import de.hsaugsburg.smas.communication.SmasCommunicationFactory
import collection.mutable.ListBuffer

object HolonUtil
{
  def getHolonAddressesFromXml(xmlFile: String): List[AddressBookEntry] =
  {
    val xml = XML.load(FileUtil.getFileInputStream(xmlFile))
    val addresses = new ListBuffer[AddressBookEntry]()

    for(holon <- xml \ "holons" \ "holon")
    {
      val host = (holon \ "host" text)
      val port = (holon \ "port" text).toInt
      addresses += SmasCommunicationFactory.getInstance.getInitialHolonService(host, port)
    }
    addresses.toList
  }
}
