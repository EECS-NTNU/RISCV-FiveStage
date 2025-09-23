package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class EXBarrier extends MultiIOModule {
  val io = IO(new Bundle {
    val InstructionIn    = Input(new Instruction)
    val ALUResultIn      = Input(UInt(32.W))
    val controlSignalsIn = Input(new ControlSignals)
    val readData2In      = Input(UInt(32.W))
    val BranchTargetIn   = Input(UInt(32.W))
    val BranchTakenIn    = Input(Bool())
    val StageControl     = Input(UInt(2.W))
    
    val InstructionOut    = Output(new Instruction)
    val ALUResultOut      = Output(UInt(32.W))
    val controlSignalsOut = Output(new ControlSignals)
    val readData2Out      = Output(UInt(32.W))
    val BranchTargetOut   = Output(UInt(32.W))
    val BranchTakenOut    = Output(Bool())
  })

  val InstructionReg    = RegInit(0.U.asTypeOf(new Instruction))
  val ALUResultReg      = RegInit(0.U(32.W))
  val controlSignalsReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val readData2Reg      = RegInit(0.U(32.W))
  val BranchTargetReg   = RegInit(0.U(32.W))
  val BranchTakenReg    = RegInit(false.B)

  when(io.StageControl != StageControl.stall) {
    when(io.StageControl === StageControl.bubble) {
      InstructionReg    := Instruction.NOP // insert NOP to create a bubble in the pipeline
    }.otherwise {
      InstructionReg    := io.InstructionIn
    }
    ALUResultReg      := io.ALUResultIn
    controlSignalsReg := io.controlSignalsIn
    readData2Reg      := io.readData2In
    BranchTargetReg   := io.BranchTargetIn
    BranchTakenReg    := io.BranchTakenIn
  }


  io.InstructionOut    := InstructionReg
  io.ALUResultOut      := ALUResultReg
  io.controlSignalsOut := controlSignalsReg
  io.readData2Out      := readData2Reg
  io.BranchTargetOut   := BranchTargetReg
  io.BranchTakenOut    := BranchTakenReg
}
