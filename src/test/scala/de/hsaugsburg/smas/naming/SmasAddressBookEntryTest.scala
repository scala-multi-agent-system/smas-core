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


import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import de.hsaugsburg.smas.cryptographie.CryptoEngine

@RunWith(classOf[JUnitRunner])
class SmasAddressBookEntryTest extends WordSpec with ShouldMatchers with MustMatchers
{
  "A AddressBookEntry" should
    {
      "become complete if all data ist set" in
        {
          var address = SmasAddressBookEntry("id", "host", 1337)
          address.isComplete must be === true
          
          address = SmasAddressBookEntry("id", null, 1337)
          address.isComplete must be === false

          address = SmasAddressBookEntry(null, "host", 1337)
          address.isComplete must be === false

          address = SmasAddressBookEntry("id", "host", 0)
          address.isComplete must be === false
        }

      "be internal if set" in
      {
        var address = SmasAddressBookEntry("id", "host", 1337)
        address.isInternal must be === false

        address = SmasAddressBookEntry("id", "host", 1337, false)
        address.isInternal must be === false

        address = SmasAddressBookEntry("id", "host", 1337, true)
        address.isInternal must be === true
      }

      "be identical" in
        {
          var address = SmasAddressBookEntry("id", "host", 1337)
          var address2 = SmasAddressBookEntry("id", "host", 1337)
          
          address must be === address2

          address = SmasAddressBookEntry("id", "host", 1337)
          address2 = SmasAddressBookEntry("id2", "host", 1337)

          address == address2 must be === false


          address = SmasAddressBookEntry("id", "host", 1337)
          address2 = SmasAddressBookEntry("id", "host2", 1337)

          address == address2 must be === false


          address = SmasAddressBookEntry("id", "host", 1337)
          address2 = SmasAddressBookEntry("id", "host", 1338)

          address == address2 must be === false


          address = SmasAddressBookEntry("id", "host", 1337)
          address2 = SmasAddressBookEntry("id", "host", 1337, true)

          address == address2 must be === false
          
          val keys = CryptoEngine.generateAsyncKeyPair(512)
          val keys2 = CryptoEngine.generateAsyncKeyPair(512)
          
          address = SmasAddressBookEntry("id", "host", 1337, true, keys.getPublic)
          address2 = SmasAddressBookEntry("id", "host", 1337, true, keys.getPublic)
          
          address == address2 must be === true

          address = SmasAddressBookEntry("id", "host", 1337, true, keys.getPublic)
          address2 = SmasAddressBookEntry("id", "host", 1337, true, keys2.getPublic)
          
          address == address2 must be === false
        }
    }

}