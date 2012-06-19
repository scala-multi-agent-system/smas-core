/*
 * SMAS - Scala Multi Agent System
 * Copyright (C) 2012  Rico Lieback, Matthias Klass
 *
 * This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation; either version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with this program; if not, see <http://www.gnu.org/licenses/>.
 */

package de.hsaugsburg.smas.configuration

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import util.Random
import java.io.{File, FileWriter}
import de.hsaugsburg.smas.cryptographie.CryptoEngine
import org.scalatest.WordSpec
import org.scalatest.matchers.{MustMatchers, ShouldMatchers}
import scala.Predef._
import org.slf4j.LoggerFactory

@RunWith(classOf[JUnitRunner])
class ConfigurationManagerTest extends WordSpec with ShouldMatchers with MustMatchers
{
  val log = LoggerFactory.getLogger(this.getClass)

  val defaultConfigFilePath = this.getClass.getResource("/config").getPath

  val rand = new Random(System.currentTimeMillis())

  val config1 = CryptoEngine.calculateHashOfString(rand.nextString(25))
  val config2 = CryptoEngine.calculateHashOfString(rand.nextString(25))
  val config3 = rand.nextInt(1000)
  val config4 = CryptoEngine.calculateHashOfString(rand.nextString(25))

  val key1 = "1_" + CryptoEngine.calculateHashOfString(rand.nextString(25))
  val key2 = "2_" + CryptoEngine.calculateHashOfString(rand.nextString(25))
  val key3 = "3_" + CryptoEngine.calculateHashOfString(rand.nextString(25))
  val key4 = "4_" + CryptoEngine.calculateHashOfString(rand.nextString(25))

  "A configuration manager" should
    {
      "find and read all configuration files" in
        {
          generateFilesAndFolders()

          val config = ConfigurationManager()
          config.loadPropertiesFromFileSystem()

          config.getProperty(key1) must be === config1
          config.getProperty(key2) must be === config2
          config.getIntProperty(key3) must be === config3
          config.getProperty(key4) must be === config4
        }

      "find and read all configuration files even over several instances" in
        {
          val config = ConfigurationManager()
          config.loadPropertiesFromFileSystem()

          config.getProperty(key4) equals config4
          evaluating { config.getIntProperty(key1) } should produce [IllegalStateException]
        }
    }

  private def generateFilesAndFolders()
  {
    log.info("generating folders in " + defaultConfigFilePath)

    createFolders(defaultConfigFilePath + "/test/")
    createFileAndWriteData(defaultConfigFilePath + "/test/config.cfg", key1 + "=" + config1)
    createFileAndWriteData(defaultConfigFilePath + "/test/config2.cfg", key2 + "=" + config2)
    createFolders(defaultConfigFilePath + "/test/sub/");
    createFileAndWriteData(defaultConfigFilePath + "/test/sub/config.cfg", key3 + "=" + config3)
    createFolders(defaultConfigFilePath + "/test/sub/sub2/");
    createFileAndWriteData(defaultConfigFilePath + "/test/sub/sub2/config.cfg", key4 + "=" + config4)
  }

  private def createFolders(foldersPath: String)
  {
    val folders = new File(foldersPath)
    folders.mkdirs()
  }

  private def createFileAndWriteData(filePath: String, data: String)
  {
    val file = new File(filePath)
    file.createNewFile()
    val outputFile = new FileWriter(file)
    outputFile.write(data)
    outputFile.flush()
    outputFile.close()
  }
}