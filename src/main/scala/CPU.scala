package FiveStage

import chisel3._
import chisel3.core.Input
import chisel3.experimental.MultiIOModule
import chisel3.experimental._


class CPU extends MultiIOModule {

  val testHarness = IO(
    new Bundle {
      val setupSignals = Input(new SetupSignals)
      val testReadouts = Output(new TestReadouts)
      val regUpdates   = Output(new RegisterUpdates)
      val memUpdates   = Output(new MemUpdates)
      val currentPC    = Output(UInt(32.W))
    }
  )

  /**
    You need to create the classes for these yourself
    */
  val IFBarrier  = Module(new IFBarrier).io
  val IDBarrier  = Module(new IDBarrier).io
  val EXBarrier  = Module(new EXBarrier).io
  val MEMBarrier = Module(new MEMBarrier).io

  val IF  = Module(new InstructionFetch)
  val ID  = Module(new InstructionDecode)
  val EX  = Module(new Execute)
  val MEM = Module(new MemoryFetch)


  /**
    * Setup. You should not change this code
    */
  IF.testHarness.IMEMsetup     := testHarness.setupSignals.IMEMsignals
  ID.testHarness.registerSetup := testHarness.setupSignals.registerSignals
  MEM.testHarness.DMEMsetup    := testHarness.setupSignals.DMEMsignals

  testHarness.testReadouts.registerRead := ID.testHarness.registerPeek
  testHarness.testReadouts.DMEMread     := MEM.testHarness.DMEMpeek

  /**
    * spying stuff
    */
  testHarness.regUpdates := ID.testHarness.testUpdates
  testHarness.memUpdates := MEM.testHarness.testUpdates
  testHarness.currentPC  := IF.testHarness.PC

  /**
    TODO: Your code here
    */

  // IF to ID
  IFBarrier.PCIn          := IF.io.PC
  IFBarrier.InstructionIn := IF.io.Instruction
  ID.io.Instruction       := IFBarrier.InstructionOut

  // ID to EX
  IDBarrier.InstructionIn    := ID.io.Instruction
  IDBarrier.readData1In      := ID.io.readData1
  IDBarrier.readData2In      := ID.io.readData2
  IDBarrier.controlSignalsIn := ID.io.controlSignals
  IDBarrier.branchTypeIn     := ID.io.branchType
  IDBarrier.op1SelectIn      := ID.io.op1Select
  IDBarrier.op2SelectIn      := ID.io.op2Select
  IDBarrier.immTypeIn        := ID.io.immType
  IDBarrier.ALUopIn          := ID.io.ALUop
  IDBarrier.PCIn             := IFBarrier.PCOut // passed through ID
  EX.io.Instruction    := IDBarrier.InstructionOut
  EX.io.readData1      := IDBarrier.readData1Out
  EX.io.readData2      := IDBarrier.readData2Out
  EX.io.op1Select      := IDBarrier.op1SelectOut
  EX.io.op2Select      := IDBarrier.op2SelectOut
  EX.io.immType        := IDBarrier.immTypeOut
  EX.io.branchType     := IDBarrier.branchTypeOut
  EX.io.ALUop          := IDBarrier.ALUopOut
  EX.io.PC             := IDBarrier.PCOut
  EX.io.controlSignals := IDBarrier.controlSignalsOut

  // EX to MEM
  EXBarrier.InstructionIn    := EX.io.Instruction
  EXBarrier.ALUResultIn      := EX.io.ALUResult
  EXBarrier.BranchTargetIn   := EX.io.BranchTarget
  EXBarrier.BranchTakenIn    := EX.io.BranchTaken
  EXBarrier.controlSignalsIn := IDBarrier.controlSignalsOut
  EXBarrier.readData2In      := EX.io.readData2 // passed through EX
  MEM.io.ALUResult           := EXBarrier.ALUResultOut
  MEM.io.readData2           := EXBarrier.readData2Out
  MEM.io.controlSignals      := EXBarrier.controlSignalsOut

  // MEM to WB
  MEMBarrier.InstructionIn    := EXBarrier.InstructionOut
  MEMBarrier.ALUResultIn      := EXBarrier.ALUResultOut
  MEMBarrier.controlSignalsIn := EXBarrier.controlSignalsOut
  MEMBarrier.BranchTargetIn   := EXBarrier.BranchTargetOut // BranchTarget passed through
  MEMBarrier.BranchTakenIn    := EXBarrier.BranchTakenOut // BranchTaken passed through
  MEMBarrier.DMEMDataIn       := MEM.io.DMEMData
  ID.io.ALUResultFromWB       := MEMBarrier.ALUResultOut
  ID.io.instructionFromWB     := MEMBarrier.InstructionOut
  ID.io.controlSignalsFromWB  := MEMBarrier.controlSignalsOut
  ID.io.DMEMDataFromWB        := MEMBarrier.DMEMDataOut
  IF.io.BranchTarget          := MEMBarrier.BranchTargetOut    // for branches and jumps
  IF.io.BranchTaken           := MEMBarrier.BranchTakenOut     // for branches and jumps
  IF.io.controlSignals        := MEMBarrier.controlSignalsOut
}
