package dev.constructive.eo
package jsoniter

/** jsoniter-scala's whole-document entry points, which the specs use to build their fixtures. */
export com.github.plokhotnyuk.jsoniter_scala.core.{readFromArray, writeToArray}

/** The leaf codecs every jsoniter spec reads through, declared once: a spec brings them into scope
  * with `import SpecCodecs.given` and declares only the codecs of its own fixture types.
  */
object SpecCodecs:
  given JsoniterCodec[Int] = JsoniterCodec.derived
  given JsoniterCodec[Long] = JsoniterCodec.derived
  given JsoniterCodec[Double] = JsoniterCodec.derived
  given JsoniterCodec[String] = JsoniterCodec.derived
