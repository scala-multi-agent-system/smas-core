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

package de.hsaugsburg.smas.util

import collection.mutable.ListBuffer
import util.Random


object Randomizer
{

  private val rand = new Random()

  def listOfListOfRandomStrings(listCount: Int, itemCount: Int): List[List[String]] =
  {
    val result = new ListBuffer[List[String]]()

    var count = 0
    while(count < listCount)
    {
      result += listOfRandomStrings(itemCount)
      count += 1
    }

    result.toList
  }

  def listOfRandomStrings(itemCount: Int): List[String] =
  {
    var count = 0
    val result = new ListBuffer[String]()

    while(count < itemCount)
    {
      result += "Plugin_" + rand.nextInt(500000) + "_" + rand.nextInt(500000)
      count += 1
    }

    result.toList
  }
}