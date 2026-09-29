import org.scalatest.funsuite.AnyFunSuite

class ExercisesTest extends AnyFunSuite {
  import Exercises._

  test("doubleElements doubles each element") {
    assert(doubleElements(List(1, 2, 3)) == List(2, 4, 6))
    assert(doubleElements(Nil) == Nil)
  }

  test("filterOddNumbers removes odd numbers") {
    assert(filterOddNumbers(List(1, 2, 3, 4, 5)) == List(2, 4))
    assert(filterOddNumbers(List(1, 3, 5)) == Nil)
    assert(filterOddNumbers(List(2, 4, 6)) == List(2, 4, 6))
  }

  test("applyFunction applies a function to each element") {
    assert(applyFunction((x: Int) => x * x, List(1, 2, 3)) == List(1, 4, 9))
    assert(applyFunction((s: String) => s.toUpperCase, List("a", "b")) == List("A", "B"))
  }
}
