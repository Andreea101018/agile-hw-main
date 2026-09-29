import chisel3._
import chisel3.util._

class Simple extends Module {
  val io = IO(new Bundle {
    val in = Input(UInt(5.W))
    val out = Output(UInt(5.W))
  })

  io.out := io.in + 1.U

  assert(io.out =/= 0.U || io.in === 31.U, "Output should not be zero unless input is 31")

  // this assertion is wrong, Chisel formal will find a counterexample
  assert(io.out =/= 16.U || io.in === 16.U, "This will not be triggered by the test, but by formal verification")

}