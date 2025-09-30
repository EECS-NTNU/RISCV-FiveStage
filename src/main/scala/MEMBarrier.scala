package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class MEMBarrier extends Module {
  val io = IO(new Bundle {
    val InstructionIn    = Input(new Instruction)
    val ALUResultIn      = Input(UInt(32.W))
    val controlSignalsIn = Input(new ControlSignals)
    val DMEMDataIn       = Input(UInt(32.W))
    val BranchTargetIn   = Input(UInt(32.W))
    val BranchTakenIn    = Input(Bool())
    val StageControl     = Input(UInt(2.W))

    val InstructionOut    = Output(new Instruction)
    val ALUResultOut      = Output(UInt(32.W))
    val controlSignalsOut = Output(new ControlSignals)
    val DMEMDataOut       = Output(UInt(32.W))
    val BranchTargetOut   = Output(UInt(32.W))
    val BranchTakenOut    = Output(Bool())
  })

  val InstructionReg    = RegInit(0.U.asTypeOf(new Instruction))
  val ALUResultReg      = RegInit(0.U(32.W))
  val controlSignalsReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val BranchTargetReg   = RegInit(0.U(32.W))
  val BranchTakenReg    = RegInit(false.B)

  when(io.StageControl != StageControl.stall) {
    when(io.StageControl === StageControl.bubble) {
      InstructionReg    := Instruction.NOP // insert NOP to create a bubble in the pipeline
      controlSignalsReg := ControlSignals.nop
    }.otherwise {
      InstructionReg    := io.InstructionIn
      controlSignalsReg := io.controlSignalsIn
    }
  
    ALUResultReg      := io.ALUResultIn
    BranchTargetReg   := io.BranchTargetIn
    BranchTakenReg    := io.BranchTakenIn
  }

  io.InstructionOut    := InstructionReg
  io.ALUResultOut      := ALUResultReg
  io.controlSignalsOut := controlSignalsReg
  io.BranchTargetOut   := BranchTargetReg
  io.BranchTakenOut    := BranchTakenReg

  io.DMEMDataOut       := io.DMEMDataIn // pass through, retrieving from memory takes a cycle
}
