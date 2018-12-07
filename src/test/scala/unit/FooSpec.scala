package unit

import collection.mutable.ArrayStack
import org.scalatest._

class FooSpec extends FlatSpec with Matchers  {

  "A Stack" should "pop values in last-in-first-out order" in {
    val stack = new ArrayStack[Int]
    stack.push(1)
    stack.push(2)
    assert(stack.pop() === 2)
    assert(stack.pop() === 1)
  }

/*  it should "throw NoSuchElementException if an empty stack is popped" in {
    val emptyStack = new ArrayStack[String]
    assertThrows[NoSuchElementException] {
      emptyStack.pop()
    }
  }*/

}
