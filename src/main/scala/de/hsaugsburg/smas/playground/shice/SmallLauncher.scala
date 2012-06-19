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

import java.util.concurrent._


object SmallLauncher
{
  def main(args: Array[String])
  {
    val holonRequests = new ConcurrentHashMap[String, Request]()
    holonRequests.put("test", new Request())


    val searchTask = new Runnable
    {
      def run()
      {
        val request = holonRequests.get("test")

        request.synchronized
        {
          println("Start waiting...")
          request.wait()
          println("Stop waiting...")
        }
      }
    }

    val secondSearchTask = new Runnable
    {
      def run()
      {
        val request = holonRequests.get("test")

        request.synchronized
        {
          println("Start waiting 2...")
          request.wait()
          println("Stop waiting 2...")
        }
      }
    }

    val holonRequest = new Runnable
    {
      def run()
      {
        Thread.sleep(5000)

        val request = holonRequests.get("test")

        request.synchronized
        {
          println("Try to notify...")
          request.notifyAll()
          println("Notify all...")
        }
      }
    }


    val workQueue = Executors.newFixedThreadPool(4)

    workQueue.submit(secondSearchTask)
    workQueue.submit(searchTask)
    workQueue.submit(holonRequest)

    workQueue.shutdown()
  }

}

class Request
{
  private var result = ""

  def getResult: String = result

  def setResult(res: String)
  {
    result = res
  }
}
