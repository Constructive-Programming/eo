package dev.constructive.eo
package schemes

/** The user-supplied bridge between a recursive type `S` and one *layer* of its **pattern functor**
  * `F[_]`, for the typed recursion-scheme path ([[Schemes.cataF]] / [[Schemes.anaF]] /
  * [[Schemes.hyloF]]).
  *
  * A pattern functor replaces `S`'s recursive positions with a type parameter:
  * {{{
  *   enum Bin:        case Leaf(n: Int);  case Branch(l: Bin, r: Bin)
  *   enum BinF[A]:    case LeafF(n: Int); case BranchF(l: A,  r: A)   // recursion → A
  * }}}
  * [[Project]] peels one layer off (`S => F[S]`), [[Embed]] glues one layer back on (`F[S] => S`).
  * The driver then walks `F` with the user's `Traverse[F]`, so algebras pattern-match `F`'s **named
  * constructors** (`case BranchF(l, r) => l + r`) instead of indexing an erased `PSVec[AnyRef]` —
  * the type-safety the default [[Schemes.cata]] path lacks.
  *
  * Unlike the `F` type itself (which the user must write — Scala-3 macros emit terms, not type
  * definitions), `Project`/`Embed` are ordinary instances. v1 expects them **hand-written**
  * (droste's model); deriving them from the `S`↔`F` constructor correspondence is deferred future
  * work.
  *
  * '''Coherence laws''' (the `S`↔`F` correspondence is hand-maintained and NOT compiler-checked — a
  * swapped or non-exhaustive mapping is a silent bug, so these are exercised by the typed-scheme
  * law suite):
  * {{{
  *   embed(project(s)) == s          // round-trip through one layer of S
  *   project(embed(fs)) == fs        // round-trip through one layer of F
  * }}}
  */
trait Project[F[_], S]:

  /** Peel one layer: expose `S`'s immediate children as `F`'s recursive slots. */
  def project(s: S): F[S]

/** @see [[Project]] — the dual, gluing one `F`-layer back into an `S`. */
trait Embed[F[_], S]:

  /** Glue one layer: rebuild an `S` node from an `F` of already-built children. */
  def embed(fs: F[S]): S

/** Both halves of the `S`↔`F` correspondence in one instance — the convenience an implementor
  * reaches for when supplying `project` and `embed` together. A `given Basis` satisfies both a
  * `Project` and an `Embed` requirement.
  */
trait Basis[F[_], S] extends Project[F, S], Embed[F, S]

object Basis:

  /** Build a [[Basis]] from the two halves. */
  def apply[F[_], S](projectFn: S => F[S], embedFn: F[S] => S): Basis[F, S] =
    new Basis[F, S]:
      def project(s: S): F[S] = projectFn(s)
      def embed(fs: F[S]): S = embedFn(fs)
