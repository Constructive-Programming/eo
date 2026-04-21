package eo

import optics.Optic
import io.circe.{Codec, Decoder, HCursor, Json}

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

  // Work-in-progress stub — body is `???` pending migration into a demo
  // spec (see docs/plans/2026-04-17-001-feat-production-readiness-laws-docs-plan.md).
  // The unused params reflect the eventual API shape.
  def ofOptic[S, A: Codec, F[_, _]: ForgetfulFunctor](
      @scala.annotation.unused o: Optic[S, S, A, A, F]
  ): JsonOptic[S, A, F] =
    new JsonOptic[S, A, F]:
      def to: S => F[HCursor, A] = ???
      def from: F[HCursor, Json] => Json = ???
