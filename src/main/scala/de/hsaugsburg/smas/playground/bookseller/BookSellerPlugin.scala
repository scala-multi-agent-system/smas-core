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
import de.hsaugsburg.smas.configuration.ConfigurationManager
import collection.mutable.ListBuffer
import de.hsaugsburg.smas.naming.AddressBookEntry

class BookSellerPlugin extends SmasPlugin
{
  private val books = ListBuffer[Book]()

  protected var bookProp = "books"

  def onStop = true

  def onStart = loadBooks

  private def loadBooks: Boolean =
  {
    val bookString = getConfig(bookProp)
    
    for(item <- bookString.split(";"))
    {
      books += Book(item.split(":")(0), item.split(":")(1).toDouble)
    }
    
    true
  }

  def handleBookSearch(msg: BookSearch)
  {
    for(book <- books)
    {
      if(book.name == msg.name)
      {
        node ! BookOffer(book.name, book.price, msg.sender)
        log.info("Book found, offer sent!")
        return
      }
    }

    node ! BookNotFound(msg.name, msg.sender)
    log.info("Book not found!")
  }
  
  def handleBookOrder(msg: BookOrder)
  {
    val book = Book(msg.name, msg.price)
    if(books.contains(book))
    {
      val newBooks = books diff List(book)
      books.clear()
      books ++= newBooks

      node ! BookDelivery(book,  msg.sender)
    }
  }
  
}

class BookSellerOnePlugin extends BookSellerPlugin
{
  this.bookProp = "booksOne"
}

class BookSellerTwoPlugin extends BookSellerPlugin
{
  this.bookProp = "booksTwo"
}


