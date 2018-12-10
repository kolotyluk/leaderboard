package net.kolotyluk.scala.extras

import java.util.UUID
import java.nio.ByteBuffer

import org.apache.commons.codec.binary.Base64


object Identity {

  /** =UUID to URL Safe Base 64=
    *
    * @param uuid
    * @return
    *
    * @see [[https://commons.apache.org/proper/commons-codec/archives/1.11/apidocs/org/apache/commons/codec/binary/Base64.html#encodeBase64URLSafeString-byte:A- encodeBase64URLSafeString]]
    */
  def getUrlIdentifier(uuid: UUID = UUID.randomUUID()): String = {
    val uuidBytes = ByteBuffer.wrap(new Array[Byte](16))
    uuidBytes.putLong(uuid.getMostSignificantBits())
    uuidBytes.putLong(uuid.getLeastSignificantBits())
    Base64.encodeBase64URLSafe(uuidBytes.array()).toString
  }

  /** =URL Safe Base 64 to UUID=
    *
    * @param identifier
    * @return
    *
    * @see [[https://commons.apache.org/proper/commons-codec/archives/1.11/apidocs/org/apache/commons/codec/binary/Base64.html#decodeBase64-java.lang.String- decodeBase64]]
    */
  def getUrlIdentifier(identifier: String): UUID = {
    UUID.nameUUIDFromBytes(Base64.decodeBase64(identifier))
  }
}
