# Lab 8: Formal Verification

This lab introduces formal verification techniques for hardware designs using Chisel Formal.
You will learn how to specify properties of your hardware and verify them using formal methods.

To use Chisel formal you need to install the open [Z3 theorem prover](https://github.com/Z3Prover/z3). Use your package manager to install, e.g. on Ubuntu:

```bash
sudo apt install z3
```
On MacOS you can use Homebrew:

```bash
brew install z3
```

## Hello World

We start with a minimal example in `Simple.scala`, which contains two assertions.
One is correct the other one is incorrect. The test given in `SimpleTest` will pass, although one assertion is wrong.

```
sbt "testOnly SimpleTest"
```

However, the formal verification (`SimpleVerify`) will catch the error.

```
sbt "testOnly SimpleVerify"
```

Play with this example: run the tests and explore the VCD file.
Does the formal verification show a counter example in the VCD file?

Fix the assertion and check that the formal test passes.


## A Simple Counter

Next we do a simple example of a 2-bit counter. Create a new Chisel module `Counter2Bit` in a file named `Counter2Bit.scala`:

```scala
import chisel3._
import chisel3.util._
import chiseltest.formal._

class Counter2Bit extends Module {
  val io = IO(new Bundle {
    val out = Output(UInt(2.W))
  })
  val count = RegInit(0.U(2.W))
  count := count + 1.U
  io.out := count

  // Formal properties
  assert(count <= 3.U, "Counter should not exceed 3")
  cover(count === 0.U, "Counter reaches 0")
}
```
and write test verification in `Counter2BitVerify.scala`.

Change the properties and see how the formal verification reacts.
What else can you verify?

Can you check that the counter increases by one each cycle?

`past(x)` provides the value of `x` in the previous cycle.
Conditional properties can be expressed using `when`, `elsewhen`, and `otherwise`.

How large must the bound be to verify the counter?
Make the check fail by changing the properties and explore when the failure is not covered with a too small bound.

Explore other available functions in https://www.javadoc.io/static/edu.berkeley.cs/chiseltest_2.12/0.5.5/chiseltest/formal/index.html

## Test a Queue

The source `MyQueue.scala` has initial steps to implement a queue, e.g.,
the class definition and IO definition and a single buffer component.
Use that buffer to implement a bubble queue (e.g., an element is moved forward
when the next position is empty) of depth given as parameter.

`MyQueueTest.scala` implements a simple test for the queue.
Use that test to get your implementation started.
Add a test for the latency of the queue (the element needs to bubble
from the input to the output).

Then write formal verification in `MyQueueVerify.scala`.

Write a test plan on what should be checked formally.
Examples are:

- An element that is enqueued will eventually be dequeued.
- The order of elements is preserved (FIFO).
- Full and empty conditions are correctly handled.

Also check the properties that have been shown on slide 7 of
the [testing lecture](../06_testing_and_ci.pdf). Note, that some properties need to be adapted.
E.g., you can see that a queue is not empty by checking that the output is valid (after some cycles).

## Use Chisel Formal on your own Design!

Find at least one feature that you can formally verify on your own Chisel design.
