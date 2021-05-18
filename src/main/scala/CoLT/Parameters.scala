package CoLT

import chisel3._
import chisel3.util._

class Params extends Bundle{
    val pMemSize = 128
    val pMemAddressWidth = log2Ceil(pMemSize)

    val vMemSize = 256
    val vMemAddressWidth: Int = log2Ceil(vMemSize)

    val cacheLineSize = 4 // Number of pages to be fetched from a PTW
    val ptSize = 128  // 2^7
    val pageSize = 32 //bits
    val pageOffset = log2Ceil(pageSize)

    val dataWidth = 32

    val tlbSize = 8
    val coalBits = 3
    val attrBits = 3
    val vpn_width=vMemAddressWidth - pageOffset
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
}
