package FiveStage
import chisel3._
import chisel3.util.{ BitPat, MuxCase }
import chisel3.experimental.MultiIOModule


class InstructionDecode extends MultiIOModule {

  // Don't touch the test harness
  val testHarness = IO(
    new Bundle {
      val registerSetup = Input(new RegisterSetupSignals)
      val registerPeek  = Output(UInt(32.W))

      val testUpdates   = Output(new RegisterUpdates)
    })


  val io = IO(
    new Bundle {
      /**
        * TODO: Your code here.
        */
      val Instruction        = Input(new Instruction)

      // from write back stage
      val ALUResultFromWB      = Input(UInt(32.W))
      val controlSignalsFromWB = Input(new ControlSignals)
      val instructionFromWB    = Input(new Instruction)
      val DMEMDataFromWB       = Input(UInt(32.W))
      val StageControl         = Input(UInt(2.W))
  
      val readData1 = Output(UInt(32.W))
      val readData2 = Output(UInt(32.W))

      val controlSignals = Output(new ControlSignals)
      val branchType     = Output(UInt(3.W))
      val op1Select      = Output(UInt(1.W))
      val op2Select      = Output(UInt(1.W))
      val immType        = Output(UInt(3.W))
      val ALUop          = Output(UInt(4.W))
    }
  )

  val registers = Module(new Registers)
  val decoder   = Module(new Decoder).io


  /**
    * Setup. You should not change this code
    */
  registers.testHarness.setup := testHarness.registerSetup
  testHarness.registerPeek    := registers.io.readData1
  testHarness.testUpdates     := registers.testHarness.testUpdates

  /**
    * TODO: Your code here.
    */

  // register file
  registers.io.readAddress1 := io.Instruction.registerRs1
  registers.io.readAddress2 := io.Instruction.registerRs2

  io.readData1 := registers.io.readData1
  io.readData2 := registers.io.readData2

  // these two get their values from the WB stage
  registers.io.writeAddress := io.instructionFromWB.registerRd
  registers.io.writeEnable  := io.controlSignalsFromWB.regWrite && io.StageControl != StageControl.stall

  // Loads vs alu operations
  when(io.controlSignalsFromWB.memRead) {
    registers.io.writeData    := io.DMEMDataFromWB
  }.otherwise {
    registers.io.writeData    := io.ALUResultFromWB
  }

  // decoder
  decoder.instruction := io.Instruction

  // control
  io.controlSignals := decoder.controlSignals
  io.branchType := decoder.branchType
  io.op1Select := decoder.op1Select
  io.op2Select := decoder.op2Select
  io.immType := decoder.immType
  io.ALUop := decoder.ALUop

  // Debug signals
  KeepSignal(io.readData1)
  KeepSignal(io.readData2)
}
