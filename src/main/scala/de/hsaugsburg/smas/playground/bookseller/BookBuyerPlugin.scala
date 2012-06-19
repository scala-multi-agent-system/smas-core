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

package de.hsaugsburg.smas.playground.bookseller

import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.naming.AddressBookEntry
import de.hsaugsburg.smas.services.{RequestForService}
import java.util.{TimerTask, Timer}

class BookBuyerPlugin extends SmasPlugin
{
  var sellerCount = 0
  var bestBookPrice = 100.0
  var bestBookSeller: AddressBookEntry = null
  val executor = new Timer(true)

  def onStop = true

  def onStart = buyBookDelayed

  private def buyBookDelayed: Boolean =
  {
    executor.schedule(new TimerTask {
      def run()
      {
          val bookSeller = node ?? RequestForService("BookSellerService")
          sellerCount = bookSeller.size

          for(seller <- bookSeller)
          {
            node ! BookSearch("Programming in Scala", seller)
          }
      }
    }, 1000)
    true
  }

  def handleBookOffer(msg: BookOffer)
  {
    this.synchronized
    {
      log.info("Offer received... %s %s".format(msg.name, msg.price.toString))
      if(sellerCount > 0 && msg.price < bestBookPrice)
      {
        bestBookPrice = msg.price
        bestBookSeller = msg.sender
      }
      sellerCount -= 1

      log.debug("SellerCount: " + sellerCount)

      if(sellerCount == 0)
      {
        node ! BookOrder(msg.name, bestBookPrice, bestBookSeller)
      }
    }
  }

  def handleBookDelivery(msg: BookDelivery)
  {
    log.info("Book %s bought for %s".format(msg.book.name, msg.book.price.toString))
  }
}
