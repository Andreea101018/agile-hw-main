import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

class MyQueueTest extends AnyFlatSpec with ChiselScalatestTester {

  "MyQueue" should "work" in {
    test(new MyQueue(4, UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>

      // write something into the queue
      dut.io.enq.valid.poke(true.B)
      dut.io.enq.bits.poke(5.U)
      dut.clock.step()
      dut.io.enq.valid.poke(false.B)
      var found = false
      for (i <- 0 until 10) {
        if (dut.io.deq.valid.peekBoolean()) {
          dut.io.deq.bits.expect(5.U)
          found = true
        }
        dut.clock.step()
      }
      assert(found, "Did not find valid output in MyQueue")
    }
  }

  "MyQueue" should "have a latency of 4" in {
    test(new MyQueue(4, UInt(8.W))).withAnnotations(Seq(WriteVcdAnnotation)) { dut =>
      // ???
    }
  }
}