// Very complicated logic we cannot easily test intuitively

import chisel3._
import chisel3.util._

class SHA256IO extends Bundle {
  val start = Input(Bool())
  val message = Input(UInt(512.W)) // 512-bit message block
  val hash = Output(UInt(256.W)) // 256-bit hash output
  val valid = Output(Bool()) // Output valid signal
  val ready = Output(Bool()) // Ready for new input
}

// Naive sha256 implementation
// If you want to see a goated implementation see https://github.com/cjavad/os-challenge-cmpxchg-Sammenlign-Byt/blob/main/src/sha256/x4/reverse_fullyfused_asm_x4.c
class SHA256 extends Module {
  val io = IO(new SHA256IO)

  private val K = VecInit(Seq(
    0x428a2f98L, 0x71374491L, 0xb5c0fbcfL, 0xe9b5dba5L, 0x3956c25bL, 0x59f111f1L, 0x923f82a4L, 0xab1c5ed5L,
    0xd807aa98L, 0x12835b01L, 0x243185beL, 0x550c7dc3L, 0x72be5d74L, 0x80deb1feL, 0x9bdc06a7L, 0xc19bf174L,
    0xe49b69c1L, 0xefbe4786L, 0x0fc19dc6L, 0x240ca1ccL, 0x2de92c6fL, 0x4a7484aaL, 0x5cb0a9dcL, 0x76f988daL,
    0x983e5152L, 0xa831c66dL, 0xb00327c8L, 0xbf597fc7L, 0xc6e00bf3L, 0xd5a79147L, 0x06ca6351L, 0x14292967L,
    0x27b70a85L, 0x2e1b2138L, 0x4d2c6dfcL, 0x53380d13L, 0x650a7354L, 0x766a0abbL, 0x81c2c92eL, 0x92722c85L,
    0xa2bfe8a1L, 0xa81a664bL, 0xc24b8b70L, 0xc76c51a3L, 0xd192e819L, 0xd6990624L, 0xf40e3585L, 0x106aa070L,
    0x19a4c116L, 0x1e376c08L, 0x2748774cL, 0x34b0bcb5L, 0x391c0cb3L, 0x4ed8aa4aL, 0x5b9cca4fL, 0x682e6ff3L,
    0x748f82eeL, 0x78a5636fL, 0x84c87814L, 0x8cc70208L, 0x90befffaL, 0xa4506cebL, 0xbef9a3f7L, 0xc67178f2L
  ).map(_.U(32.W)))

  private val H_INIT = Seq(
    0x6a09e667L, 0xbb67ae85L, 0x3c6ef372L, 0xa54ff53aL,
    0x510e527fL, 0x9b05688cL, 0x1f83d9abL, 0x5be0cd19L
  ).map(_.U(32.W))

  private object State extends ChiselEnum {
    val sIdle, sExpand, sCompress, sFinalize, sDone = Value
  }

  private val state = RegInit(State.sIdle)
  private val roundCounter = RegInit(0.U(log2Ceil(64).W)) // 0 to 63

  // Working variables
  private val a = RegInit(0.U(32.W))
  private val b = RegInit(0.U(32.W))
  private val c = RegInit(0.U(32.W))
  private val d = RegInit(0.U(32.W))
  private val e = RegInit(0.U(32.W))
  private val f = RegInit(0.U(32.W))
  private val g = RegInit(0.U(32.W))
  private val h = RegInit(0.U(32.W))

  private val H = RegInit(VecInit(H_INIT))
  private val W = RegInit(VecInit(Seq.fill(64)(0.U(32.W))))

  // Helper functions for SHA-256 operations
  private def rotr(x: UInt, n: Int): UInt = (x >> n) | (x << (32 - n))
  private def Ch(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ (~x & z)
  private def Maj(x: UInt, y: UInt, z: UInt): UInt = (x & y) ^ (x & z) ^ (y & z)
  private def Sigma0(x: UInt): UInt = rotr(x, 2) ^ rotr(x, 13) ^ rotr(x, 22)
  private def Sigma1(x: UInt): UInt = rotr(x, 6) ^ rotr(x, 11) ^ rotr(x, 25)
  private def sigma0(x: UInt): UInt = rotr(x, 7) ^ rotr(x, 18) ^ (x >> 3)
  private def sigma1(x: UInt): UInt = rotr(x, 17) ^ rotr(x, 19) ^ (x >> 10)

  io.ready := state === State.sIdle
  io.valid := state === State.sDone
  io.hash := Cat(H)

  switch(state) {
    is(State.sIdle) {
      when(io.start) {
        // Initialize working variables with current hash
        a := H(0)
        b := H(1)
        c := H(2)
        d := H(3)
        e := H(4)
        f := H(5)
        g := H(6)
        h := H(7)

        // Load first 16 words from message (big-endian)
        for (i <- 0 until 16) {
          W(i) := io.message((511 - i * 32), (480 - i * 32))
        }

        roundCounter := 16.U
        state := State.sExpand
      }
    }

    is(State.sExpand) {
      // Expand message schedule (words 16-63)
      val idx = roundCounter
      val s0 = sigma0(W((idx + 49.U) & 0x3F.U)) // (idx - 15) mod 64
      val s1 = sigma1(W((idx + 62.U) & 0x3F.U)) // (idx - 2) mod 64
      W(idx) := W((idx + 48.U) & 0x3F.U) + s0 + W((idx + 57.U) & 0x3F.U) + s1

      when(roundCounter === 63.U) {
        roundCounter := 0.U
        state := State.sCompress
      }.otherwise {
        roundCounter := roundCounter + 1.U
      }
    }

    is(State.sCompress) {
      // Compression function (64 rounds)
      val S1 = Sigma1(e)
      val ch = Ch(e, f, g)
      val temp1 = h + S1 + ch + K(roundCounter) + W(roundCounter)
      val S0 = Sigma0(a)
      val maj = Maj(a, b, c)
      val temp2 = S0 + maj

      h := g
      g := f
      f := e
      e := d + temp1
      d := c
      c := b
      b := a
      a := temp1 + temp2

      when(roundCounter === 63.U) {
        state := State.sFinalize
      }.otherwise {
        roundCounter := roundCounter + 1.U
      }
    }

    // Defer finalization to next cycle to avoid combinational loop
    is(State.sFinalize) {
      // Add compressed chunk to current hash
      H(0) := H(0) + a
      H(1) := H(1) + b
      H(2) := H(2) + c
      H(3) := H(3) + d
      H(4) := H(4) + e
      H(5) := H(5) + f
      H(6) := H(6) + g
      H(7) := H(7) + h
      state := State.sDone
    }

    is(State.sDone) {
      when(io.start) {
        io.valid := false.B
        state := State.sIdle
      }.otherwise {
        io.valid := true.B
      }
    }
  }
}

// Test harness generator (for simulation) claude has clearly read martins book and puts the verilog file in the 
// generated directory.
object SHA256 extends App {
  emitVerilog(new SHA256, Array("--target-dir", "generated"))
}