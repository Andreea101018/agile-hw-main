import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec


// Main test class
class SimpleTest extends AnyFlatSpec with ChiselScalatestTester {

  behavior of "Simple"

  it should "add 1" in {
    test(new Simple).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // Test case 1: input 0 should give output 1
      dut.io.in.poke(0.U)
      dut.io.out.expect(1.U)
      dut.clock.step()

      // Test case 2: input 10 should give output 11
      dut.io.in.poke(10.U)
      dut.io.out.expect(11.U)
      dut.clock.step()

      // Test case 3: input 31 should give output 0 (wrap around)
      dut.io.in.poke(31.U)
      dut.io.out.expect(0.U)
      dut.clock.step()
    }
  }
}