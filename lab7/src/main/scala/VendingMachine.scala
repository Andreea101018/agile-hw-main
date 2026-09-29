import chisel3._
import chisel3.util._

// TODO: Implement a VendingMachine module that matches the behavior of VendingMachineModel
//
// Requirements:
// - Accept a Map[Int, Int] of item IDs to prices (in Danish Crowns)
// - Support inserting coins and accumulating balance
// - Support selecting items for purchase
// - Dispense item when balance is sufficient
// - Return change when overpaying
// - Handle invalid item IDs gracefully
//
// Hints:
// - You need to design the IO interface yourself
// - Think about what inputs and outputs you need
// - Consider using registers to maintain state
// - The prices Map can be used in hardware with VecInit or MuxLookup
//
// Refer to VendingMachineModel in the test file to understand the expected behavior

class VendingMachine(prices: Map[Int, Int]) extends Module {
  // TODO: Define your IO bundle here

  // TODO: Implement your vending machine logic here
}
