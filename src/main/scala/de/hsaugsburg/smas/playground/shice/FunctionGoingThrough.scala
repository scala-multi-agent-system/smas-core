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

package de.hsaugsburg.smas.playground.shice

import collection.mutable.HashMap


case class BasicMessage()
case class MessageOne(text: String) extends BasicMessage
case class MessageTwo() extends BasicMessage

object FunctionGoingThrough
{
	private val handlers = new HashMap[String, (Any=>Unit)]()

	def main(args: Array[String])
	{
		val f1 = handleOne(_)
		val f2: (Any=>Unit) = f1.asInstanceOf[(Any=>Unit)]
		val f3 = f1.asInstanceOf[(MessageOne=>Unit)]
		//f3(MessageOne("world"))
		//f2(MessageOne("wupp"))

		registerHandler(classOf[MessageOne], handleOne)

		for(item <- handlers.keys)
		{
			println(item)
		}

		handle(new MessageOne(" ffbuuudu"))
	}

	private def registerHandler[T](c: Class[T], f: (T=>Unit))
	{
		handlers.put(c +"", f.asInstanceOf[Any=>Unit])
	}

	private def handle(msg: AnyRef)
	{
		val method = handlers.get(msg.getClass+"") match
		{
			case Some(item) => item
			case None => null
		}
		println(method)
		method(msg)
	}

	private def handleOne(msg: MessageOne)
	{
		println("hello" + msg.text)
	}

	private def handleTwo(msg: MessageTwo)
	{

	}



}