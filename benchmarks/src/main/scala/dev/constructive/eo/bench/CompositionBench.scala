package dev.constructive.eo
package bench

import fixture.*

/** Cross-carrier `.andThen` composition cost — EO's signature feature (plan 009, Phase 2).
  *
  * Two axes, on the shared [[fixture.Nested]] chain:
  *   - **build** — constructing a depth-N composed optic from its hops (`a.andThen(b).andThen…`).
  *     The result is returned so JMH consumes it; this isolates the per-`andThen` allocation +
  *     `Composer` dispatch cost.
  *   - **reuse** — `.modify` through a pre-built depth-N optic (the steady-state cost once the
  *     optic is a constant).
  *
  * Two carrier shapes:
  *   - **same-carrier** `Lens.andThen(Lens)` (`Tuple2`), depths 3 and 6.
  *   - **cross-carrier** `Lens…andThen(Optional)` — the `Tuple2` Lens chain is lifted into the
  *     `Affine` carrier by an automatic `Morph[Tuple2, Affine]`, no explicit `.morph` at the call
  *     site. This is the composition EO does that a fixed-carrier optic library can't.
  */
class CompositionBench extends JmhDefaults:

  import NestedOptics.{d3, d6, eoFlag, eoN1, eoN2, eoN3, eoN4, eoN5, eoN6, leaf}

  // Leaf Lens on Nested0.value (NestedOptics ships a Getter / Modify for it,
  // but composition needs a full Lens).
  private val leafValue =
    EoLens[Nested0, Int](_.value, (n, v) => n.copy(value = v))

  // Pre-built composed optics for the reuse axis.
  private val lens3 = eoN3.andThen(eoN2).andThen(eoN1).andThen(leafValue)

  private val lens6 =
    eoN6.andThen(eoN5).andThen(eoN4).andThen(eoN3).andThen(eoN2).andThen(eoN1).andThen(leafValue)

  private val opt3 = eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlag)

  // ---- same-carrier Lens.andThen(Lens) (Tuple2) ---------------------

  @Benchmark def buildLens3: Any =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(leafValue)

  @Benchmark def buildLens6: Any =
    eoN6.andThen(eoN5).andThen(eoN4).andThen(eoN3).andThen(eoN2).andThen(eoN1).andThen(leafValue)

  @Benchmark def reuseLens3: Nested3 = lens3.modify(_ + 1)(d3)
  @Benchmark def reuseLens6: Nested6 = lens6.modify(_ + 1)(d6)

  // ---- cross-carrier Lens…andThen(Optional) (Tuple2 → Affine) -------

  @Benchmark def buildLensOptional3: Any =
    eoN3.andThen(eoN2).andThen(eoN1).andThen(eoFlag)

  @Benchmark def reuseLensOptional3: Nested3 = opt3.modify(_.toUpperCase)(d3)

  // ---- depth-1 reference + uncomposed leaf floor --------------------

  private val lens1 = eoN1.andThen(leafValue)

  @Benchmark def buildLens1: Any = eoN1.andThen(leafValue)
  @Benchmark def reuseLens1: Nested1 = lens1.modify(_ + 1)(Nested.Default1)

  // Uncomposed leaf Lens — the floor (no `.andThen` at all).
  @Benchmark def reuseLeaf: Nested0 = leafValue.modify(_ + 1)(leaf)

  // ---- untyped-tree kit shape: field (Optional) ∘ scalar (PickMendPrism) ----
  // `kitFused` is the concrete Optional ∘ PickMendPrism overload; `kitGeneric` the Morph route the
  // kits took before it existed; `kitHand` the hand-written match.
  import KitTree.*
  private val kitFused = name.andThen(str)

  private val kitGeneric =
    (name: optics.Optic[V, V, V, V, data.Affine])
      .andThen(str: optics.Optic[V, V, String, String, Either])

  @Benchmark def kitFusedRead: Option[String] = kitFused.getOption(doc)
  @Benchmark def kitGenericRead: Option[String] = kitGeneric.getOption(doc)
  @Benchmark def kitFusedModify: V = kitFused.modify(_.toUpperCase)(doc)
  @Benchmark def kitGenericModify: V = kitGeneric.modify(_.toUpperCase)(doc)
  @Benchmark def kitHandModify: V = handShout(doc)

/** A minimal untyped tree, shaped like the zio-json / kyo / spark-variant kits' values. */
object KitTree:

  enum V:
    case Obj(fields: Map[String, V])
    case Str(s: String)

  val doc: V = V.Obj(Map("name" -> V.Str("ada"), "role" -> V.Str("eng")))

  val name = dev
    .constructive
    .eo
    .optics
    .Optional[V, V, V, V](
      {
        case v @ V.Obj(fs) => fs.get("name").toRight(v)
        case v             => Left(v)
      },
      {
        case (V.Obj(fs), x) => V.Obj(fs.updated("name", x))
        case (v, _)         => v
      },
    )

  val str = dev
    .constructive
    .eo
    .optics
    .Prism
    .optional[V, String](
      {
        case V.Str(s) => Some(s)
        case _        => None
      },
      V.Str(_),
    )

  def handShout(v: V): V = v match
    case V.Obj(fs) =>
      fs.get("name") match
        case Some(V.Str(s)) => V.Obj(fs.updated("name", V.Str(s.toUpperCase)))
        case _              => v
    case _ => v
