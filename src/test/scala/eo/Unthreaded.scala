package eo

import optics.*
import data.{Forget, PowerSeries}

import io.circe.{Codec, Json}

import scala.collection.immutable.ArraySeq
import cats.instances.arraySeq.*
import io.circe.Decoder
import io.circe.Encoder
import scala.reflect.ClassTag

case class Person(id: Int, name: String, phones: ArraySeq[Phone])
case class Phone(isMobile: Boolean, number: String)

@main def unthreaded =
  import eo.data.PowerSeries.given
  import Optic.*

  given phoneCodec: Codec[Phone] =
    Codec.derived[Phone]
  given arraySeqCodec[A: Codec: ClassTag]: Codec[ArraySeq[A]] =
    Codec.from(Decoder.decodeArraySeq[A], Encoder.encodeIterable)
  given personCodec: Codec[Person] =
    Codec.derived[Person]
  given isMobleCodec: Codec[Boolean] =
    Codec.from(Decoder.decodeBoolean, Encoder.encodeBoolean)

  val personPhone =
    Lens[Person, ArraySeq[Phone]](_.phones, (s, b) => s.copy(phones = b))
      .morph[PowerSeries]
      .andThen[Phone, Phone](Traversal.each[ArraySeq, Phone])

  val phoneIsMobile = Lens[Phone, Boolean](_.isMobile, (s, b) => s.copy(b))

  val personAllMobiles = personPhone
    .andThen(phoneIsMobile.morph[PowerSeries])

  val passthrough = JsonOptic.ofOptic(personAllMobiles)

  val Bob = Person(1, "John", ArraySeq(Phone(true, "+310655940946")))
  val Alice = Person(1, "John", ArraySeq(Phone(false, "+88 55940946")))

  val nBob = personAllMobiles
    .replace(false)(Bob)
  println(s"Bob was: $Bob\nNow he's: $nBob")

  val jBob = passthrough.transform((isMobile: Boolean) => !isMobile)
