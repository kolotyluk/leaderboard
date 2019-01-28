import java.nio.ByteBuffer
import java.util.{Base64, UUID}

def uuidToUrlId(uuid: UUID): String =
  Base64
    .getUrlEncoder
    .withoutPadding
    .encodeToString(ByteBuffer.allocate(16)
      .putLong(uuid.getMostSignificantBits())
      .putLong(uuid.getLeastSignificantBits())
      .array())

def UrlIdToUuid(id: String): UUID = {
  val byteBuffer = ByteBuffer.wrap(Base64.getUrlDecoder.decode(id))
  val high = byteBuffer.getLong
  val low = byteBuffer.getLong
  new UUID(high,low)
}
  //UUID.nameUUIDFromBytes(Base64.getUrlDecoder.decode(id))

//val uuid = UUID.fromString("0d0cfdd9-0c3f-4a10-81f9-33177fec2201")
val uuid = UUID.randomUUID()

val id = uuidToUrlId(uuid)

ByteBuffer.allocate(16)
  .putLong(uuid.getMostSignificantBits())
  .putLong(uuid.getLeastSignificantBits())
  .array()

Base64.getUrlDecoder.decode(id)

UrlIdToUuid(id)


UrlIdToUuid("bogus")

UrlIdToUuid("?uWnCrtETqWtSC8rEYIiUA")

