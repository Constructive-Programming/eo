package dev.constructive.eo
package data

import cats.Representable

import optics.Optic

/** Fixed-index carrier: the leftover `X` and a focus tabulation over `I`. */
type Glass[I] = [X, A] =>> (context: X, values: I => A)

/** Standalone prototype of the full-grid successor to `MultiFocus[Function1[X0, *]]`
  * (`docs/research/2026-10-05-multifocus-redesign.md`): an [[Optic]] whose focus is a tabulation
  * over a fixed index, so composition takes the PRODUCT index and reads every nested coordinate,
  * not a shared-index diagonal.
  *
  * It is an ordinary member of the `Optic` family — carrier `Glass[I]`, leftover `X`. Constructors
  * implement `to` / `from`; nothing else stores state.
  *
  * A lawful glass is an isomorphism `S <-> (X, I => A)`: `from(to(s))` recovers the source, and
  * `to(from(bundle))` recovers the context and supplied tabulation pointwise — including bundles
  * not obtained from `to`. Polymorphic glasses require the corresponding target-side decomposition
  * to state the second law. Context equality, like function equality, is observational; it need not
  * be Scala `==`.
  *
  * Implementations and tabulations must be pure. Composition derives inner contexts lazily at each
  * outer index, with no enumeration, coordinate comparison, sampling, or memoization. An empty
  * index is therefore valid. The laws are obligations on constructors, not enforced by this trait's
  * types.
  *
  * Deliberately ABSENT while this is a prototype: no `AssociativeFunctor[Glass[I]]` (same-index
  * composition would force the diagonal this family exists to escape), no inbound/outbound bridges
  * to other carriers, and no `zip`/`collect` — those are context-sensitive operations whose honest
  * home is the `X = Unit` specialization or a later design.
  *
  * @tparam I
  *   the fixed index type; `at`, `modify`, `replace` and the composed grid all address it.
  *
  * @groupname ops Coordinate-addressable operations
  */
trait IndexedGlass[S, T, A, B, I] extends Optic[S, T, A, B, Glass[I]]:
  outer =>

  /** Read one coordinate. This is not a single-coordinate setter: arbitrary indexes do not come
    * with a notion of coordinate equality.
    *
    * @group ops
    */
  def at(i: I): S => A = s => to(s).values(i)

  /** Rewrite every coordinate with its own index visible to the function, preserving the context
    * returned by this source's `to`.
    *
    * @group ops
    */
  def modify(f: (I, A) => B): S => T = s =>
    val bundle = to(s)
    from((context = bundle.context, values = (i: I) => f(i, bundle.values(i))))

  /** Broadcast to the whole index space, including every coordinate of a composed grid.
    *
    * @group ops
    */
  def replace(value: B): S => T = s =>
    val bundle = to(s)
    from((context = bundle.context, values = (_: I) => value))

  /** Full-grid composition. Index and context reassociation, rather than literal type equality,
    * relate the two groupings of a three-level composition. Unit axes are not normalized away.
    *
    * Round-trip proof for lawful monomorphic components: write `outer.to(s) = (x, a)` and
    * `inner.to(a(i)) = (y(i), c(i))`. Applying `from` to this bundle gives
    * `outer.from((x, i => inner.from((y(i), c(i))))) = outer.from((x, a)) = s`. Conversely, for ANY
    * `(x, y)` and grid `w`, `from` uses rows `b(i) = inner.from((y(i), j => w(i,j)))`. The outer
    * law recovers `(x, b)`; the inner law recovers `(y(i), j => w(i, j))` at every `i`, so the
    * composite recovers `((x, y), w)`. No context obtained from an earlier grid is cached. Both
    * three-level groupings reconstruct as
    * `outer.from((x, i => middle.from((y(i), j => inner.from((z(i,j), k => w(i,j,k)))))))`; their
    * `to` operations recover the same contexts and focus after reassociation. The same argument
    * applies to paired source/target algebras for polymorphic composition.
    */
  def andThen[C, D, J](
      inner: IndexedGlass[A, B, C, D, J]
  ): IndexedGlass.Aux[S, T, C, D, (I, J), (outer.X, I => inner.X)] =
    new IndexedGlass[S, T, C, D, (I, J)]:
      type X = (outer.X, I => inner.X)

      def to(s: S): Glass[(I, J)][X, C] =
        val bundle = outer.to(s)
        val rows: I => Glass[J][inner.X, C] = i => inner.to(bundle.values(i))
        (
          context = (bundle.context, (i: I) => rows(i).context),
          values = (ij: (I, J)) => rows(ij._1).values(ij._2)
        )

      def from(xd: Glass[(I, J)][X, D]): T =
        val context = xd.context
        val grid = xd.values
        outer.from(
          (
            context = context._1,
            values = (i: I) =>
              inner.from(
                (
                  context = context._2(i),
                  values = (j: J) => grid((i, j))
                )
              )
          )
        )

object IndexedGlass:

  type Aux[S, T, A, B, I, X0] = IndexedGlass[S, T, A, B, I] { type X = X0 }

  /** The context-free specialization — the classical Grate shape (`X = Unit`), with the full-grid
    * index. Generic `andThen` retains its product context even when both inputs have `Unit`
    * context; no context-erasing coercion is installed in this prototype.
    */
  type Grate[S, T, A, B, I] = Aux[S, T, A, B, I, Unit]

  /** User-written `to` / `from` algebra. The caller is responsible for the laws stated on
    * [[IndexedGlass]].
    */
  def apply[S, T, A, B, I, X0](decompose: S => (X0, I => A))(
      assemble: (X0, I => B) => T
  ): Aux[S, T, A, B, I, X0] =
    new IndexedGlass[S, T, A, B, I]:
      type X = X0
      def to(s: S): Glass[I][X0, A] =
        val (x, k) = decompose(s)
        (context = x, values = k)
      def from(xa: Glass[I][X0, B]): T = assemble(xa.context, xa.values)

  /** A cats `Representable` supplies precisely the two inverse tabulation operations needed here.
    * Passed explicitly so its path-dependent `Representation` is retained without another given.
    */
  def representable[F[_], A, B](r: Representable[F]): Grate[F[A], F[B], A, B, r.Representation] =
    apply[F[A], F[B], A, B, r.Representation, Unit](fa => ((), r.index(fa)))((_, values) =>
      r.tabulate(values),
    )

  /** An Iso has one coordinate, not a broadcast over an arbitrary (possibly empty) index. The
    * functions must be inverse in the monomorphic case, or have matching target-side inverses in
    * the polymorphic case.
    */
  def iso[S, T, A, B](forward: S => A)(backward: B => T): Grate[S, T, A, B, Unit] =
    apply[S, T, A, B, Unit, Unit](s => ((), _ => forward(s)))((_, values) => backward(values(())))

  /** Identity algebra on a single `Unit` coordinate, including type-changing writes. */
  def unit[A, B]: Grate[A, B, A, B, Unit] = iso[A, B, A, B](identity)(identity)
