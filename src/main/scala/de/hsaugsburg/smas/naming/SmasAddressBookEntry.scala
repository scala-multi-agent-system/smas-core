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

package de.hsaugsburg.smas.naming

import java.security.PublicKey

//TODO [low] more common trait
case class SmasAddressBookEntry(private val id: String, private val host: String, private val port: Int,
                                private val internalUseOnly: Boolean = false, private val publicKey: PublicKey = null)
  extends AddressBookEntry
{
  private var state = false

  checkState()

  private def checkState()
  {
    if (id != null && host != null && port != 0)
      state = true
    else if (isInternal && id != null)
      state = true
  }

  def getId = id

  def getHost = host

  def getPort = port

  def getPublicKey = publicKey

  def isComplete: Boolean = state

  def isInternal: Boolean = internalUseOnly

  override def toString: String =
  {
    id + "@" + host + ":" + port
  }
}

