import org.scalatest.flatspec.AnyFlatSpec
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import java.nio.file.Files

import chisel3._
import chiseltest._
import help.DynamicBundle

class CsrSpecTest extends AnyFlatSpec with ChiselScalatestTester {
  private def withSheet(rows: Seq[Seq[String]])(check: String => Unit): Unit = {
    val path = Files.createTempFile("csr-spec-", ".xlsx")
    val book = new XSSFWorkbook()
    def sheet(name: String, data: Seq[Seq[String]]): Unit = {
      val s = book.createSheet(name)
      data.zipWithIndex.foreach { case (values, i) =>
        val row = s.createRow(i)
        values.zipWithIndex.foreach { case (v, j) => row.createCell(j).setCellValue(v) }
      }
    }
    sheet("Map", Seq(Seq("Block", "Name", "Interface", "Base Address", "End Address"),
      Seq("Test", "unit0", "APB", "0x1000", "0x10ff")))
    sheet("Test", Seq(Seq("Register", "Offset", "Field", "Type", "Range", "Init")) ++ rows)
    try {
      val stream = Files.newOutputStream(path)
      try book.write(stream) finally stream.close()
      book.close()
      check(path.toString)
    } finally Files.deleteIfExists(path)
  }

  "CSR specification" should "accept directional overlap and parse hexadecimal and decimal values" in {
    withSheet(Seq(
      Seq("data", "0x0", "tx", "wotrg", "15:8", "0x42"),
      Seq("data", "0x0", "rx", "rotrg", "15:8", "?"),
      Seq("ctrl", "4.0", "", "rw", "7:0", "7.0"))) { path =>
      val regs = CsrSpec.load(path).head.registers
      assert(regs.head.address == 0x1000)
      assert(regs.head.fields.head.width == 8)
      assert(regs.head.fields.head.init.contains(BigInt(0x42)))
      assert(regs(1).fields.head.init.contains(BigInt(7)))
    }
  }

  it should "generate shifted fields and nonzero reset values from a different workbook" in {
    withSheet(Seq(
      Seq("config", "0x0", "mode", "rw", "15:8", "0x42"),
      Seq("config", "0x0", "version", "const", "23:16", "0xab"),
      Seq("event", "0x4", "", "wotrg", "7:0", "0"))) { path =>
      test(new CsrAdapter(path)) { dut =>
        val bfm = new ApbMasterBfm(dut.clock, dut.reset, dut.apb.psel, dut.apb.penable,
          dut.apb.paddr, dut.apb.pwrite, dut.apb.pwdata, dut.apb.prdata, dut.apb.pready, dut.apb.pslverr)
        dut.apb.psel.poke(false.B); dut.apb.penable.poke(false.B)
        dut.apb.pwrite.poke(false.B); dut.apb.paddr.poke(0.U); dut.apb.pwdata.poke(0.U)
        bfm.reset()
        bfm.readExpect(0x1000, Some(0xab4200))
        assert(bfm.write(0x1000, BigInt("ffffffff", 16)).nonEmpty)
        bfm.readExpect(0x1000, Some(0xabff00))
        val block = dut.csr("unit0").asInstanceOf[DynamicBundle]
        block("config").asInstanceOf[DynamicBundle]("mode").asInstanceOf[UInt].expect(255.U)
        bfm.readExpect(0x1004, None) // write-only register
        assert(bfm.write(0x1004, 0x123).nonEmpty)
        block("event").asInstanceOf[DynamicBundle]("data").asInstanceOf[UInt].expect(0x23.U)
        bfm.reset()
        bfm.readExpect(0x1000, Some(0xab4200))
      }
    }
  }

  it should "reject malformed definitions before elaboration" in {
    val bad = Seq(
      Seq(Seq("r", "0x0", "", "unknown", "7:0", "0")),
      Seq(Seq("r", "0x1", "", "rw", "7:0", "0")),
      Seq(Seq("r", "0x100", "", "rw", "7:0", "0")),
      Seq(Seq("r", "0x0", "", "rw", "32:0", "0")),
      Seq(Seq("r", "0x0", "", "rw", "7:0", "256")),
      Seq(Seq("r", "0x0", "", "const", "7:0", "?")),
      Seq(Seq("r", "0x0", "a", "rw", "7:0", "0"),
          Seq("r", "0x0", "b", "ro", "4:0", "?")))
    for (rows <- bad) withSheet(rows) { path =>
      intercept[IllegalArgumentException] { CsrSpec.load(path) }
    }
  }
}
