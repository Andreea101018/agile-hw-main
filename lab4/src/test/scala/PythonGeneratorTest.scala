import org.scalatest.flatspec.AnyFlatSpec
import chiseltest._
import chisel3._

class PythonGeneratorTest extends AnyFlatSpec with ChiselScalatestTester {
  val annotations = Seq(WriteVcdAnnotation)
  "Python CSR Generator" should "generate CSR adapter for soc.xlsx" in {
    assume(new java.io.File("soc_adapter.sv").isFile,
      "Generate soc_adapter.sv with python3 csr_adapter_gen.py soc.xlsx to run the optional reference test")
    assume(sys.env.getOrElse("PATH", "").split(java.io.File.pathSeparator).exists(dir =>
      new java.io.File(dir, "verilator").canExecute), "The optional Python reference test requires Verilator")

    test(new PythonSocAdapterWrapper)
      .withAnnotations(Seq(VerilatorBackendAnnotation) ++ annotations) {
        dut =>
          // instantiate APB master Bus Functional Model (BFM)
          val bfm = new ApbMasterBfm(
            dut.clock,
            dut.reset,
            dut.io.psel,
            dut.io.penable,
            dut.io.paddr,
            dut.io.pwrite,
            dut.io.pwdata,
            dut.io.prdata,
            dut.io.pready,
            dut.io.pslverr
          )

          // reset DUT
          bfm.reset()

          // test read-only (ro) field
          dut.io.gpio0_dataIn.poke(0xdeadbeefL.U)
          bfm.readExpect(0x41004008, Some(0xdeadbeefL)) // gpio0 dataIn

          // test read trigger
          // spawn two threads: one to perform the apb read, another to monitor the trigger signal
          fork {
            dut.io.uart0_data_rxData.poke(0xaa.U) // uart0 rxData
            bfm.readExpect(0x41003008, Some(0xaa))
          }.fork
            .withRegion(Monitor) { // use thread ordering
              // this thread runs in the monitor region, so it can observe signals *after* the other thread has poked them
              dut.clock.step(1) // wait for access phase
              dut.io.uart0_data_rxData_trg.expect(1.B)
          }.joinAndStep()

          // test write trigger
          // spawn two threads: one to perform the apb write, another to monitor the trigger signal
          fork {
            bfm.write(0x41003008, 0x5aL) // uart0 txData
            dut.io.uart0_data_txData.expect(0x5a.U)
          }.fork
            .withRegion(Monitor) { // use thread ordering
              // this thread runs in the monitor region, so it can observe signals *after* the other thread has poked them
              dut.clock.step(1) // wait for access phase
              dut.io.uart0_data_txData_trg.expect(1.B)
          }.joinAndStep()

          // test internal register read/write
          bfm.write(0x41003000, 0xdeadbeefL)
          bfm.readExpect(0x41003000, Some(0x3)) // only lower 2 bits are writable

          // test constant
          bfm.readExpect(0x80000000L, Some(0xdeadbeefL))

          // test invalid address
          bfm.readExpect(0x50000000L, None)

      }

  }

}
