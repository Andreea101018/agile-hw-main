import chisel3._
import chiseltest._
import chiseltest.formal._
import org.scalatest.flatspec.AnyFlatSpec


// Main test class
class SimpleVerify extends AnyFlatSpec with ChiselScalatestTester with Formal {

  "Simple" should "be OK with verification" in {
    verify(new Simple, Seq(BoundedCheck (5), WriteVcdAnnotation))
  }
}