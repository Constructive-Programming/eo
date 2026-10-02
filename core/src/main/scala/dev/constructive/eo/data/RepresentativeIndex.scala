package dev.constructive.eo
package data

/** A concrete index value for the `Function1[X0, *]` (Grate) carrier — the witness that makes a
  * bundle read *real* instead of forged.
  *
  * '''Why a witness exists at all.''' A Grate bundle is a function `X0 => A`; reading it needs an
  * `X0`, and no rule of the type system produces one. Exactly one site in the library needs that
  * read: the `from` of the bridge's product (`Function1BroadcastOptic`, `private[eo]`), which must
  * turn a written `MultiFocus[Function1[X0, *]][Unit, B]` back into a `T`, and whose own carrier
  * stores only `Unit`. Every other read in the Grate surface is *handed* its index by the caller
  * (`to(s)`, the `.at(i)` extension, `F.index`), and every write walks the index space itself
  * ([[MultiFocusK.tuple]] counts `0..size-1`, `representable` tabulates). So this typeclass is the
  * whole index-supply story, and it is consulted in exactly one place.
  *
  * '''What the choice of index does — and does not — affect.''' Instances are read on a path whose
  * bundle is constant by construction (see that optic's `from`), where every index yields the same
  * value: this witness changes *which* value is read only when someone hands `from` a varying
  * bundle by hand. It never changes `.modify` / `.replace` / `collect*`, and it never changes a
  * `grate ∘ iso` composition — the kernel rebuilds those per index through `broadcastFrom`, which
  * needs no index.
  *
  * '''Supply.''' The shipped instances name the canonical first index of the index types the Grate
  * factories fix: `Int` for [[MultiFocusK.tuple]] (and for `apply` over a `Function1[Int, *]`),
  * `Boolean`, `Unit`, plus any singleton type via `ValueOf`. For every other index type the caller
  * supplies one — either a local `given` (`given RepresentativeIndex[Symbol] =
  * RepresentativeIndex.at(Symbol("x"))`) or the [[RepresentativeIndex$.at at]] smart constructor at
  * the use site. [[MultiFocusK.representable]] pins its index to `F.Representation`, which has no
  * canonical inhabitant — so a grate over one takes its witness from the caller, the same value the
  * read side passes to `.at(i)` per call.
  *
  * '''The shipped instances do privilege a value''' (`0` for `Int`, `false` for `Boolean`) — the
  * thing [[MultiFocusK.representable]]'s doc rules out for a *constructor* index, and rightly: an
  * index that lives on the optic is a claim about the optic. This one is not that. It is the
  * position a read falls back to when nobody is there to name one, it keeps the bridge implicit (so
  * `iso.andThen(grate)` stays import-free), and it is unobservable on every shipped path — which is
  * why shipping it is a convenience rather than a semantics. The alternative the design note prices
  * out is no shipped instances at all: three composition cells go red and every caller writes a
  * `given`.
  *
  * An '''uninhabited''' index type (`Function1[Nothing, *]`-indexed, a phantom slot) gets no
  * instance, deliberately: there is no index to witness, so the bridge refuses rather than reading
  * a bundle at a value that cannot exist.
  */
trait RepresentativeIndex[X0]:
  def index: X0

object RepresentativeIndex:

  /** Explicit witness — the construction path for index types with no canonical value. */
  def at[X0](i: X0): RepresentativeIndex[X0] = new RepresentativeIndex[X0]:
    def index: X0 = i

  /** Canonical index for an `Int`-indexed Grate ([[MultiFocusK.tuple]]'s index space). */
  given int: RepresentativeIndex[Int] = at(0)

  /** Canonical index for a `Boolean`-indexed Grate (`Function1[Boolean, *]`, the two-point Naperian
    * shape `MultiFocus.representable[[a] =>> Boolean => a, _]` produces).
    */
  given boolean: RepresentativeIndex[Boolean] = at(false)

  /** The one-inhabitant index space: `Unit` has no other index to pick. */
  given unit: RepresentativeIndex[Unit] = at(())

  /** Singleton index types — the compiler knows the value, so no canonical choice has to be made.
    */
  given singleton[X0 <: Singleton](using v: ValueOf[X0]): RepresentativeIndex[X0] = at(v.value)
