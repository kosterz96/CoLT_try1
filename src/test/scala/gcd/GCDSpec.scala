// See README.md for license details.

package gcd

import chisel3._
import chisel3.tester._
import org.scalatest.FreeSpec
import chisel3.experimental.BundleLiterals._

/**
  * This is a trivial example of how to run this Specification
  * From within sbt use:
  * {{{
  * testOnly gcd.GcdDecoupledTester
  * }}}
  * From a terminal shell use:
  * {{{
  * sbt 'testOnly gcd.GcdDecoupledTester'
  * }}}
  */
class GCDSpec extends FreeSpec with ChiselScalatestTester {

  test(new CoLT_FA()) { c => 
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
}
