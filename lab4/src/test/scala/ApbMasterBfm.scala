import chisel3._
import chiseltest._

class ApbMasterBfm(
    clock: Clock,
    reset: Reset,
    psel: Bool,
    penable: Bool,
    paddr: UInt,
    pwrite: Bool,
    pwdata: UInt,
    prdata: UInt,
    pready: Bool,
    pslverr: Bool
) {

  private def awaitReady(): Unit = {
    var cycles = 0
    while (!pready.peekBoolean() && cycles < 100) {
      clock.step()
      cycles += 1
    }
    assert(pready.peekBoolean(), "APB transaction timed out after 100 wait cycles")
  }

  /// Apply reset to the DUT
  def reset(): Unit = {
    reset.poke(1.B)
    clock.step()
    reset.poke(0.B)
  }

  /**
    * Perform an APB read transaction
    * 
    * If the read is successful, returns Some(data), otherwise None
    *
    * @param address The address to read from
    */
  def read(address: BigInt): Option[BigInt] = {

    // setup phase
    psel.poke(1.B)
    penable.poke(0.B)
    paddr.poke(address.U)
    pwrite.poke(0.B)

    clock.step()

    // access phase
    penable.poke(1.B)

    awaitReady()

    val res = if (pslverr.peekBoolean()) None else Some(prdata.peekInt())

    clock.step()

    // return to idle
    psel.poke(0.B)
    penable.poke(0.B)
    paddr.poke(0.U)
    pwrite.poke(0.U)

    return res
  }

  /**
    * Perform an APB read transaction and check the result
    * 
    * If a failure is expected, pass None as expected value
    *
    * @param address The address to read from
    * @param expected The expected value
    */
  def readExpect(address: BigInt, expected: Option[BigInt]): Unit = {
    // setup phase
    psel.poke(1.B)
    penable.poke(0.B)
    paddr.poke(address.U)
    pwrite.poke(0.B)

    clock.step()

    // access phase
    penable.poke(1.B)

    awaitReady()

    expected match {
      case None => pslverr.expect(1.B, s"Expected error on read at address 0x${address.toString(16)}")
      case Some(v) => {
        pslverr.expect(0.B, s"Expected no error for read at address 0x${address.toString(16)}")
        prdata.expect(v.U, s"Expected data 0x${v.toString(16)} for read at address 0x${address.toString(16)}")
      }
    }

    clock.step()

    // return to idle
    psel.poke(0.B)
    penable.poke(0.B)
    paddr.poke(0.U)
    pwrite.poke(0.U)
  }

  /**
    * Perform an APB write transaction
    * 
    * Ignores any errors
    *
    * @param address The address to write to
    * @param data The data to write
    */
  def write(address: BigInt, data: BigInt): Option[Unit] = {
    // setup phase
    psel.poke(1.B)
    penable.poke(0.B)
    paddr.poke(address.U)
    pwrite.poke(1.B)
    pwdata.poke(data.U)

    clock.step()

    // access phase
    penable.poke(1.B)

    awaitReady()

    val res = if (pslverr.peekBoolean()) None else Some(())

    clock.step()

    // return to idle
    psel.poke(0.B)
    penable.poke(0.B)
    paddr.poke(0.U)
    pwrite.poke(0.U)

    res
  }
}
