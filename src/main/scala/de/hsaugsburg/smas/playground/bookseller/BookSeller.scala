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

import de.hsaugsburg.smas.node.SmasNode
import de.hsaugsburg.smas.services.messages.{UnRegisterService, RegisterService}

class BookSeller extends SmasNode
{
  def onStart()
  {
    // register the book seller service on startup
    manager ! RegisterService("BookSellerService", me)
  }

  def onStop()
  {
    // unregister the book seller service on stop
    manager ! UnRegisterService("BookSellerService", me)
  }
}
