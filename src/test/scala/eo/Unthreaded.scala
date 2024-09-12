package eo

import optics.*
import data.Forget

import scodec.Codec
import scodec.bits.BitVector

import scala.collection.immutable.ArraySeq
import cats.instances.arraySeq.*

case class Person(id: Int, name: String, phones: ArraySeq[Phone])
case class Phone(isMobile: Boolean, number: String)

@main def unthreaded =
  import eo.data.Forgetful.given

  val personPhone =
    Lens[Person, ArraySeq[Phone]](_.phones, (s, b) => s.copy(phones = b))
      .morph[Forget[ArraySeq]]
      .andThen[Phone, Phone](Traversal.each[ArraySeq, Phone])

  val phoneIsMobile = Lens[Phone, Boolean](_.isMobile, (s, b) => s.copy(b))

  val personCodec = Prism[Person, BitVector](Codec.encode.andthen(_.require), Codec.decode)
  val phoneCodec = Prism[Person, BitVector](Codec.encode.andthen(_.require), Codec.decode)

  val personPipeline: BitVector => BitVector = ???
  val phonePipeline: BitVector => BitVector = ???

  val personPhonesAreMobile: Optic[Person, Person, Boolean, Boolean, Forget[ArraySeq]] = ???
  val personPlaceIsMobile: Optic[Person, BitVector, Boolean, Boolean, Forget[ArraySeq]] = ???

  val Bob = Person(1, "John", ArraySeq(Phone(true, "+310655940946")))
  val Alice = Person(1, "John", ArraySeq(Phone(false, "+88 55940946")))

  val BobBits: BitVector = personCodec.get(Bob)
  val AliceBits: BitVector = personCodec.get(Bob)
