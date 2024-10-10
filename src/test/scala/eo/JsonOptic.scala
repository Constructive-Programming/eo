package eo

import optics.Optic
import io.circe.{Codec, Json, HCursor, Decoder}

case class JsonF[A, B](cursor: HCursor, res: Decoder.Result[B])

abstract class JsonOptic[S, A: Codec, F[_, _]: ForgetfulFunctor] extends Optic[S, Json, A, Json, F]:
  type X = HCursor
  given pass: (Json => F[X, A]) with
    def apply(j: Json): F[X, A] = ???
  def transform[D](f: A => A): Json => Json =
    Optic.transform(this)(f.andThen(Codec[A].apply))


object JsonOptic:
  def fromCodec[A](codec: Codec[A]): Optic[Json, HCursor, A, A, JsonF] =
    new Optic[Json, HCursor, A, A, JsonF]:
      type X = HCursor
      def to: Json => JsonF[X, A] =
        js => JsonF(js.hcursor, codec.decodeJson(js))
      def from: JsonF[X, A] => HCursor =
        _.cursor

  def ofOptic[S: Codec, A: Codec, F[_, _]: ForgetfulFunctor](o: Optic[S, S, A, A, F]):
      JsonOptic[S, A, F] =
    new JsonOptic[S, A, F]:
      def to: S => F[HCursor, A] = ???
      def from: F[HCursor, Json] => Json = ???
