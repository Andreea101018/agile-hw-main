import chisel3._
import chisel3.util._

// src: https://github.com/cjavad/cpu_slow/blob/main/src/main/scala/ALU.scala
class ALU extends Module {
  val io = IO(new Bundle {
    val in_signed = Input(Bool())
    val in_sel = Input(UInt(5.W))

    val in_op1 = Input(UInt(32.W))
    val in_op2 = Input(UInt(32.W))

    val in_op1_signed = Input(SInt(32.W))
    val in_op2_signed = Input(SInt(32.W))

    val out_result = Output(UInt(32.W))
    val out_result_signed = Output(SInt(32.W))

    // < = > <= >=
    val out_comp = Output(Vec(5, Bool()))
    val out_result_comp_0 = Output(Vec(5, Bool()))
  })

  io.out_result := 0.U
  io.out_result_signed := 0.S

  switch(io.in_sel) {
    is(0.U) {
      io.out_result := ~io.in_op1
      io.out_result_signed := ~io.in_op1_signed
    }
    is(1.U) {
      io.out_result := io.in_op1 & io.in_op2
      io.out_result_signed := io.in_op1_signed & io.in_op2_signed
    }
    is(2.U) {
      io.out_result := io.in_op1 | io.in_op2
      io.out_result_signed := io.in_op1_signed | io.in_op2_signed
    }
    is(3.U) {
      io.out_result := io.in_op1 ^ io.in_op2
      io.out_result_signed := io.in_op1_signed ^ io.in_op2_signed
    }
    is(4.U) {
      io.out_result := io.in_op1 + io.in_op2
      io.out_result_signed := io.in_op1_signed + io.in_op2_signed
    }
    is(5.U) {
      io.out_result := io.in_op1 - io.in_op2
      io.out_result_signed := io.in_op1_signed - io.in_op2_signed
    }
    is(6.U) {
      io.out_result := io.in_op1 / io.in_op2
      io.out_result_signed := io.in_op1_signed / io.in_op2_signed
    }
    is(7.U) {
      io.out_result := io.in_op1 % io.in_op2
      io.out_result_signed := io.in_op1_signed % io.in_op2_signed
    }
    is(8.U) {
      io.out_result := io.in_op1 * io.in_op2
      io.out_result_signed := io.in_op1_signed * io.in_op2_signed

    }
    is(9.U) {
      io.out_result := io.in_op1 >> io.in_op2(5, 0)
      io.out_result_signed := io.in_op1_signed >> io.in_op2_signed(5, 0)
    }
    is(10.U) {
      io.out_result := io.in_op1 << io.in_op2(5, 0)
      io.out_result_signed := io.in_op1_signed << io.in_op2_signed(5, 0)
    }

    is(11.U) {}
    is(12.U) {}
    is(13.U) {}
    is(14.U) {}
    is(15.U) {}
  }

  // Comp flags

  val is_greater = io.in_op1 > io.in_op2
  val is_lesser = io.in_op1 < io.in_op2
  val is_equal = io.in_op1 === io.in_op2

  val is_greater_s = io.in_op1_signed > io.in_op2_signed
  val is_lesser_s = io.in_op1_signed < io.in_op2_signed
  val is_equal_s = io.in_op1_signed === io.in_op2_signed


  val is_lesser_0 = io.out_result < 0.U
  val is_greater_0 = io.out_result > 0.U
  val is_equal_0 = io.out_result === 0.U


  val is_lesser_0_s = io.out_result_signed < 0.S
  val is_greater_0_s = io.out_result_signed > 0.S
  val is_equal_0_s = io.out_result_signed === 0.S


  io.out_comp(0) := Mux(io.in_signed, is_lesser_s, is_lesser)
  io.out_comp(1) := Mux(io.in_signed, is_equal_s, is_equal)
  io.out_comp(2) := Mux(io.in_signed, is_greater_s, is_greater)
  io.out_comp(3) := Mux(io.in_signed, is_lesser_s | is_equal_s, is_lesser | is_equal)
  io.out_comp(4) := Mux(io.in_signed, is_greater_s | is_equal_s, is_greater | is_equal)

  io.out_result_comp_0(0) := Mux(io.in_signed, is_lesser_0_s, is_lesser_0)
  io.out_result_comp_0(1) := Mux(io.in_signed, is_equal_0_s, is_equal_0)
  io.out_result_comp_0(2) := Mux(io.in_signed, is_greater_0_s, is_greater_0)
  io.out_result_comp_0(3) := Mux(io.in_signed, is_lesser_0_s | is_equal_0_s, is_lesser_0 | is_equal_0)
  io.out_result_comp_0(4) := Mux(io.in_signed, is_greater_0_s | is_equal_0_s, is_greater_0 | is_equal_0)
}