package dev.constructive.eo.circe

import org.specs2.mutable.Specification

/** Shared base trait for the circe-test specs.
  *
  * '''2026-04-26 dedup.''' Six spec files used to spell the same import block at the top:
  *
  * {{{
  *   import cats.data.{Chain, Ior}
  *   import hearth.kindlings.circederivation.KindlingsCodecAsObject
  *   import io.circe.syntax.*
  *   import io.circe.{Codec, Json}
  *   import org.specs2.mutable.Specification
  * }}}
  *
  * Re-exporting the type-level names through this trait brings them into every subclass's scope
  * automatically. Spec bodies that want `cats.data.Chain` write it as just `Chain` (lifted via the
  * trait member `type Chain[A] = cats.data.Chain[A]` plus the `val Chain` re-export).
  *
  * Note: extension methods (e.g. `io.circe.syntax.EncoderOps.asJson`) STILL need a per-file
  * `import io.circe.syntax.*` — Scala 3's `export` doesn't propagate extension-method scope across
  * compilation units the way a wildcard import does.
  */
trait JsonSpecBase extends Specification:

  type Chain[A] = cats.data.Chain[A]
  val Chain: cats.data.Chain.type = cats.data.Chain

  type Ior[+A, +B] = cats.data.Ior[A, B]
  val Ior: cats.data.Ior.type = cats.data.Ior

  type Codec[A] = io.circe.Codec[A]
  val Codec: io.circe.Codec.type = io.circe.Codec

  type Json = io.circe.Json
  val Json: io.circe.Json.type = io.circe.Json

  val KindlingsCodecAsObject: hearth.kindlings.circederivation.KindlingsCodecAsObject.type =
    hearth.kindlings.circederivation.KindlingsCodecAsObject
