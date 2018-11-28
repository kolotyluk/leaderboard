package unit

import java.util.LinkedList
import java.util.NoSuchElementException

import org.scalatest.FlatSpec

class FooSpec extends FlatSpec  {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new LinkedList[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }

  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new LinkedList[String]
    assertThrows[NoSuchElementException] {
      emptyStack.pop()
    }
  }

}
