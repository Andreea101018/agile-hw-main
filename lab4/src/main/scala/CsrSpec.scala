import help.Sheet

/** Pure Scala description, consumed before any hardware is constructed. */
object CsrSpec {
  case class Field(name: String, kind: String, high: Int, low: Int, init: Option[BigInt]) {
    val width: Int = high - low + 1
    def readable: Boolean = Set("rw", "ro", "rotrg", "const")(kind)
    def writable: Boolean = Set("rw", "wotrg")(kind)
  }
  case class Register(name: String, address: BigInt, fields: Seq[Field])
  case class Block(name: String, registers: Seq[Register])

  private def number(s: String): BigInt = {
    val value = s.trim
    if (value.startsWith("0x") || value.startsWith("0X")) BigInt(value.drop(2), 16)
    else BigDecimal(value).toBigIntExact.getOrElse(
      throw new IllegalArgumentException(s"Not an integer: $s"))
  }
  private def identifier(s: String): Unit =
    require(s.matches("[A-Za-z_][A-Za-z0-9_]*"), s"Invalid CSR name: $s")

  def load(path: String): Seq[Block] = {
    val sheets = Sheet.load(path)
    def records(sheet: Sheet): Seq[Map[String, String]] =
      sheet.rows.filter(_.exists(_.trim.nonEmpty)).map(row => sheet.header.zip(row.map(_.trim)).toMap)
    val entries = records(sheets("Map"))
    require(entries.nonEmpty, "Memory map must not be empty")
    require(entries.map(_("Name")).distinct.size == entries.size, "Duplicate block instance name")
    val ranges = entries.map { entry =>
      val base = number(entry("Base Address"))
      val end = number(entry("End Address"))
      require(base >= 0 && base <= end && end < (BigInt(1) << 32), "Invalid block address range")
      (base, end)
    }
    for (i <- ranges.indices; j <- 0 until i)
      require(ranges(i)._1 > ranges(j)._2 || ranges(j)._1 > ranges(i)._2, "Overlapping block address ranges")

    entries.zip(ranges).map { case (entry, (base, end)) =>
      val name = entry("Name")
      identifier(name)
      require(entry("Interface") == "APB", s"Unsupported interface for $name")
      val rows = records(sheets(entry("Block")))
      val registers = rows.map(_("Register")).distinct.map { regName =>
        identifier(regName)
        val fields = rows.filter(_("Register") == regName)
        val offsets = fields.map(f => number(f("Offset"))).distinct
        require(offsets.size == 1, s"Inconsistent offsets for $name.$regName")
        val address = base + offsets.head
        require(offsets.head >= 0 && address % 4 == 0 && address + 3 <= end,
          s"Unaligned or out-of-range register $name.$regName")
        val parsed = fields.map { row =>
          val fieldName = row("Field")
          if (fieldName.nonEmpty) identifier(fieldName)
          val kind = row("Type")
          require(Set("rw", "ro", "wotrg", "rotrg", "const")(kind), s"Unknown field type $kind")
          val bits = row("Range").split(":")
          require(bits.length == 2, s"Invalid bit range for $name.$regName")
          val high = bits(0).toInt
          val low = bits(1).toInt
          require(low >= 0 && high >= low && high < 32, s"Invalid bit range ${row("Range")}")
          val init = if (row("Init") == "?" || row("Init").isEmpty) None else Some(number(row("Init")))
          require(kind != "const" || init.nonEmpty, "Constants require a value")
          require(init.forall(v => v >= 0 && v < (BigInt(1) << (high - low + 1))), "Initial value does not fit field")
          Field(fieldName, kind, high, low, init)
        }
        require(parsed.size == 1 || parsed.forall(_.name.nonEmpty), "Unnamed field must occupy its own register")
        require(parsed.map(_.name).distinct.size == parsed.size, "Duplicate field name")
        // Read-only and write-only views may intentionally share bits (UART data).
        for (i <- parsed.indices; j <- 0 until i) {
          val a = parsed(i); val b = parsed(j)
          val overlap = a.low <= b.high && b.low <= a.high
          require(!overlap || !(a.readable && b.readable || a.writable && b.writable),
            s"Overlapping fields for the same access direction in $name.$regName")
        }
        Register(regName, address, parsed)
      }
      require(registers.map(_.address).distinct.size == registers.size, s"Duplicate register address in $name")
      Block(name, registers)
    }
  }
}
