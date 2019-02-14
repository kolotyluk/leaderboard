package net.kolotyluk.scala.extras

import scala.collection.concurrent.TrieMap

/** =Class of Internalized Objects=
  *
  * Because there are no duplicates in this class, obj1 eq obj2 is sufficient
  *
  * Behaves similar to [[java.lang.String#intern]]
  *
  */
class Internalized[T](val value: T) {
  override def equals(that: Any): Boolean = this eq that.asInstanceOf[AnyRef]
  def getValue[T]: T = {
    //if (value.isInstanceOf[T])
      value.asInstanceOf[T]
    //else
    //  throw new Exception()
  }
}

/** =Maintain a Manifest of Objects=
  *
  * Behaves similar to [[java.lang.String#intern]]
  *
  * This code is thread-safe
  *
  * ==Examples==
  *
  * {{{
  * val id1 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
  * val id2 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
  * val id3 = UUID.fromString("02b9670c-afd4-4c39-b4d3-3c46ac4f1a9c")
  * assert(id1 == id2)
  * assert(! (id1 eq id2))
  * assert(id1 == id3)
  * assert(! (id1 eq id3))
  * assert(id2 == id3)
  * assert(! (id2 eq id3))
  * assert(Internalized(id1) eq Internalized(id1))
  * assert(Internalized(id2) eq Internalized(id3))
  * }}}
  *
  * ==Uses==
  *
  *
  * @see [[net.kolotyluk.scala.extras.InternalizedSpec]]
  */
object Internalized {
  val manifest = new TrieMap[Any,Internalized[Any]]
  def apply[T](value: T): Internalized[T] = manifest.getOrElseUpdate(value,new Internalized[Any](value)).asInstanceOf[Internalized[T]]
}