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

object AddressBookEntryFactory
{
  def getAddressBookEntry(id: String, host: String,
                          port: Int,
                          internal: Boolean = false,
                          publicKey: PublicKey = null): AddressBookEntry =
  {
    new SmasAddressBookEntry(id, host, port, internal, publicKey)
  }

  def getAddressBookEntry(entry: AddressBookEntry): AddressBookEntry =
  {
    new SmasAddressBookEntry(entry.getId, entry.getHost, entry.getPort, entry.isInternal, entry.getPublicKey)
  }
}
