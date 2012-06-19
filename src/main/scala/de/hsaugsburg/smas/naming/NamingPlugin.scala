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

import de.hsaugsburg.smas.plugin.base.SmasPlugin
import de.hsaugsburg.smas.services.messages.{MemberResponse, ServiceResponse}
import de.hsaugsburg.smas.services.{IsAliveResponse, IsAliveRequest}


class NamingPlugin extends SmasPlugin
{
  def onStop = true

  def onStart = true

  def handleServiceResponse(msg: ServiceResponse)
  {
    log.debug("%s on %s".format(msg.service, msg.serviceLocations))
    node.handleRequestResponse(msg.service, msg.serviceLocations)
  }

  def handleMemberResponse(msg: MemberResponse)
  {
    log.debug("%s on %s".format(msg.memberName, msg.memberLocation))
    node.handleRequestResponse(msg.memberName, List(msg.memberLocation))
  }

  def handleIsAliveRequest(msg: IsAliveRequest)
  {
    msg.sender ! IsAliveResponse(msg.name)
  }
}