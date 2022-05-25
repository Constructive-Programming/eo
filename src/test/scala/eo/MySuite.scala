package eo

import optics.Iso

import org.specs2.Specification

class OpticsSpecs extends Specification {
  override def is =
    s2"""
      Iso composed with Iso is Iso in $isoCiso
      """

  type I2 = (Int, Int)

  type S4 = (Short, Short, Short, Short)

  val i2IsoLong = Iso[I2, I2, Long, Long]({ case (a, b) => (a.toLong << 32) | (b.toLong & 0xffffffffL)},
  l => (l >> 32).toInt -> l.toInt)

  val s4IsoLong = Iso[S4, S4, Long, Long]

  def isoCiso = true

}
