import chisel3._
import chiseltest._
import org.scalatest.flatspec.AnyFlatSpec

// A trait defining the interface for vending machine implementations
// This allows us to test both software and hardware implementations against the same specification
trait VendingMachineImpl {
  def insertCoin(amount: Int): Unit

  def selectItem(itemId: Int): (Boolean, Int) // Returns (dispensed: Boolean, change: Int)

  def reset(): Unit
}

// Software model of a vending machine
// This is our "golden model" or source of truth
class VendingMachineModel(prices: Map[Int, Int]) extends VendingMachineImpl {
  private var balance: Int = 0

  def insertCoin(amount: Int): Unit = {
    require(amount >= 0, "Coin amount must be non-negative")
    balance += amount
  }

  def selectItem(itemId: Int): (Boolean, Int) = {
    prices.get(itemId) match {
      case Some(price) =>
        if (balance >= price) {
          val change = balance - price
          balance = 0 // Clear balance after successful purchase
          (true, change)
        } else {
          // Insufficient funds - keep balance for next insertion
          (false, 0)
        }
      case None =>
        // Invalid item ID - don't dispense, don't clear balance
        (false, 0)
    }
  }

  def reset(): Unit = {
    balance = 0
  }
}

// Chisel implementation wrapper
// TODO: Implement all methods from VendingMachineImpl trait
// class VendingMachineChisel(dut: VendingMachine) extends VendingMachineImpl {
//   ???
// }

// Simple tester that compares multiple implementations
// Executes a sequence of operations on all implementations and verifies they produce identical results
class VendingMachineSimpleTester(implementations: Seq[VendingMachineImpl]) {

  // Test a single transaction sequence
  def testTransaction(description: String)(operations: VendingMachineImpl => (Boolean, Int)): Unit = {
    // Reset all implementations first
    implementations.foreach(_.reset())

    // Run operations on all implementations
    val results = implementations.map(operations)

    // Verify all implementations agree
    assert(results.distinct.size == 1,
      s"Mismatch for '$description': ${results.mkString(", ")}")
  }
}

// Main test class
class VendingMachineTest extends AnyFlatSpec with ChiselScalatestTester {

  // Define sample prices in Danish Crowns (DKK)
  val samplePrices = Map(
    0 -> 15, // Cola
    1 -> 10, // Chips
    2 -> 5, // Candy
    3 -> 12 // Water
  )

  behavior of "VendingMachine"

  it should "dispense item with exact payment" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      tester.testTransaction("exact payment for candy (5 DKK)") { impl =>
        impl.insertCoin(5)
        impl.selectItem(2) // Candy costs 5 DKK
      }
    }
  }

  it should "return correct change on overpayment" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      tester.testTransaction("overpayment for candy (10 DKK -> 5 DKK change)") { impl =>
        impl.insertCoin(10)
        val result = impl.selectItem(2) // Candy costs 5 DKK
        assert(result._1, "Item should be dispensed")
        assert(result._2 == 5, "Change should be 5 DKK")
        result
      }
    }
  }

  it should "not dispense on insufficient funds" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      tester.testTransaction("insufficient funds (3 DKK < 5 DKK)") { impl =>
        impl.insertCoin(3)
        val result = impl.selectItem(2) // Candy costs 5 DKK
        assert(result._1 == false, "Item should not be dispensed")
        assert(result._2 == 0, "No change should be returned")
        result
      }
    }
  }

  it should "accumulate coins over multiple insertions" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      tester.testTransaction("accumulate 2 + 3 + 5 = 10 DKK for chips") { impl =>
        impl.insertCoin(2)
        impl.insertCoin(3)
        impl.insertCoin(5)

        val result = impl.selectItem(1) // Chips cost 10 DKK
        assert(result._1 == true, "Item should be dispensed")
        assert(result._2 == 0, "No change (exact amount)")
        result
      }
    }
  }

  it should "handle multiple sequential transactions" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      // Transaction 1: Buy candy
      tester.testTransaction("transaction 1: candy") { impl =>
        impl.insertCoin(5)
        impl.selectItem(2)
      }

      // Transaction 2: Buy chips
      tester.testTransaction("transaction 2: chips") { impl =>
        impl.insertCoin(10)
        impl.selectItem(1)
      }

      // Transaction 3: Buy cola with change
      tester.testTransaction("transaction 3: cola with change") { impl =>
        impl.insertCoin(20)
        val result = impl.selectItem(0) // Cola costs 15 DKK
        assert(result._1, "Item should be dispensed")
        assert(result._2 == 5, "Should return 5 DKK change")
        result
      }
    }
  }

  it should "handle invalid item IDs gracefully" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val tester = new VendingMachineSimpleTester(Seq(model)) // TODO: Add chiselImpl when implemented

      tester.testTransaction("invalid item ID preserves balance") { impl =>
        impl.insertCoin(20)

        // Try to select invalid item
        val invalidResult = impl.selectItem(99) // Invalid item ID
        assert(invalidResult._1 == false, "Should not dispense")
        assert(invalidResult._2 == 0, "No change")

        // Verify balance was preserved by purchasing a valid item
        val validResult = impl.selectItem(0) // Cola costs 15 DKK
        assert(validResult._1 == true, "Should dispense cola")
        assert(validResult._2 == 5, "Should return 5 DKK change")
        validResult
      }
    }
  }

  it should "test all items with various payment combinations" in {
    test(new VendingMachine(samplePrices)) { dut =>
      val model = new VendingMachineModel(samplePrices)
      // val chiselImpl = new VendingMachineChisel(dut)
      val implementations = Seq(model) // TODO: Add chiselImpl when implemented

      // Test each item
      for ((itemId, price) <- samplePrices) {
        // Reset
        implementations.foreach(_.reset())

        // Test with exact amount
        implementations.foreach { impl =>
          impl.insertCoin(price)
          val (dispensed, change) = impl.selectItem(itemId)
          assert(dispensed, s"Item $itemId should dispense with exact payment")
          assert(change == 0, s"Item $itemId should have no change with exact payment")
        }

        // Reset for overpayment test
        implementations.foreach(_.reset())

        // Test with overpayment
        implementations.foreach { impl =>
          impl.insertCoin(price + 7)
          val (dispensed, change) = impl.selectItem(itemId)
          assert(dispensed, s"Item $itemId should dispense with overpayment")
          assert(change == 7, s"Item $itemId should return 7 DKK change")
        }
      }
    }
  }
}
