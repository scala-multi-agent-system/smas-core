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

import java.util.Properties
import java.io.{FileInputStream, File}
import collection.mutable.ListBuffer
import java.lang.IllegalStateException
import org.slf4j.LoggerFactory
import de.hsaugsburg.smas.util.FileUtil

object ConfigurationManager
{
  private val INSTANCE: ConfigurationManager = new ConfigurationManager()

  def apply(): ConfigurationManager =
  {
    INSTANCE
  }
}

class ConfigurationManager extends Serializable
{
  val log = LoggerFactory.getLogger(this.getClass)
  private val properties = new Properties()
  private val CONFIG_FOLDER = "/config"
  private val PROPERTY_FILE_EXTENSION = ".cfg"
  loadPropertiesFromFileSystem()

  def getProperty(key: String): String =
  {
    log.debug("Property was demanded: %s".format(key))
    val result = properties.getProperty(key, "")

    result
  }

  def getProperty(key: String, default: String): String =
  {
    val result = getProperty(key)

    if(result == null)
    {
      default
    }
    else
    {
      result
    }
  }

  def getIntProperty(key: String): Int =
  {
    log.debug("Integer property with key '%s' was demanded".format(key))
    getAndConvertProperty(key, _.toInt)
  }

  def getIntProperty(key: String, default: Int): Int =
  {
    log.debug("Integer property with key '%s' and default '%s' was demanded".format(key, default))
    getAndConvertProperty(key, default, _.toInt)
  }

  def getBoolProperty(key: String): Boolean =
  {
    log.debug("Boolean property with key '%s' was demanded".format(key))
    getAndConvertProperty(key, _.toBoolean)
  }

  def getBoolProperty(key: String, default: Boolean): Boolean =
  {
    log.debug("Boolean property with key '%s' and default '%s' was demanded".format(key, default))
    getAndConvertProperty(key, default, _.toBoolean)
  }

  private def getAndConvertProperty[T](key: String, defaultValue: T, convertFunction: (String) => T) : T =
  {
    try
    {
      getAndConvertProperty(key, convertFunction)
    }
    catch
      {
        case e: IllegalStateException => defaultValue
      }
  }

  private def getAndConvertProperty[T](key: String, convertFunction: (String) => T): T =
  {
    val propertyValue = getProperty(key)
    try
    {
      convertFunction(propertyValue)
    }
    catch
      {
        case nfe: NumberFormatException =>
        {
          log.warn(String.format("Property cannot be parsed. Key: %s, Value: %s", key, propertyValue))
          throw new IllegalStateException(String.format("Property cannot be parsed. Key: %s, Value: %s", key, propertyValue))
        }
      }
  }

  def loadPropertiesFromFileSystem()
  {
    log.info("loading properties from %s".format(CONFIG_FOLDER))
    val directory = FileUtil.getFile(CONFIG_FOLDER)

    log.debug("Properties will be loaded and prepared using path '%s'".format(directory.getPath))
    val files = getAllConfigurationFiles(directory)

    for(file <- files)
    {
      log.debug("loading properties from %s".format(file.getAbsolutePath))
      val props = new Properties()
      props.load(new FileInputStream(file))

      properties.putAll(props)
    }
  }

  private def getAllConfigurationFiles(file: File): ListBuffer[File] =
  {
    val filesAndFolders = ListBuffer[File]()
    for(item <- file.listFiles().toList)
      filesAndFolders += item

    if(filesAndFolders != null)
    {
      val foundFiles = filesAndFolders.filter(f => f.isDirectory).flatMap{f => getAllConfigurationFiles(f)}
      for(file <- foundFiles)
        filesAndFolders += file

      return filesAndFolders.filter(f => f.getName.endsWith(PROPERTY_FILE_EXTENSION))
    }

    ListBuffer[File]()
  }
}