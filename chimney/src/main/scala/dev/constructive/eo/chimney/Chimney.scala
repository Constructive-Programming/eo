package dev.constructive.eo
package chimney

import _root_.io.scalaland.chimney.{
  Codec,
  Iso as ChimneyIso,
  PartialTransformer,
  Patcher,
  Transformer,
}

import optics.{BijectionIso, ForgetFold, Getter, Modify, PickMendPrism}

/** Constructors that turn chimney's derived transformations into eo optics. Each one summons (or,
  * for chimney's `Iso` / `Codec`, derives) the chimney value and hands it to the matching `.toX`
  * extension in the [[chimney]] package — use those directly when you already hold a
  * hand-configured chimney value (`Transformer.define[…].withFieldRenamed(…).buildTransformer`).
  *
  * Constructors, not givens: several lawful optics can share an `(S, A)` pair, so the caller
  * declares the one it wants (the cats-eo doctrine for every bridge).
  *
  * | chimney                    | eo optic                                             | writes? |
  * |:---------------------------|:-----------------------------------------------------|:--------|
  * | `Transformer[S, T]`        | `Getter[S, T]`                                       | no      |
  * | `Iso[A, B]`                | `Iso[A, B]` (`BijectionIso`)                         | yes     |
  * | `Codec[Domain, Dto]`       | `Prism` from `Dto` to `Domain`                       | yes     |
  * | `PartialTransformer[S, T]` | `ForgetFold[S, PartialResult, T]` (keeps the errors) | no      |
  * | `Patcher[A, P]`            | `Modify[A, A, Unit, P]` (`replace(p)`)               | yes     |
  */
object Chimney:

  /** The forward half of a transformation as a read: `S` turned into `T`, ready for eo optics on
    * `T`'s fields — `Chimney.getter[User, UserDto].andThen(lens[UserDto](_.email))`. chimney
    * derives the `Transformer` when none is in scope.
    *
    * Read-only because a `Transformer` has no way back: use [[iso]] / [[prism]] to write.
    *
    * @group Constructors
    */
  def getter[S, T](using t: Transformer[S, T]): Getter[S, T] =
    t.toGetter

  /** chimney's derived `Iso[A, B]` (two total transformers) as an eo `Iso` — writable, so a chain
    * through it can `modify` fields of `B` and land back in `A`. Lawful iff the two transformers
    * round-trip; check it with cats-eo-laws' `IsoTests`.
    *
    * @group Constructors
    */
  inline def iso[A, B]: BijectionIso[A, A, B, B] =
    ChimneyIso.derive[A, B].toIso

  /** chimney's derived `Codec[Domain, Dto]` (total encode, partial decode) as an eo prism from the
    * `Dto` to the `Domain`: `getOption` decodes (a decode failure is a miss), `reverseGet` encodes.
    * Lawful iff decode∘encode is the identity on domain values and encode∘decode on the DTOs that
    * decode; check it with cats-eo-laws' `PrismTests`. For WHY a decode failed, read through
    * `Chimney.resultFold[Dto, Domain]` instead.
    *
    * @group Constructors
    */
  inline def prism[Domain, Dto]: PickMendPrism[Dto, Domain, Domain] =
    Codec.derive[Domain, Dto].toPrism

  /** A `PartialTransformer[S, T]` as a fold into its [[PartialResult]] — a read that may fail,
    * keeping chimney's errors (with their field paths) on the miss. chimney derives the transformer
    * when none is in scope.
    *
    * @group Constructors
    */
  def resultFold[S, T](using pt: PartialTransformer[S, T]): ForgetFold[S, PartialResult, T] =
    pt.toResultFold()

  /** A `Patcher[A, P]` as a write-only optic: `replace(p)` patches an `A` with `p`. Compose it
    * after any writable optic to patch a nested part — `lens[Order](_.customer)
    * .andThen(Chimney.patching[Customer, CustomerPatch]).replace(p)`. For a one-off,
    * `optic.patch(p)` does the same without building the optic. chimney derives the patcher when
    * none is in scope.
    *
    * @group Constructors
    */
  def patching[A, P](using p: Patcher[A, P]): Modify[A, A, Unit, P] =
    p.toModify
