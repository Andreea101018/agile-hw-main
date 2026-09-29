# Scala

## Simple Exercises with REPL

Use the Scala REPL (Read-Eval-Print Loop) to experiment with the following exercises.
REPL is available as `scala`, `scala-cli`, `sbt console`, or online in [scastie](https://scastie.scala-lang.org/).

### Values and Variables

* Create an immutable value `pi` with value 3.14159.
* Create a mutable variable `counter` initialized to 0. Increment it by 1.
* Try reassigning `pi` — what happens? Why?

### Functions and Conditionals

* Write a function `max(a: Int, b: Int): Int` that returns the larger of two integers using an if expression.
* Write a recursive function `fib(n: Int): Int` that computes the n-th Fibonacci number. Test it with `fib(5)` (should be 5).

### Collections

Create a list:
```scala
val numbers = List(1, 2, 3, 4, 5)
```
* Print the head and tail of the list.
* Append the number 7 at the end.

### Classes

* Define a class `Person` with the following properties:
  - `name: String`
  - `age: Int`

* Add a method `greet` that returns a greeting message.
* Write an `App` to create a `Person` instance and call `greet`.

## Simple Functional Programming Exercises in Scala

These examples plus tests are available in `Exercises.scala` and `ExercisesTest.scala`.

1. **Double Elements**
	- Write a function that takes a list of integers and returns a new list with each element doubled.
  Hint: Define the function and then use the `map` function.

2. **Filter Odd Numbers**
	- Implement a function that filters out all odd numbers from a list of integers.
  Hint: Use the `filter` function.

3. **Higher-Order Function**
	- Implement a higher-order function that takes a function and a list, and applies the function to each element of the list.
  Hint: Use the `map` function.

Run the examples with the provided test suite, with the following command:
```bash
sbt test 
```