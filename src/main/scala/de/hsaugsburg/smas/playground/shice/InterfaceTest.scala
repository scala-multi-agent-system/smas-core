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

import java.lang.reflect.Method


object InterfaceTest
{
  def main(args: Array[String])
  {
    val instance: Interface = new Implementation
    var test = new TestMethodWrapper(null, null)

    println(instance.getClass.toString)
    for(method <- instance.getClass.getMethods)
    {
      println(method.getName)
      if(method.getName == "hello")
      {
        test = new TestMethodWrapper(instance, method)
      }
    }

    test.handle("Rico")
    println(this.getClass.getName)
  }
}

trait Interface
{
  def hello(name: String)
}

class Implementation extends Interface
{
  def hello(name: String) = { println("hello "+ name) }
  def world = { println("world") }
}

case class TestMethodWrapper(obj: AnyRef, method: Method)
{
  def handle(msg: AnyRef) =
  {
    method.invoke(obj, msg)
    val stackTraceElements = Thread.currentThread().getStackTrace
    //println(stackTraceElements(1).getClassName)
  }
}