import java.util.UUID

import scala.collection.concurrent.TrieMap

class Internalized(val value: Any) {
  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]
}

object Internalized {
  val manifest = new TrieMap[Any,Internalized]
  def apply[T](value: T): Internalized = manifest.getOrElseUpdate(value,new Internalized(value))
}

val id1 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
val id2 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
val id3 = Internalized(id1)
val id4 = Internalized(id2)

id1 == id2
id1 eq id2

id3 == id4
id3 eq id4

id3.value
id4.value

id3.hashCode()
id4.hashCode()

id3 eq null
id3 == null
id3.eq(null)
id3.equals(null)

val foo = "91e02865-0102-4b09-b487-bbfacb0ff759"
println(foo.length)

//def urlIdToUuid(base64UrlId: String): UUID = {
//  try {
//    val byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder.decode(base64UrlId))
//    val high = byteBuffer.getLong
//    val low = byteBuffer.getLong
//    new UUID(high,low)
//  } catch {
//    case cause: IllegalArgumentException =>
//      println(s"cause = $cause")
//      if (base64UrlId.length != 22) {
//        val message = s"base64UrlId = $base64UrlId does not contain exactly 22 characters"
//        println(message)
//        throw new InvalidBase64UrlToUuidException(message, cause)
//      } else {
//        val message = s"base64UrlId = $base64UrlId contains invalid base 64 URL characters"
//        println(message)
//        throw new InvalidBase64UrlToUuidException(message, cause)
//      }
//  }
//}
//
//val uuid1 = urlIdToUuid("keAoZQECSwm0h7v6yw_3WQ")
//
//try {
//  urlIdToUuid("keAoZQECSwm0h7v6yw_3W@")
//} catch {
//  case cause: Throwable =>
//    println(cause.getMessage)
//}


//id1 == id3
//id1 eq id3
//
//val id4 = BigInt(1)
//val id5 = BigInt(0) + BigInt(1)
//// val id6 = Internalize(id4)
//
//id4 == id5
//id4 eq id5
//
////id4 == id6
////id4 eq id6
//
//val id7 = new Internalized(id1)
//val id8 = new Internalized(id2)
//val id9 = new Internalized(id3)
//
//id7.equals(id8)
//id7.equals(id9)