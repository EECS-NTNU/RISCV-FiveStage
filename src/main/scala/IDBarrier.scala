package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule

class IDBarrier extends MultiIOModule {
  val io = IO(
    new Bundle {
      // in
      val InstructionIn   = Input(new Instruction)
      val readData1In     = Input(UInt(32.W))
      val readData2In     = Input(UInt(32.W))
      val controlSignalsIn= Input(new ControlSignals)
      val branchTypeIn    = Input(UInt(3.W))
      val op1SelectIn     = Input(UInt(1.W))
      val op2SelectIn     = Input(UInt(1.W))
      val immTypeIn       = Input(UInt(3.W))
      val ALUopIn         = Input(UInt(4.W))
      val PCIn = Input(UInt(32.W))
      val StageControl     = Input(UInt(2.W))

      // out
      val InstructionOut   = Output(new Instruction)
      val readData1Out     = Output(UInt(32.W))
      val readData2Out     = Output(UInt(32.W))
      val controlSignalsOut= Output(new ControlSignals)
      val branchTypeOut    = Output(UInt(3.W))
      val op1SelectOut     = Output(UInt(1.W))
      val op2SelectOut     = Output(UInt(1.W))
      val immTypeOut       = Output(UInt(3.W))
      val ALUopOut         = Output(UInt(4.W))
      val PCOut = Output(UInt(32.W))
    }
  )

  // pipeline registers
  val InstructionReg    = RegInit(0.U.asTypeOf(new Instruction))
  val readData1Reg      = RegInit(0.U(32.W))
  val readData2Reg      = RegInit(0.U(32.W))
  val controlSignalsReg = RegInit(0.U.asTypeOf(new ControlSignals))
  val branchTypeReg     = RegInit(0.U(3.W))
  val op1SelectReg      = RegInit(0.U(1.W))
  val op2SelectReg      = RegInit(0.U(1.W))
  val immTypeReg        = RegInit(0.U(3.W))
  val ALUopReg          = RegInit(0.U(4.W))
  val PCReg             = RegInit(0.U(32.W))

  // connections
  when(io.StageControl != StageControl.stall) {
    when(io.StageControl === StageControl.bubble) {
      InstructionReg    := Instruction.NOP // insert NOP to create a bubble in the pipeline
      controlSignalsReg := ControlSignals.nop
    }.otherwise {
      InstructionReg    := io.InstructionIn
      controlSignalsReg := io.controlSignalsIn
    }
    readData1Reg      := io.readData1In
    readData2Reg      := io.readData2In
    branchTypeReg     := io.branchTypeIn
    op1SelectReg      := io.op1SelectIn
    op2SelectReg      := io.op2SelectIn
    immTypeReg        := io.immTypeIn
    ALUopReg          := io.ALUopIn
    PCReg             := io.PCIn
  }

  io.InstructionOut   := InstructionReg
  io.readData1Out     := readData1Reg
  io.readData2Out     := readData2Reg
  io.controlSignalsOut:= controlSignalsReg
  io.branchTypeOut    := branchTypeReg
  io.op1SelectOut     := op1SelectReg
  io.op2SelectOut     := op2SelectReg
  io.immTypeOut       := immTypeReg
  io.ALUopOut         := ALUopReg
  io.PCOut            := PCReg
}
