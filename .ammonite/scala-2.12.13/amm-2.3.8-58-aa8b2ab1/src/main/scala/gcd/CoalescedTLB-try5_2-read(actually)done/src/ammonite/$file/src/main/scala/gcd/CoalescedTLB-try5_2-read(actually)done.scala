
package ammonite
package $file.src.main.scala.gcd
import _root_.ammonite.interp.api.InterpBridge.{
  value => interp
}
import _root_.ammonite.interp.api.InterpBridge.value.{
  exit
}
import _root_.ammonite.interp.api.IvyConstructor.{
  ArtifactIdExt,
  GroupIdExt
}
import _root_.ammonite.compiler.CompilerExtensions.{
  CompilerInterpAPIExtensions,
  CompilerReplAPIExtensions
}
import _root_.ammonite.runtime.tools.{
  browse,
  grep,
  time,
  tail
}
import _root_.ammonite.compiler.tools.{
  desugar,
  source
}
import _root_.mainargs.{
  arg,
  main
}
import _root_.ammonite.repl.tools.Util.{
  PathRead
}


object `CoalescedTLB-try5_2-read(actually)done`{
/*<script>*/val path = System.getProperty("user.dir") + "/source/load-ivy.sc"
/*<amm>*/val res_1 = /*</amm>*/interp.load.module(ammonite.ops.Path(java.nio.file.FileSystems.getDefault().getPath(path)))

import chisel3._
import chisel3.util._
import chisel3.tester._
import chisel3.tester.RawTester.test

// Parameters

val pMemSize = 128
val pMemAddressWidth = log2Ceil(pMemSize)

val vMemSize = 256
val vMemAddressWidth = log2Ceil(vMemSize)

val cacheLineSize = 4 // Number of pages to be fetched from a PTW
val ptSize = 128  // 2^7
val pageSize = 32 //bits
val pageOffset = log2Ceil(pageSize)

val dataWidth = 32

val tlbSize = 8
val coalBits = 3
val attrBits = 3
val vpn_width=vMemAddressWidth - pageOffsetgfdgrd
val ppn_width=pMemAddressWidth - pageOffset

val tlbEntryWidth = vpn_width + coalBits + attrBits + ppn_width


// ========= CoLT-FA TLB entry structure =========
// BaseVPN [vMemAddressWidth - pageOffset] | CoalLength[coalBits] | Attributes [attrBits] | Base PPN [pMemAddressWidth - pageOffset]
val vpn_tlb_start        =tlbEntryWidth-1
val vpn_tlb_end          =vpn_tlb_start-vpn_width+1

val coal_length_start    =vpn_tlb_end-1
val coal_length_end      =coal_length_start-coalBits+1

val attributes_start     =coal_length_end-1
val attributes_end       =attributes_start-attrBits+1

val ppn_tlb_start        =attributes_end-1
val ppn_tlb_end          =ppn_tlb_start-ppn_width+1



def randomUInt(upperLimit: Int)= scala.util.Random.nextInt(upperLimit).U

def getVPNfromTLB (entry: UInt):UInt={
    entry(vpn_tlb_start, vpn_tlb_end)
}
def getVPNfromVA (entry: UInt):UInt={
    entry(vMemAddressWidth-1, vMemAddressWidth-vpn_width)
}
def getCoalLengthFromTLB (entry: UInt):UInt={
    entry(coal_length_start, coal_length_end)
}
def getAttrFromTLB (entry: UInt):UInt={
    entry(attributes_start, attributes_end)
}
def getPPNfromTLB (entry: UInt):UInt={
    entry(ppn_tlb_start, ppn_tlb_end)
}
def getPPNfromPA (entry: UInt):UInt={
    entry(pMemAddressWidth-1, pMemAddressWidth-ppn_width)
}


class CoLT_FA extends Module {
    val io = IO(new Bundle {
        val readAddress = Input (UInt(vMemAddressWidth.W))  // Determines the requested VIRTUAL address
        val readEnable = Input(Bool()) // Determines whether or not read operation is allowed
        
        // Incoming write operations indicate that a page table walk has been conducted
        // The "write Address" field indicates the page that was found
        // and brought into the CoLT-FA TLB to be stored
        val writeAddress = Input (UInt((cacheLineSize*pMemAddressWidth).W)) 
        val writeEnable = Input(Bool())
        val writeData = Input (UInt(dataWidth.W)) //maybe useless
        
        val retData = Output (UInt(dataWidth.W))  // Provides the requested TLB entry
        val validData = Output (Bool())   // Ensures that the TLB entry provided to the consumer
                                          // is valid (it was found among the entries)
                                          // and it's not just the previous return address. Check lookup.
        val retAddress = Output (UInt(ppn_width.W)) //Returns the desired PPN
    })
    
