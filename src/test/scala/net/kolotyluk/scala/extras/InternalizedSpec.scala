package net.kolotyluk.scala.extras

import java.util.UUID

import org.scalatest.WordSpec

class InternalizedSpec extends WordSpec {

  "Internalized.apply(object: Any)" can {
    "behave like String.intern" should {
      "given multiple different but equal objects, only return a single object" in {
        val id1 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
        val id2 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
        val id3 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
        assert(id1 == id2)
        assert(! (id1 eq id2))
        assert(id1 == id3)
        assert(! (id1 eq id3))
        assert(id2 == id3)
        assert(! (id2 eq id3))
        assert(Internalized(id1) eq Internalized(id1))
        assert(Internalized(id2) eq Internalized(id3))
      }
    }
  }

}
