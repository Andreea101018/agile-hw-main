# Lab Session 6: Building a Priority List Using a Systolic Array

## Objective
In this lab, students will design, test, and verify a systolic array-based priority list using **Chisel**. The lab emphasizes **test-driven development (TDD)**, **property-based testing**, and **continuous integration (CI)**, culminating in the integration of **scan chains** for enhanced testability.

---

## Lab Steps

### 1. **Design and Test Individual Nodes (Unit Testing)**
- **Goal**: Implement a single node in the systolic array that compares and forwards key-value pairs.
- **Tasks**:
  - Write Chisel code for the node.
  - Use **TDD**: Write unit tests before implementing functionality.
  - Use **ChiselTest** for simulation.
  - Cover basic behaviors: compare, forward, hold, etc.
- **Objectives**:
  - Node implementation.
  - Unit tests verifying node behavior.

---

### 2. **Setup Continuous Integration (CI)**
- **Goal**: Ensure all tests run automatically on every commit or pull request.
- **Tasks**:
  - Create `.github/workflows/ci.yml` using GitHub Actions. _Remember: GitHub Actions is only free on public repositories._
  - Include steps for checking out code, setting up Java and sbt, and running tests.
  - Push changes to GitHub and verify CI runs correctly.
- **Objectives**:
  - Working CI pipeline.
  - CI fails on failing tests, succeeds on succeeding tests.

---

### 3. **Integrate Property-Based Testing (PBT)**
- **Goal**: Improve test coverage using randomized inputs.
- **Tasks**:
  - Use **ScalaCheck** with ChiselTest.
  - Define properties such as:
    - “Forwarded key is always less than or equal to incoming key.”
    - “Equal keys preserve insertion order.”
  - Implement input generators and golden models.
  - Convert failing cases into regression tests.
- **Objectives**:
  - Property-based test suite.
  - Updated unit tests using PBT.

---

### 4. **Build and Test the Full Array (Integration Testing)**
- **Goal**: Connect multiple nodes to form the full systolic array.
- **Tasks**:
  - Implement array construction with configurable depth.
  - Use **TDD** and **PBT** to verify:
    - Keys bubble correctly.
    - PUSH/POP operations behave as expected.
    - Throughput matches design (1 pop every 2 cycles).
  - Regularly push to GitHub to verify CI.
- **Objectives**:
  - Full array implementation.
  - Integration tests and property-based tests.

---

### 5. **Integrate Scan Chains**
- **Goal**: Enhance testability using scan chains.
- **Tasks**:
  - Modify array design to include scan-in and scan-out ports.
  - Enable initialization and readout via scan chains.
  - Update property-based tests to:
    - Initialize array using scan-in.
    - Optionally verify results using scan-out.
- **Objectives**:
  - Updated array design with scan chains.
  - Tests demonstrating scan chain usage.
  - CI verification of scan chain tests.

