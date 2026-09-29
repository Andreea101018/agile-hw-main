import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

import java.security.MessageDigest


object SHA256Runner {
  def runTest(dut: SHA256, message: String): Unit = {
    val paddedMessage = SHA256Helper.padMessageString(message)
    val expectedHash = BigInt(1, MessageDigest.getInstance("SHA-256").digest(message.getBytes("UTF-8")))
    dut.io.start.poke(true.B)
    dut.io.message.poke(paddedMessage.U)
    dut.clock.step()
    dut.io.start.poke(false.B)
    while (!dut.io.valid.peek().litToBoolean) {
      dut.clock.step()
    }
    dut.io.hash.expect(expectedHash.U)
  }
}
// Another layer of abstraction hmm

class SHA256Test03 extends AnyFlatSpec with ChiselScalatestTester {
  "SHA256" should "hash 'abc' correctly" in {
    test(new SHA256) { dut =>
      SHA256Runner.runTest(dut, "abc")
    }
  }
}