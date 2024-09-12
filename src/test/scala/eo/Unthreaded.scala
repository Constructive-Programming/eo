package eo

import optics.*

import scodec.Codec
import scodec.implicits.*
import scodec.bits.BitVector

case class Person(id: Int, name: String, phones: Array[Phone])
case class Phone(isMobile: Boolean, number: String)

@main def unthreaded =
  val personId = Lens[Person, Int](_.id, (s, b) => s.copy(b))
  val phoneIsMobile = Lens[Phone, Boolean](_.isMobile, (s, b) => s.copy(b))

  val personCodec = Prism[Person, BitVector](Codec.encode.andthen(_.require), Codec.decode)
  val phoneCodec = Prism[Person, BitVector](Codec.encode.andthen(_.require), Codec.decode)
