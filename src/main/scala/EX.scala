package FiveStage
import chisel3._
import chisel3.experimental.MultiIOModule
import chisel3.util.{ Fill, Cat }

class Execute extends MultiIOModule {

  val io = IO(
    new Bundle {
      val Instruction       = Input(new Instruction)
      val controlSignals    = Input(new ControlSignals)

      val readData1 = Input(UInt(32.W))
      val readData2 = Input(UInt(32.W))

      val PC = Input(UInt(32.W))

      val op1Select      = Input(UInt(1.W))
      val op2Select      = Input(UInt(1.W))
      val immType        = Input(UInt(3.W))
      val ALUop          = Input(UInt(4.W))

      val ALUResult      = Output(UInt(32.W))
      val BranchTarget   = Output(UInt(32.W))
    }
  )

  import Op1Select._
  import Op2Select._
  import ImmFormat._

  val ALU = Module(new ArithmeticUnit).io

  ALU.ALUop := io.ALUop 
  io.ALUResult := ALU.ALUResult

  val imm = Wire(UInt(32.W))

  // might be a good idea to move this to the ID stage
  when(io.immType === ImmFormat.ITYPE) {
    imm := Cat(Fill(20, io.Instruction.immediateIType(11.U)), io.Instruction.immediateIType)
  }.elsewhen(io.immType === ImmFormat.STYPE) {
    imm := Cat(Fill(20, io.Instruction.immediateSType(11.U)), io.Instruction.immediateSType)
  }.elsewhen(io.immType === ImmFormat.JTYPE) {
    imm := Cat(Fill(11, io.Instruction.immediateSType(20.U)), io.Instruction.immediateJType)
  }.elsewhen(io.immType === ImmFormat.UTYPE) {
    imm := io.Instruction.immediateUType.asUInt
  }.otherwise {
    imm := 0.U
  }

  /*
   * Branch target calculation
   */
  when(io.controlSignals.jump && io.immType === ImmFormat.ITYPE) {
    // JALR
    io.BranchTarget := (io.readData1 + imm) // and with boi
  }.elsewhen(io.controlSignals.jump || io.controlSignals.branch) {
    // JAL or branch
    io.BranchTarget := io.PC + imm
  }.otherwise {
    // not a branch or jump
    io.BranchTarget := 0.U
  }

  /*
   * ALU
   */

  // operation 1 select
  when(io.op1Select === Op1Select.rs1) {
    ALU.op1 := io.readData1
  }.elsewhen(io.op1Select === Op1Select.PC) { // for jump and link
    ALU.op1 := io.PC
  }.otherwise {
    ALU.op1 := 0.U
  }

  // operation 2 select
  when(io.op2Select === Op2Select.DC && io.controlSignals.jump) { // increment PC
    ALU.op2 := 4.U
  }.elsewhen(io.op2Select === Op2Select.imm) {
    ALU.op2 := imm
  }.elsewhen(io.op2Select === Op2Select.rs2) {
    ALU.op2 := io.readData2
  }.otherwise {
    ALU.op2 := 0.U
  }

  // debug
  KeepSignal(io.BranchTarget)
  KeepSignal(io.immType)
  KeepSignal(io.PC)
} 
  
