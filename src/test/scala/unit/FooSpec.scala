package unit

import java.util.LinkedList
import java.util.NoSuchElementException

import org.scalatest.FlatSpec

class FooSpec extends FlatSpec  {

  "A Stack" should "pop values in last-in-first-out order" in {
    val queue = new LinkedList[Int]
    queue.push(1)
    queue.push(2)
    assert(queue.pop() === 2)
    assert(queue.pop() === 1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyqueue = new LinkedList[String]
    assertThrows[NoSuchElementException] {
      emptyqueue.pop()
    }
  }

}
