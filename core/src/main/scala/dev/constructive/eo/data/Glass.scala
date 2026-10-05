package dev.constructive.eo
package data

import cats.Representable

/** Standalone prototype of a fixed-index, context-carrying optic. It deliberately does not use
  * `Optic`, `MultiFocus`, or `AssociativeFunctor`.
  *
  * A lawful monomorphic glass is an isomorphism `S <-> (Context, I => A)`: rebuilding a split
  * recovers the source, and splitting a rebuild recovers the context and the supplied tabulation
  * pointwise, including tabulations not obtained from a split. Polymorphic glasses require the
  * corresponding target-side decomposition to state the second law. Context equality, like function
  * equality, is observational; it need not be Scala `==`.
  *
  * Implementations and tabulations must be pure. Composition derives inner contexts lazily at each
  * outer index, with no enumeration, coordinate comparison, sampling, or memoization. An empty
  * index is therefore valid. The laws are obligations on constructors, not enforced by this trait's
  * types.
  */
trait IndexedGlass[S, T, A, B, I]:
  outer =>

  type Context

  def split(s: S): (Context, I => A)

  def rebuild(context: Context, values: I => B): T

  /** Read one coordinate. This is not a single-coordinate setter: arbitrary indexes do not come
    * with a notion of coordinate equality.
    */
  def at(i: I): S => A = s => split(s)._2(i)

  /** Rewrite every coordinate, preserving the context returned by this source's split. */
  def modify(f: (I, A) => B): S => T = s =>
    val (context, values) = split(s)
    rebuild(context, i => f(i, values(i)))

  /** Broadcast to the whole index space, including every coordinate of a composed grid. */
  def replace(value: B): S => T = s =>
    val (context, _) = split(s)
    rebuild(context, _ => value)

  /** Full-grid composition. Index and context reassociation, rather than literal type equality,
    * relate the two groupings of a three-level composition. Unit axes are not normalized away.
    *
    * Round-trip proof for lawful monomorphic components: write `outer.split(s) = (x, a)` and
    * `inner.split(a(i)) = (y(i), c(i))`. Rebuilding the split gives
    * `outer.rebuild(x, i => inner.rebuild(y(i), c(i))) = outer.rebuild(x, a) = s`. Conversely, for
    * ANY `(x, y)` and grid `w`, rebuilding uses rows `b(i) = inner.rebuild(y(i), j => w((i, j)))`.
    * The outer law recovers `(x, b)`; the inner law recovers `(y(i), j => w((i, j)))` at every `i`,
    * so the composite recovers `((x, y), w)`. No context obtained from an earlier grid is cached.
    * Both three-level groupings rebuild as
    * `outer.rebuild(x, i => middle.rebuild(y(i), j => inner.rebuild(z(i,j), k => w(i,j,k))))`;
    * their splits recover the same three contexts and focus pointwise after reassociation. The same
    * argument applies to paired source/target algebras for polymorphic composition.
    */
  def andThen[C, D, J](
      inner: IndexedGlass[A, B, C, D, J]
  ): IndexedGlass.Aux[S, T, C, D, (I, J), (outer.Context, I => inner.Context)] =
    new IndexedGlass[S, T, C, D, (I, J)]:
      type Context = (outer.Context, I => inner.Context)

      def split(s: S): (Context, ((I, J)) => C) =
        val (context, values) = outer.split(s)
        val rows: I => (inner.Context, J => C) = i => inner.split(values(i))
        ((context, i => rows(i)._1), ij => rows(ij._1)._2(ij._2))

      def rebuild(context: Context, values: ((I, J)) => D): T =
        outer.rebuild(context._1, i => inner.rebuild(context._2(i), j => values((i, j))))

object IndexedGlass:

  type Aux[S, T, A, B, I, X] = IndexedGlass[S, T, A, B, I] { type Context = X }

  /** The context-free specialization. Generic `andThen` retains its product context even when both
    * inputs have Unit context; no context-erasing coercion is installed in this prototype.
    * Context-sensitive collection/zip operations are deliberately absent from the general API.
    */
  type Grate[S, T, A, B, I] = Aux[S, T, A, B, I, Unit]

  /** User-written split/rebuild algebra. The caller is responsible for the laws above. */
  def apply[S, T, A, B, I, X](decompose: S => (X, I => A))(
      assemble: (X, I => B) => T,
  ): Aux[S, T, A, B, I, X] =
    new IndexedGlass[S, T, A, B, I]:
      type Context = X
      def split(s: S): (X, I => A) = decompose(s)
      def rebuild(context: X, values: I => B): T = assemble(context, values)

  /** A cats Representable supplies precisely the two inverse tabulation operations needed here.
    * Passed explicitly so its path-dependent Representation is retained without another given.
    */
  def representable[F[_], A, B](r: Representable[F]): Grate[F[A], F[B], A, B, r.Representation] =
    IndexedGlass[F[A], F[B], A, B, r.Representation, Unit](s => ((), r.index(s)))((_, values) =>
      r.tabulate(values),
    )

  /** An Iso has one coordinate, not a broadcast over an arbitrary (possibly empty) index. The
    * functions must be inverse in the monomorphic case, or have matching target-side inverses in
    * the polymorphic case.
    */
  def iso[S, T, A, B](forward: S => A)(backward: B => T): Grate[S, T, A, B, Unit] =
    IndexedGlass[S, T, A, B, Unit, Unit](s => ((), _ => forward(s)))((_, values) =>
      backward(values(())),
    )

  /** Identity algebra on a single Unit coordinate, including type-changing writes. */
  def unit[A, B]: Grate[A, B, A, B, Unit] = iso[A, B, A, B](identity)(identity)
