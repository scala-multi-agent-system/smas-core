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

import util.Random
import org.apache.commons.codec.binary.Hex
import java.security._


object CryptoEngine extends Serializable
{

  private val random = new Random()

  private val hashAlgorithm = "SHA-256"
  private val asyncAlgorithm = "RSA"
  private val signAlgorithm = "SHA256withRSA"

  def generateUniqueName: String =
  {
    val randomStringSequence = generateRandomStringSequence
    val hashBytes = calculateHashFromBytes((randomStringSequence + getRandomNumber (1, 50000)).getBytes)

    convertByteArrayToHexString(hashBytes)
  }

  def calculateHashOfString(toHash: String): String =
  {
    convertByteArrayToHexString(calculateHashFromBytes(toHash.getBytes))
  }

  def calculateHashFromBytes(toHash: Array[Byte]): Array[Byte] =
  {
    var hashCreator: MessageDigest = null

    try
    {
      hashCreator = MessageDigest.getInstance(hashAlgorithm)
    }
    catch
    {
      case e: NoSuchAlgorithmException => throw new IllegalStateException("We need some basic crypto algorithms for " +
        "this software, please check your JVM installation")
    }

    hashCreator.update(toHash)

    hashCreator.digest()

  }

  def generateAsyncKeyPair(keySize: Int): KeyPair =
  {
    var rsaKeyGenerator: KeyPairGenerator = null

    try
    {
      rsaKeyGenerator = KeyPairGenerator.getInstance(asyncAlgorithm)
    }
    catch
    {
      case e: NoSuchAlgorithmException => throw new IllegalStateException("We need some basic crypto algorithms for " +
        "this software, please check your JVM installation")
    }

    rsaKeyGenerator.initialize(keySize)
    val keyPair = rsaKeyGenerator.genKeyPair()

    keyPair
  }

  def signBytes(bytes: Array[Byte], privateKey: PrivateKey): SignedBytes =
  {
    var signature: Signature = null

    try
    {
      signature = Signature.getInstance(signAlgorithm)
    }
    catch
    {
      case e: NoSuchAlgorithmException => throw new IllegalStateException("We need some basic crypto algorithms for " +
        "this software, please check your JVM installation")
    }

    signature.initSign(privateKey)
    signature.update(bytes)
    val sign = signature.sign()

    SignedBytes(bytes, sign)
  }

  def verifyBytes(signedBytes: SignedBytes, publicKey: PublicKey): Boolean =
  {
    var signature: Signature = null

    try
    {
      signature = Signature.getInstance(signAlgorithm)
    }
    catch
    {
      case e: NoSuchAlgorithmException => throw new IllegalStateException("We need some basic crypto algorithms for " +
        "this software, please check your JVM installation")
    }


    signature.initVerify(publicKey)
    signature.update(signedBytes.bytes)

    signature.verify(signedBytes.sign)
  }

  private def generateRandomStringSequence: String =
  {
    random.nextString(getRandomNumber(1, 20))
  }

  private def getRandomNumber(start: Int, end: Int): Int =
  {
    var number = 0

    while(number < start || number > end)
    {
      number = random.nextInt(end+1)
    }

    number

  }

  private def convertByteArrayToHexString(toConvert: Array[Byte]): String =
  {
    new String(Hex.encodeHex(toConvert))
  }

}