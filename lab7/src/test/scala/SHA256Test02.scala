import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

object SHA256Helper {
  def padMessage(message: Seq[Byte]): BigInt = {
    val messageBytes = message.toArray
    val messageLenBits = messageBytes.length * 8L
    val paddedHex = new StringBuilder
    messageBytes.foreach(b => paddedHex.append(f"${b & 0xFF}%02x"))
    paddedHex.append("80")
    val currentLenBytes = paddedHex.length / 2
    val zerosNeeded = 56 - currentLenBytes
    paddedHex.append("00" * zerosNeeded)
    paddedHex.append(f"${messageLenBits}%016x")
    BigInt(paddedHex.toString, 16)
  }

  def padMessageString(message: String): BigInt = {
    padMessage(message.getBytes("UTF-8"))
  }
}

// We have to make sure our utility also works :O
class SHA256Test02 extends AnyFlatSpec with ChiselScalatestTester {
  "SHA256" should "hash 'abc' correctly" in {
    test(new SHA256) { dut =>
      val message = SHA256Helper.padMessageString("abc")

      val expectedMessage = BigInt(
        "6162638000000000000000000000000000000000000000000000000000000000" +
          "0000000000000000000000000000000000000000000000000000000000000018", 16)

      assert(message == expectedMessage, "Padded message does not match expected value")


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