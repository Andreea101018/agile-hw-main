import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// Basic test
class SHA256Test01 extends AnyFlatSpec with ChiselScalatestTester {
  "SHA256" should "hash 'abc' correctly" in {
    test(new SHA256) { dut =>
      // Message "abc" with SHA-256 padding to 512 bits:
      // "abc" = 0x616263
      // Padding: 0x80 followed by zeros, then length 0x18 (24 bits) in last 64 bits
      val message = BigInt(
        "6162638000000000000000000000000000000000000000000000000000000000" +
          "0000000000000000000000000000000000000000000000000000000000000018", 16)

      dut.io.start.poke(true.B)
      dut.io.message.poke(message.U)
      dut.clock.step()
      dut.io.start.poke(false.B)

      // Wait for completion
      while (!dut.io.valid.peek().litToBoolean) {
        dut.clock.step()
      }

      // Expected SHA-256 hash for "abc"
      val expected = BigInt("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad", 16)
      dut.io.hash.expect(expected.U)
    }
  }
}