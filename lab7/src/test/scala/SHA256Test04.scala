import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import java.security.MessageDigest

// A "trait", could also be considered an interface for our implementations
trait SHA256Impl {
  def sha256(message: String): String
}

// Software model using Java's MessageDigest
class SHA256Model extends SHA256Impl {
  def sha256(message: String): String = {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(message.getBytes("UTF-8"))
    hashBytes.map("%02x".format(_)).mkString
  }
}

// Chisel implementation wrapper
class SHA256Chisel(dut: SHA256) extends SHA256Impl {
  def sha256(message: String): String = {
    val paddedMessage = SHA256Helper.padMessageString(message)
    dut.io.start.poke(true.B)
    dut.io.message.poke(paddedMessage.U)
    dut.clock.step()
    dut.io.start.poke(false.B)
    while (!dut.io.valid.peek().litToBoolean) {
      dut.clock.step()
    }
    val hashBigInt = dut.io.hash.peek().litValue
    dut.reset.poke(true.B)
    dut.clock.step()
    dut.reset.poke(false.B)
    f"$hashBigInt%064x"
  }
}

// Tester that compares multiple implementations, this one is simple and checks cross-equality
class SHA256SimpleTester(implementations: Seq[SHA256Impl], testMessages: Seq[String]) {
  def runTests(): Unit = {
    for (message <- testMessages) {
      val results = implementations.map(impl => impl.sha256(message))
      assert(results.distinct.size == 1, s"Mismatch for message '$message': ${results.mkString(", ")}")
    }
  }
}

// Abstracted tester
class SHA256Test04 extends AnyFlatSpec with ChiselScalatestTester {
  "SHA256" should "hash 'abc' correctly" in {
    test(new SHA256) { dut =>
      val model = new SHA256Model()
      val chiselImpl = new SHA256Chisel(dut)

      val testMessages = Seq(
        "abc",
        "hello world",
        "",
        "The quick brown fox jumps over the lazy dog",
        "The quick brown fox jumps over the lazy dog."
      )

      val tester = new SHA256SimpleTester(Seq(model, chiselImpl), testMessages)
      tester.runTests()
    }
  }
  "SHA256" should "hash random messages correctly" in {
    test(new SHA256) { dut =>
      val model = new SHA256Model()
      val chiselImpl = new SHA256Chisel(dut)

      val random = new scala.util.Random(0)
      val testMessages = (1 to 20).map { _ =>
        val len = random.nextInt(25)
        random.alphanumeric.take(len).mkString
      }

      val tester = new SHA256SimpleTester(Seq(model, chiselImpl), testMessages)
      tester.runTests()
    }
  }

  "SHA256" should "hash strings spanning multiple blocks correctly" in {
    test(new SHA256) { dut =>
      val model = new SHA256Model()
      val chiselImpl = new SHA256Chisel(dut)

      val longMessage = "a" * 128
      val testMessages = Seq(longMessage)

      val tester = new SHA256SimpleTester(Seq(model, chiselImpl), testMessages)
      tester.runTests()
    }
  }
}