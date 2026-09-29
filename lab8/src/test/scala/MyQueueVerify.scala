import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec


// Main test class
class MyQueueVerify extends AnyFlatSpec with ChiselScalatestTester with Formal {

  "Simple" should "be OK with verification" in {
    verify(new MyQueue(4, UInt(8.W)), Seq(BoundedCheck (5), WriteVcdAnnotation))
  }
}