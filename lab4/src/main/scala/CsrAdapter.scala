import help._
import chisel3._
import chisel3.util._

class ApbPort extends Bundle {
  val psel = Input(Bool())
  val penable = Input(Bool())
  val pwrite = Input(Bool())
  val paddr = Input(UInt(32.W))
  val pwdata = Input(UInt(32.W))
  val prdata = Output(UInt(32.W))
  val pready = Output(Bool())
  val pslverr = Output(Bool())
}

/** Zero-wait-state APB adapter. The spreadsheet determines both ports and logic. */
class CsrAdapter(descriptionSheetPath: String) extends Module {
  private val blocks = CsrSpec.load(descriptionSheetPath)

  private def fieldType(field: CsrSpec.Field): Data = field.kind match {
    case "rw" => Output(UInt(field.width.W))
    case "ro" => Input(UInt(field.width.W))
    case "wotrg" => new DynamicBundle(Seq(
      "data" -> Output(UInt(field.width.W)), "trg" -> Output(Bool())))
    case "rotrg" => new DynamicBundle(Seq(
      "data" -> Input(UInt(field.width.W)), "trg" -> Output(Bool())))
  }
  private def registerType(reg: CsrSpec.Register): Data = {
    val fields = reg.fields.filter(_.kind != "const")
    if (fields.head.name.isEmpty) fieldType(fields.head)
    else new DynamicBundle(fields.map(f => f.name -> fieldType(f)))
  }

  val apb = IO(new ApbPort)
  val csr = IO(new DynamicBundle(blocks.flatMap { block =>
    val registers = block.registers.filter(_.fields.exists(_.kind != "const"))
    if (registers.isEmpty) None
    else Some(block.name -> new DynamicBundle(registers.map(r => r.name -> registerType(r))))
  }))

  // Side effects occur only on an APB access, never during setup or idle.
  val access = apb.psel && apb.penable
  val write = access && apb.pwrite
  val read = access && !apb.pwrite
  apb.pready := true.B
  apb.prdata := 0.U
  apb.pslverr := access // An unmapped address or disallowed direction is an error.

  for (block <- blocks; reg <- block.registers) {
    val selected = apb.paddr === reg.address.U(32.W)
    val readFields = reg.fields.filter(_.readable)
    val writeFields = reg.fields.filter(_.writable)
    if (readFields.nonEmpty) when(read && selected) { apb.pslverr := false.B }
    if (writeFields.nonEmpty) when(write && selected) { apb.pslverr := false.B }

    val values = reg.fields.flatMap { field =>
      val port = if (field.kind == "const") None else {
        val register = csr(block.name).asInstanceOf[DynamicBundle](reg.name)
        Some(if (field.name.isEmpty) register else register.asInstanceOf[DynamicBundle](field.name))
      }
      val value: UInt = field.kind match {
        case "rw" | "wotrg" =>
          val storage = field.init match {
            case Some(init) => RegInit(init.U(field.width.W))
            case None => Reg(UInt(field.width.W))
          }
          when(write && selected) { storage := apb.pwdata(field.high, field.low) }
          if (field.kind == "rw") port.get.asInstanceOf[UInt] := storage
          else {
            val trigger = port.get.asInstanceOf[DynamicBundle]
            // Present the current write data while trg is high so a synchronous
            // consumer samples the new value at the completing clock edge.
            trigger("data") := Mux(write && selected, apb.pwdata(field.high, field.low), storage)
            trigger("trg") := write && selected
          }
          storage
        case "ro" => port.get.asInstanceOf[UInt]
        case "rotrg" =>
          val trigger = port.get.asInstanceOf[DynamicBundle]
          trigger("trg") := read && selected
          trigger("data").asInstanceOf[UInt]
        case "const" => field.init.get.U(field.width.W)
      }
      if (field.readable) Some(value.pad(32) << field.low) else None
    }
    if (values.nonEmpty) when(read && selected) {
      apb.prdata := values.reduce(_ | _)
    }
  }
}

object CsrAdapter extends App {
  emitVerilog(new CsrAdapter("soc.xlsx"), Array("--target-dir", "generated"))
}
