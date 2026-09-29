# Lab 7 - Co-simulation

The types of tests we write in Chisel can be very manual, even after we have a clear idea of
what our component is supposed to do, have identified important properties and so-called "golden" cases, testing can be
tedious, verbose and leave a lot of margin for (human) error.

We can solve some of these human errors by leveraging automating correctness with co-simulated software models. Given an
software implementation we trust
we can take all the properties, golden tests and put them through both implementations, our source of truth (model) and
our actual hardware implementation with Chisel.

## Part 1: The basics ft. SHA256

I want to implement SHA256 with chisel, so I asked claude.ai to cook up an implementation, it was quite bug riddled
but after some manual fine adjustment it seems to work with my simple `abc` test case, good enough yes? Maybe.

SHA256 is pretty complex, even a naive implementation of the
algorithm ([src/main/scala/SHA256.scala](src/main/scala/SHA256.scala))
is hard to validate by even the most thorough code-review. We know SHA256 works pretty well, and the code *looks* like
it's correct
and even our abc test case passes perfectly fine!

1. See [src/test/scala/SHA256Test01.scala](src/test/scala/SHA256Test01.scala)

> Q: What is tedious or annoying with this test?

Ok - maybe hardcoding the message scheme and manually encoding the message length and magic bytes at the end is not a
very scalable
way to writing test cases, we can all agree with that.

So let's add some helper functions to generate the input at the very least to deal with that for us.

2. See [src/test/scala/SHA256Test02.scala](src/test/scala/SHA256Test02.scala)

> Q: Do we trust our abstraction?

We have a small problem here, now we have written even more code that might have issues so we need to also validate that
implementation but a simple regression check says it produces the same input bytes for `abc` so we should be good!

It is still a bit awkward having to manually manage the clock, we can wrap that into a really nice function as well.
We also hardcode the expected output, but doesn't java already implement SHA256 for us, why don't we just use
that?

3. See [src/test/scala/SHA256Test03.scala](src/test/scala/SHA256Test03.scala)

Actually `java.security.MessageDigest`'s implementation of SHA256 is actually our **model**

4. See [src/test/scala/SHA256Test04.scala](src/test/scala/SHA256Test04.scala)

> Q: Can we consider this model our source of truth, why or why not?

## Part 2: Writing a model based on a component

You are provided with an existing ALU (Arithmetic Logic Unit) hardware implementation and need to write a software model
for it.

1. Study the existing ALU hardware in [src/main/scala/ALU.scala](src/main/scala/ALU.scala)
   > Q: What operations does the ALU support? List them all.
2. Implement the `ALUModel` and dut driver `ALUChisel` in [src/test/scala/ALUTest.scala](src/test/scala/ALUTest.scala)
3. Enable co-simulation testing:
    - In each test, uncomment the line: `// val model = new ALUModel()`
    - Change `Seq(chiselImpl)` to `Seq(model, chiselImpl)` in the tester constructor
    - Now both implementations will be tested and compared automatically
4. Run the tests: `sbt "testOnly ALUTest"`
5. Extra: Add edge case tests (Division by zero/Overflow scenarios/Maximum/minimum values)
   > Q: Here the model and hardware might differ a lot, how so?

## Part 3: Writing a component based on a model

You are provided with a software model of a vending machine and a hardware template to implement.

1. Review the software model in [src/test/scala/VendingMachineTest.scala](src/test/scala/VendingMachineTest.scala)
2. First, verify the model works by running tests: `sbt "testOnly VendingMachineTest"`
3. Design and implement the Chisel hardware module
   in [src/main/scala/VendingMachine.scala](src/main/scala/VendingMachine.scala)
4. Implement the `VendingMachineChisel` wrapper
   in [src/test/scala/VendingMachineTest.scala](src/test/scala/VendingMachineTest.scala)
5. Enable co-simulation testing:
    - In each test, uncomment the line: `// val chiselImpl = new VendingMachineChisel(dut)`
    - Change `Seq(model)` to `Seq(model, chiselImpl)` in the tester constructor
    - Now both implementations will be tested and compared automatically
6. Run the tests again: `sbt "testOnly VendingMachineTest"`
7. Debug any mismatches between your hardware implementation and the software model
8. Extra: Add a new test case that you think might break either implementation

## Part 4: Modelling real code?

1. Find a chisel component on
   [GitHub](https://github.com/search?q=language%3AScala+%22import+chisel3._%22+path%3A%2F%5Esrc%5C%2Fmain%5C%2Fscala%5C%2F%2F&type=code)
   > Q: Is this component freely licensed?
2. Write a model for this component
3. Identity one issue either in the implementation or in your initial draft of the model

## Part 5: Freestyle

1. Write a specification (protocol, behavior, properties) for any component
2. Write a software model using the patterns you have used so far and add some simple golden test cases and compare it
   to your model
   > Q: Identify places where drift could occur between your model and implementation
3. Write some dynamic tests that test different edge cases
4. Extra: use randomness to find edge cases

## Further Concepts / Reading

- Code coverage
- Functional coverage
- Fuzzing (manually with randomness)
- ScalaCheck / property based testing
- BFM (Bus Functional Model)
