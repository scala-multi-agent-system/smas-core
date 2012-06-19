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

package de.hsaugsburg.smas.cryptographie

import org.scalatest.WordSpec
import org.scalatest.matchers.ShouldMatchers
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import collection.mutable.ListBuffer


@RunWith(classOf[JUnitRunner])
class CryptoEngineTest extends WordSpec with ShouldMatchers
{
 	"A crypto engine" should
		 {
			 "generate a unique name" in {

         val names = new ListBuffer[String]()
         while(names.size < 5000)
         {
           val name = CryptoEngine.generateUniqueName
           if(names.contains(name))
           {
             fail()
           }
           names += name
         }
			 }
		 }
}