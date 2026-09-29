import org.scalatest.flatspec.AnyFlatSpec
import chiseltest._
import chisel3._
import help.DynamicBundle

class GeneratorTest extends AnyFlatSpec with ChiselScalatestTester {
  private def port(dut: CsrAdapter, path: String): Data =
    path.split('.').foldLeft(dut.csr: Data)((node, name) => node.asInstanceOf[DynamicBundle](name))
  private def uint(dut: CsrAdapter, path: String): UInt = port(dut, path).asInstanceOf[UInt]
  private def bool(dut: CsrAdapter, path: String): Bool = port(dut, path).asInstanceOf[Bool]
  private def bus(dut: CsrAdapter): ApbMasterBfm = new ApbMasterBfm(
    dut.clock, dut.reset, dut.apb.psel, dut.apb.penable, dut.apb.paddr,
    dut.apb.pwrite, dut.apb.pwdata, dut.apb.prdata, dut.apb.pready, dut.apb.pslverr)
  private def initialize(dut: CsrAdapter): Unit = {
    dut.apb.psel.poke(false.B); dut.apb.penable.poke(false.B)
    dut.apb.pwrite.poke(false.B); dut.apb.paddr.poke(0.U); dut.apb.pwdata.poke(0.U)
    uint(dut, "uart0.status.txEmpty").poke(0.U)
    uint(dut, "uart0.status.rxReady").poke(0.U)
    uint(dut, "uart0.data.rxData.data").poke(0.U)
    uint(dut, "gpio0.dataIn").poke(0.U)
    uint(dut, "gpio1.dataIn").poke(0.U)
    bus(dut).reset()
  }

  "Chisel CSR Generator" should "implement all register types and isolate block instances" in {
    test(new CsrAdapter("soc.xlsx")) { dut =>
      initialize(dut)
      val bfm = bus(dut)
      bfm.readExpect(0x80000000L, Some(BigInt("deadbeef", 16)))
      bfm.readExpect(0x41003000L, Some(0))
      assert(bfm.write(0x41003000L, BigInt("deadbeef", 16)).nonEmpty)
      bfm.readExpect(0x41003000L, Some(3))
      uint(dut, "uart0.ctrl.en").expect(1.U)
      uint(dut, "uart0.ctrl.loopback").expect(1.U)
      uint(dut, "uart0.status.txEmpty").poke(1.U)
      uint(dut, "uart0.status.rxReady").poke(1.U)
      bfm.readExpect(0x41003004L, Some(3))
      uint(dut, "uart0.status.txEmpty").poke(0.U)
      bfm.readExpect(0x41003004L, Some(2))

      for ((name, base) <- Seq("gpio0" -> 0x41004000L, "gpio1" -> 0x41004010L)) {
        for (offset <- Seq(0, 4, 12)) bfm.readExpect(base + offset, Some(0))
        uint(dut, s"$name.dataIn").poke("h89abcdef".U)
        bfm.readExpect(base + 8, Some(BigInt("89abcdef", 16)))
        assert(bfm.write(base, 0xff).nonEmpty)
        bfm.readExpect(base, Some(1))
        assert(bfm.write(base + 4, BigInt("ffffffff", 16)).nonEmpty)
        uint(dut, s"$name.dir").expect("hffffffff".U)
        assert(bfm.write(base + 12, base).nonEmpty)
        bfm.readExpect(base + 12, Some(base))
        uint(dut, s"$name.dataOut").expect(base.U)
      }
      bfm.readExpect(0x4100400cL, Some(0x41004000L))
      bfm.readExpect(0x4100401cL, Some(0x41004010L))
      bfm.reset()
      bfm.readExpect(0x41003000L, Some(0))
      bfm.readExpect(0x41004004L, Some(0))
      bfm.readExpect(0x4100401cL, Some(0))
    }
  }

  it should "reject invalid addresses and writes to read-only registers" in {
    test(new CsrAdapter("soc.xlsx")) { dut =>
      initialize(dut)
      val bfm = bus(dut)
      for (address <- Seq(0x50000000L, 0x41003001L, 0x4100300cL, 0x80000004L)) {
        bfm.readExpect(address, None)
        assert(bfm.write(address, 123).isEmpty)
      }
      for (address <- Seq(0x80000000L, 0x41003004L, 0x41004008L, 0x41004018L))
        assert(bfm.write(address, 123).isEmpty)
      bfm.readExpect(0x80000000L, Some(BigInt("deadbeef", 16)))
    }
  }

  it should "gate writes and triggers with APB access and support back-to-back transfers" in {
    test(new CsrAdapter("soc.xlsx")) { dut =>
      initialize(dut)
      val tx = bool(dut, "uart0.data.txData.trg")
      val rx = bool(dut, "uart0.data.rxData.trg")
      val txData = uint(dut, "uart0.data.txData.data")
      dut.apb.paddr.poke(0x41003008L.U)
      dut.apb.pwdata.poke(0x15a.U)
      dut.apb.pwrite.poke(true.B)
      dut.apb.psel.poke(true.B)
      tx.expect(false.B); rx.expect(false.B)
      dut.clock.step() // setup
      dut.apb.penable.poke(true.B)
      dut.apb.pready.expect(true.B); dut.apb.pslverr.expect(false.B)
      tx.expect(true.B); rx.expect(false.B); txData.expect(0x5a.U)
      dut.clock.step() // completing write edge

      // Keep PSEL asserted and switch to the next transfer's setup phase.
      dut.apb.penable.poke(false.B)
      dut.apb.pwrite.poke(false.B)
      uint(dut, "uart0.data.rxData.data").poke(0xab.U)
      tx.expect(false.B); rx.expect(false.B); txData.expect(0x5a.U)
      dut.clock.step()
      dut.apb.penable.poke(true.B)
      rx.expect(true.B); tx.expect(false.B)
      dut.apb.prdata.expect(0xab.U)
      dut.clock.step()
      dut.apb.psel.poke(false.B)
      rx.expect(false.B); tx.expect(false.B)

      // PENABLE without PSEL, and setup without PENABLE, must not write.
      dut.apb.pwrite.poke(true.B)
      dut.apb.paddr.poke(0x41003000L.U)
      dut.apb.pwdata.poke(3.U)
      dut.clock.step(2)
      uint(dut, "uart0.ctrl.en").expect(0.U)
      dut.apb.psel.poke(true.B); dut.apb.penable.poke(false.B)
      dut.clock.step(2)
      uint(dut, "uart0.ctrl.en").expect(0.U)
      dut.apb.penable.poke(true.B)
      dut.clock.step()
      uint(dut, "uart0.ctrl.en").expect(1.U)
    }
  }
}
