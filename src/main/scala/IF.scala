package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class InstructionFetch extends MultiIOModule {

  // Don't touch
  val testHarness = IO(
    new Bundle {
      val IMEMsetup = Input(new IMEMsetupSignals)
      val PC        = Output(UInt())
    }
  )


  /**
    * TODO: Add input signals for handling events such as jumps

    * TODO: Add output signal for the instruction. 
    * The instruction is of type Bundle, which means that you must
    * use the same syntax used in the testHarness for IMEM setup signals
    * further up.
    */
  val io = IO(
    new Bundle {
      val PC = Output(UInt(32.W))
      val Instruction = Output(new Instruction)

      // from WB stage
      val BranchTarget = Input(UInt(32.W))
      val controlSignals = Input(new ControlSignals)
    })

  val IMEM = Module(new IMEM)
  val PC   = RegInit(UInt(32.W), 0.U)


  /**
    * Setup. You should not change this code
    */
  IMEM.testHarness.setupSignals := testHarness.IMEMsetup
  testHarness.PC := IMEM.testHarness.requestedAddress


  /**
    * TODO: Your code here.
    * 
    * You should expand on or rewrite the code below.
    */
  io.PC := PC
  IMEM.io.instructionAddress := PC

  when(io.controlSignals.jump) {
    PC := io.BranchTarget
  }.otherwise {
    PC := PC + 4.U
  }

  val instruction = Wire(new Instruction)
  instruction := IMEM.io.instruction.asTypeOf(new Instruction)

  io.Instruction := instruction

  /**
    * Setup. You should not change this code.
    */
  when(testHarness.IMEMsetup.setup) {
    PC := 0.U
    instruction := Instruction.NOP
  }

  /**
    * Debug signals
    */
  KeepSignal(io.Instruction.instruction)
} 
  