    val previousRetAddressReg = RegNext(io.retAddress, 0.U(ppn_width.W))
    val reqVPN=Reg(UInt(vpn_width.W))
    val resultIndexReg = Reg(UInt(log2Ceil(tlbSize).W))
    val foundReg = RegInit(false.B)
    
    
    val pMem = SyncReadMem(pMemSize, UInt(dataWidth.W))
    //val coltEntriesRegs = RegInit(VecInit(Seq.fill(tlbSize)(0.U(tlbEntryWidth.W))))
    
    // ========= NOOOOOOOOB =================
    val coltEntriesRegs = RegInit(VecInit(259.U(tlbEntryWidth.W),
                                          545.U(tlbEntryWidth.W),
                                          0.U(tlbEntryWidth.W),
                                          0.U(tlbEntryWidth.W),
                                          1058.U(tlbEntryWidth.W),
                                          1538.U(tlbEntryWidth.W),
                                          0.U(tlbEntryWidth.W),
                                          0.U(tlbEntryWidth.W)))
    // ============== END OF NOOOOOOOOOOB ===================

    
    when (io.readEnable) {
        reqVPN := getVPNfromVA(io.readAddress)
        //=================== Range check logic ===================
        foundReg := coltEntriesRegs.exists{case x => (getVPNfromTLB(x) <= getVPNfromVA(io.readAddress)) && (getVPNfromVA(io.readAddress)<= getVPNfromTLB(x) + getCoalLengthFromTLB(x))}
        // "foundReg" is set if the requested address matches the range check logic
        
        resultIndexReg := coltEntriesRegs.indexWhere {case x => (getVPNfromTLB(x) <= getVPNfromVA(io.readAddress)) && (getVPNfromVA(io.readAddress)<= getVPNfromTLB(x) + getCoalLengthFromTLB(x))}
        // "resultIndexReg" stores the index of the TLB entry that matched the request
        // If the requested TLB entry was not found (TLB miss), then "result" value is 0.U
        
        //=================== PPN Generation Logic ===================
        val finalRes = getVPNfromVA(io.readAddress) - getVPNfromTLB(coltEntriesRegs(resultIndexReg)) + getPPNfromTLB(coltEntriesRegs(resultIndexReg))
        
        // Update-return operations
        io.retAddress := Mux(foundReg, finalRes, previousRetAddressReg)
        io.validData := foundReg
    }
    .elsewhen (io.writeEnable) { 
        /*
        val ppnVec = Reg(Vec(cacheLineSize,UInt(PPN_WIDTH.W)))
        // Spit PTE entries and extract PPN
        for (i<-0 until cacheLineSize){
            ppnVec.updated(i,getPPNfromPA(io.writeAddress(i*pMemAddressWidth+pMemAddressWidth-1,i*pMemAddressWidth)))
        }
        // At this point we have to check if reqVPN, reqVPN+1,..., correspond to consecutive PPNs
        // To do so we must traslate each VPN seperately
        */
        
        // ======= This is a NOOB write implementation just to test reads =======
        //var idx=randomUInt(tlbSize)
        //coltEntriesRegs(io.writeData)= io.writeAddress
        //coltEntriesRegs.updated(idx, io.writeAddress)
        //printf(s"Address ${io.writeAddress} was written. Index=$idx\n")
        // ============= END OF NOOB IMPLEMENTATION =============================
        
        io.retAddress := previousRetAddressReg
        io.validData:= false.B
    }
    .otherwise { 
        io.retAddress:=previousRetAddressReg
        io.validData:=false.B
    }
    io.retData:= 0.U
}



/*<amm>*/val res_37 = /*</amm>*/test(new CoLT_FA()) { c => 
/*
    // Read non-existent entry
    c.io.readEnable.poke(true.B)
    c.io.readAddress.poke(76.U)
    c.clock.step(2)
    c.io.validData.expect(false.B)
    c.io.retAddress.expect (0.U)
    
    
    // Write address 12 address at 3
    c.io.readEnable.poke(false.B)
    c.io.writeEnable.poke(true.B)
    c.io.writeAddress.poke(12.U)
    c.io.writeData.poke(3.U)
    c.clock.step(2)
    c.io.retAddress.expect (0.U)
    */
    
    // Read non-existent entry
    c.io.readEnable.poke(true.B)
    c.io.readAddress.poke(37.U)
    c.clock.step(3)
    c.io.retAddress.expect(3.U)
    c.io.validData.expect(true.B)
        
    c.io.readEnable.poke(true.B)
    c.io.readAddress.poke(91.U)
    c.clock.step(3)
    c.io.validData.expect(true.B)
    c.io.retAddress.expect(1.U)
    
    c.io.readEnable.poke(true.B)
    c.io.readAddress.poke(99.U)
    c.clock.step(3)
    c.io.validData.expect(true.B)
    c.io.retAddress.expect(2.U)   
    
    println("Success!")
}

/*    
    
    val virtMem = new VirtualMemory(vMemSize)
    val vMem = virtMem.data
    vMem.map(_.fillRandom)
    
    val pt = Module(new pageTable (ptSize))
    pt.data.map(_.fillRandom)
    
    val colt = Vec (tlbSize, new TLBentry(randomUInt(vMemSize),randomUInt(vMemSize),true.B,0.U))
 
    // Steps 1,2: Assume L1 and L2 TLB Misses
    println ("Steps 1,2: L1 and L2 TLB Misses")
    
    // Step 3: Initiate page table walk
    val target = ptw(pt, reqAddress)
    
    // Fetch a cache line with up to 8 translation. 
    println ("Step 3: Initiating cache line fetch")
    val cacheLine = Vec(cacheLineSize, new PTentry()) //maybe register is required
    for (i <- 0 until cacheLineSize){
        cacheLine:= cacheLine.updated(i,(pt.data(target - target%cacheLineSize + i)))
    }
    
    // Step 4: Check for contiguity and coalesce them if needed
    // contiguity: if the consecutive words (or blocks??) of the cache line i.e. virtual addresses
    // correspond to consecutive entries in PHYSICAL Memory
    
    val diff = cacheLine.map(x => (x.virtual, x.physical)).map{case (a,b) => a-b} 
                                // Needs to be aware of negative differences    // NEEDS TO BE FIXED!  
    val cLength = 0.U   // Maybe needs to be initialized at 1?????
    var i=1
    while (i<cacheLineSize &&(diff(i)==diff(i-1) && (((cacheLine(i).virtual - cacheLine(i-1).virtual).abs == 1.U)))){
        cLength := cLength + 1.U
        i = i+1
    }
    
    // Load the COALESCED entry (if existent) into the TLB
    val entry = new TLBentry(cacheLine(0).virtual,cacheLine(0).physical,true.B,cLength)
    when (cLength =/= 0.U){
        println("Contiguity found.")
        colt(scala.util.Random.nextInt(tlbSize-1)) := entry
        
        // ============================================================== //
        // Above we placed the entry in a random place in the TLB.   
        // Instead, I have to check the LRU and valid bits ------ FIX IT!
        // ============================================================== //
        
        
        // Step 5: Check for further coalescion in the CoLT-TLB
        
    }.otherwise{
        println("Contiguity not found. Entry is loaded into L1/L2 TLB")
    }
   
    
    def lookup (request: UInt){
        var found = false
        var result = 0.U
        var i=0
        while (!found){
            when ((colt(i).virtualBase >= request) && (colt(i).virtualBase +& colt(i).coalLength <= request)){
                found = true   // TLB HIT!
                // PPN Generation Logic
                result = request -& colt(i).virtualBase +& colt(i).physicalBase
            }.otherwise{
                i += 1
            }
        }
        (result, found)
    }
    
}
*/

/*<amm>*/val res_38 = /*</amm>*/println(getVerilog (new CoLT_FA))






/*</script>*/ /*<generated>*/
def $main() = { scala.Iterator[String]() }
  override def toString = "CoalescedTLB$minustry5_2$minusread$u0028actually$u0029done"
  /*</generated>*/
}
