package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class IFBarrier extends MultiIOModule {
  val io = IO(
    new Bundle {
      val InstructionIn = Input(new Instruction)
      val PCIn = Input(UInt(32.W))
      val StageControl     = Input(UInt(2.W))
      val InstructionOut = Output(new Instruction)
      val PCOut = Output(UInt(32.W))
    }
  )

  // delay registers
  val PCReg = RegInit(UInt(32.W), 0.U)
  val InstructionReg    = RegInit(0.U.asTypeOf(new Instruction))

  val lastCycleStall = RegInit(false.B)
  lastCycleStall := io.StageControl === StageControl.stall

  when(lastCycleStall) {
    io.InstructionOut := InstructionReg
  }.otherwise {
    io.InstructionOut := io.InstructionIn
  }

  when(io.StageControl === StageControl.stall) {
    InstructionReg := io.InstructionIn // if stalled we save the current instruction
  }.otherwise {
    PCReg := io.PCIn
  }
  io.PCOut := PCReg

  KeepSignal(io.PCOut)
}
