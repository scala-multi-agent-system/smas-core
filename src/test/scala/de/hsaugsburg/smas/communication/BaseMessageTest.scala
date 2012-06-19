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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.WordSpec
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import de.hsaugsburg.smas.naming.{AddressBookEntryFactory, AddressBookEntry}


@RunWith(classOf[JUnitRunner])
class BaseMessageTest extends WordSpec with ShouldMatchers with MustMatchers
{
  "Message extended from BaseMessage" should
    {
      "be equal" in
        {
          val abe = AddressBookEntryFactory.getAddressBookEntry("thisIsAId", "local", 1337)
          val abe2 = AddressBookEntryFactory.getAddressBookEntry("thisIsAId", "local", 1338)
          val referenceMessage = TestMessageOne("thisIsAString", 42, abe)
          
          var msg = TestMessageOne("thisIsAString", 42, abe)
          msg must be === referenceMessage
          
          msg = TestMessageOne("this3I1sASt2ri6ng", 42, abe)
          msg must not be(referenceMessage)

          msg = TestMessageOne("thisIsAString", 43, abe)
          msg must not be(referenceMessage)

          msg = TestMessageOne("thisIsAString", 42, abe2)
          msg must not be(referenceMessage)
        }

      "return the correct values" in
        {
          val abe = AddressBookEntryFactory.getAddressBookEntry("thisIsAId", "local", 1337)
          val abe2 = AddressBookEntryFactory.getAddressBookEntry("thisIsAId", "local", 1338)
          val referenceMessage = TestMessageOne("thisIsAString", 42, abe)

          referenceMessage.sender must be === null
          referenceMessage.receiver must be === abe
          referenceMessage.name must be === "thisIsAString"
          referenceMessage.version must be === 42


          referenceMessage.sender(abe2)

          referenceMessage.sender must be === abe2
          referenceMessage.receiver must be === abe
          referenceMessage.name must be === "thisIsAString"
          referenceMessage.version must be === 42


          referenceMessage.sender(abe)

          referenceMessage.sender must be === abe2
          referenceMessage.receiver must be === abe
          referenceMessage.name must be === "thisIsAString"
          referenceMessage.version must be === 42

        }
    }
  
  case class TestMessageOne(name: String, version: Int, private val rx: AddressBookEntry) extends BaseMessage(rx)
}