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

package de.hsaugsburg.smas.communication

import de.hsaugsburg.smas.naming.AddressBookEntry

class BaseMessage(private var rx: AddressBookEntry = null, private var tx: AddressBookEntry = null) extends Serializable
{
  def receiver: AddressBookEntry = rx
  def sender: AddressBookEntry = tx

  def sender(senderAddress: AddressBookEntry)
  {
    if(this.tx == null || !this.tx.isComplete)
    {
      this.tx = senderAddress
    }
  }

  def receiver(receiverAddress: AddressBookEntry)
  {
    if(this.rx == null || !this.rx.isComplete)
    {
      this.rx = receiverAddress
    }
  }

  override def toString: String =
  {
    val result = this.getClass.getName

    result
  }

}