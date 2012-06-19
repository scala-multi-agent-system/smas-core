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

package de.hsaugsburg.smas.services

import org.scalatest.WordSpec
import org.scalatest.junit.JUnitRunner
import org.junit.runner.RunWith
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import collection.JavaConversions.JConcurrentMapWrapper
import java.util.concurrent.{TimeUnit, CyclicBarrier, Executors, ConcurrentHashMap}
import collection.mutable.ListBuffer


@RunWith(classOf[JUnitRunner])
class RequestAndRequestSenderInformationTest extends WordSpec with ShouldMatchers with MustMatchers
{
  private val threadCount = Runtime.getRuntime.availableProcessors() + 1

  "a RequestAndRequestSenderInformation" should
    {

      "handle requests thread save" in
        {
          val data = new JConcurrentMapWrapper[String, RequestAndRequestSenderInformation](
            new ConcurrentHashMap[String, RequestAndRequestSenderInformation]())

          val result = new ListBuffer[RequestAndRequestSenderInformation]()

          val pool = Executors.newFixedThreadPool(threadCount)
          val barrier = new CyclicBarrier(threadCount)

          var count = 0
          while(count < threadCount)
          {
            pool.submit(new Runnable {
              def run()
              {
                barrier.await()
                val requests = new ListBuffer[RequestAndRequestSenderInformation]()

                var workingCounter = 0
                while(workingCounter < 1000)
                {
                  requests += RequestAndRequestSenderInformation.createRequestIfNecessary(data, "TestServiceRequest")

                  workingCounter += 1
                }

                result.synchronized
                {
                  result ++= requests
                }
              }
            })


            count += 1
          }

          pool.shutdown()
          pool.awaitTermination(100, TimeUnit.DAYS)

          data.size must be === 1
          result.size must be === threadCount*1000
        }
    }
}