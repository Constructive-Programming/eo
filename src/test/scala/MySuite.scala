package eo

import org.specs2.Specification

class OpticsSpecs extends Specification {
  override def is =
    s2"""
      Iso composed with Iso is Iso in $isoCiso
      """

  type I2 = (Int, Int)

  type S4 = (Short, Short, Short, Short)

  val i2IsoLong = Iso[I2, Long] {

  }

  val s4IsoLong = Iso[S4, Long] {

  }

  def isoCiso = true

}
