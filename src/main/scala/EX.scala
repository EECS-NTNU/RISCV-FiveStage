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

      val DMEMforwardedMEM  = Input(UInt(32.W)) // direct output of the data memory, used after stall
      val ALUforwardedMEM   = Input(UInt(32.W))
      val DMEMforwardedWB   = Input(UInt(32.W))
      val ALUforwardedWB    = Input(UInt(32.W))
      val lastWriteData     = Input(UInt(32.W))
      val lastWriteDest     = Input(UInt(5.W))

      val MEMInstruction = Input(new Instruction)
      val WBInstruction  = Input(new Instruction)

      val MEMControlSignals = Input(new ControlSignals)
      val WBControlSignals = Input(new ControlSignals)

      val PC = Input(UInt(32.W))

      val op1Select      = Input(UInt(1.W))
      val op2Select      = Input(UInt(1.W))
      val immType        = Input(UInt(3.W))
      val branchType     = Input(UInt(3.W))
      val ALUop          = Input(UInt(4.W))
      val DMEMData       = Output(UInt(32.W)) // write data for the DMEM

      val ALUResult       = Output(UInt(32.W))
      val BranchTarget    = Output(UInt(32.W))
      val BranchTaken     = Output(Bool())
      val PipelineControl = Output(new PipelineControl)
    }
  )

  import Op1Select._
  import Op2Select._
  import StageControl._
  import ImmFormat._
  import branchType._

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
  }.elsewhen(io.immType === ImmFormat.BTYPE) {
    imm := Cat(Fill(20, io.Instruction.immediateBType(11.U)), io.Instruction.immediateBType)
  }.otherwise {
    imm := 0.U
  }

  /*
   * Branch taken?
   */
  val BranchTaken = Wire(Bool())
  io.BranchTaken := BranchTaken

  // calculate if branch taken based on ALU output

  when(!io.controlSignals.branch) {
    BranchTaken := false.B
  }.otherwise {
    // a branch bro
    when(io.branchType === beq) {
      BranchTaken := ALU.ALUResult === 0.U
    }.elsewhen(io.branchType === neq) {
      BranchTaken := ALU.ALUResult != 0.U
    }.elsewhen(io.branchType === gte) {
      BranchTaken := ALU.ALUResult.asSInt >= 0.S
    }.elsewhen(io.branchType === lt) {
      BranchTaken := ALU.ALUResult.asSInt < 0.S
    }.elsewhen(io.branchType === gteu) {
      BranchTaken := ALU.ALUResult >= 0.U // this will not work?
    }.elsewhen(io.branchType === ltu) {
      BranchTaken := ALU.ALUResult < 0.U // this will not work?
    }.otherwise { // will not happen
      BranchTaken := false.B
    }
  }

  /*
   * Branch target calculation
   */
  when(io.controlSignals.jump && io.immType === ImmFormat.ITYPE) {
    // JALR
    io.BranchTarget := (io.readData1 + imm) & "h_ffff_fffe".U
  }.elsewhen(io.controlSignals.jump || io.controlSignals.branch) {
    // JAL or branch
    io.BranchTarget := io.PC + imm
  }.otherwise {
    // not a branch or jump
    io.BranchTarget := 0.U
  }

  /*
   * Forwarding unit and pipeline control
   */
  class Forward extends Module {
    val io = IO(
      new Bundle {
        // forwarded values
        val REG  = Input(UInt(32.W))
        val MEM  = Input(UInt(32.W))
        val WB   = Input(UInt(32.W))

        // needed source
        val RsEX  = Input(UInt(5.W))

        // register destinations
        val RdMEM = Input(UInt(5.W))
        val RdWB  = Input(UInt(5.W))

        val lastWriteData     = Input(UInt(32.W))
        val lastWriteDest     = Input(UInt(5.W))

        val sourceNeeded  = Input(Bool())
        val IsLoadMEM     = Input(Bool())

        val regSource   = Output(UInt(32.W))
        val loadForward = Output(Bool())
      }
    )

    io.loadForward := false.B
    when(!io.sourceNeeded) {
      io.regSource   := 0.U
    }.otherwise { // forwarding potentially needed
      when(io.RsEX === io.lastWriteDest && io.RsEX != io.RdMEM && io.RsEX != io.RdWB) { // forward the last written value
        io.regSource := io.lastWriteData
      }.elsewhen(io.RsEX != io.lastWriteDest && io.RsEX != io.RdMEM && io.RsEX != io.RdWB) {  // no need to forward
        io.regSource := io.REG
      }.elsewhen(io.RsEX === io.RdWB && io.RsEX != io.RdMEM) { // forward from WB stage
        io.regSource := io.WB
      }.otherwise { // forward from MEM stage
        io.regSource := io.MEM
        when(io.IsLoadMEM) { // we might need to wait for a load to resolve
          io.loadForward := true.B
        }
      }
    }
  }

  val RegSource1 = Module(new Forward).io
  val RegSource2 = Module(new Forward).io

  // register source 1 select
  RegSource1.REG := io.readData1

  RegSource1.MEM := io.ALUforwardedMEM

  when(io.WBControlSignals.memRead) {
    RegSource1.WB  := io.DMEMforwardedWB
  }.otherwise {
    RegSource1.WB  := io.ALUforwardedWB
  }

  RegSource1.lastWriteData := io.lastWriteData
  RegSource1.lastWriteDest := io.lastWriteDest

  RegSource1.RsEX  := io.Instruction.registerRs1
  RegSource1.RdMEM := io.MEMInstruction.registerRd
  RegSource1.RdWB  := io.WBInstruction.registerRd
  RegSource1.sourceNeeded := io.op1Select === Op1Select.rs1
  RegSource1.IsLoadMEM    := io.MEMControlSignals.memRead

  // register source 2 select
  RegSource2.REG := io.readData2
  RegSource2.MEM := io.ALUforwardedMEM

  when(io.WBControlSignals.memRead) {
    RegSource2.WB  := io.DMEMforwardedWB
  }.otherwise {
    RegSource2.WB  := io.ALUforwardedWB
  }

  RegSource2.lastWriteData := io.lastWriteData
  RegSource2.lastWriteDest := io.lastWriteDest

  RegSource2.RsEX  := io.Instruction.registerRs2
  RegSource2.RdMEM := io.MEMInstruction.registerRd
  RegSource2.RdWB  := io.WBInstruction.registerRd
  RegSource2.sourceNeeded := io.op2Select === Op2Select.rs2
  RegSource2.IsLoadMEM    := io.MEMControlSignals.memRead

  io.DMEMData := RegSource1.regSource // for stores

  // when depending on result from a load in the mem stage we stall for a cycle
  val lastCycleStall = RegInit(false.B) // to avoid deadlock

  // we need to wait for a load to resolve
  lastCycleStall := false.B
  when((RegSource1.loadForward || RegSource2.loadForward) && !lastCycleStall) {
    io.PipelineControl.IF := StageControl.stall
    io.PipelineControl.ID := StageControl.stall
    io.PipelineControl.EX := StageControl.stall
    io.PipelineControl.MEM := StageControl.bubble
    io.PipelineControl.WB := StageControl.run
    lastCycleStall := true.B
  }.otherwise {
    io.PipelineControl.IF := StageControl.run
    io.PipelineControl.ID := StageControl.run
    io.PipelineControl.EX := StageControl.run
    io.PipelineControl.MEM := StageControl.run
    io.PipelineControl.WB := StageControl.run
  }

  /*
   * ALU
   */

  // operation 1 select
  when(io.op1Select === Op1Select.rs1) {
    ALU.op1 := RegSource1.regSource
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
    ALU.op2 := RegSource2.regSource
  }.otherwise {
    ALU.op2 := 0.U
  }

  // debug
  KeepSignal(io.BranchTarget)
  KeepSignal(io.immType)
  KeepSignal(io.PC)
  KeepSignal(io.branchType)
  KeepSignal(io.PipelineControl.IF)
  KeepSignal(io.PipelineControl.ID)
  KeepSignal(io.PipelineControl.EX)
  KeepSignal(io.PipelineControl.MEM)
  KeepSignal(io.PipelineControl.WB)
  KeepSignal(lastCycleStall)
} 
  
