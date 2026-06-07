package dev.constructive.eo
package bench
package fixture

import cats.{Applicative, Traverse}
import cats.instances.list.given
import cats.instances.vector.given
import io.circe.Json
import monocle.{PTraversal, Traversal as MTraversal}
import monocle.function.Plated as MPlated

import dev.constructive.eo.generics.plate
import dev.constructive.eo.optics.Plated as EoPlated

/** Recursive ADTs + paired EO / Monocle `Plated` instances for the `PlatedUniverseBench`.
  *
  * Top-level (not nested) so the `plate[S]` macro's `new V(...)` keeps its outer accessor — the
  * same reason the generics test fixtures live at package scope.
  */
enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

enum Bin:
  case Leaf(value: Int)
  case Node(left: Bin, right: Bin)

object PlatedTrees:

  // ----- EO plates -----------------------------------------------------------
  // Macro-derived (the path users actually take). `platedJson` comes from
  // `dev.constructive.eo.circe.given` for the JSON subject.
  given eoExpr: EoPlated[Expr] = plate[Expr]
  given eoBin: EoPlated[Bin] = plate[Bin]

  // ----- Monocle plates (hand-written; Monocle ships none for these) ---------
  given mExpr: MPlated[Expr] = new MPlated[Expr]:
    val plate: MTraversal[Expr, Expr] = new PTraversal[Expr, Expr, Expr, Expr]:
      def modifyA[F[_]](f: Expr => F[Expr])(s: Expr)(using F: Applicative[F]): F[Expr] =
        s match
          case Expr.Var(_)       => F.pure(s)
          case Expr.App(a, b)    => F.map2(f(a), f(b))(Expr.App(_, _))
          case Expr.Lam(b, body) => F.map(f(body))(Expr.Lam(b, _))

  given mBin: MPlated[Bin] = new MPlated[Bin]:
    val plate: MTraversal[Bin, Bin] = new PTraversal[Bin, Bin, Bin, Bin]:
      def modifyA[F[_]](f: Bin => F[Bin])(s: Bin)(using F: Applicative[F]): F[Bin] =
        s match
          case Bin.Leaf(_)    => F.pure(s)
          case Bin.Node(l, r) => F.map2(f(l), f(r))(Bin.Node(_, _))

  given mJson: MPlated[Json] = new MPlated[Json]:
    val plate: MTraversal[Json, Json] = new PTraversal[Json, Json, Json, Json]:
      def modifyA[F[_]](f: Json => F[Json])(s: Json)(using F: Applicative[F]): F[Json] =
        s.fold(
          F.pure(s),
          _ => F.pure(s),
          _ => F.pure(s),
          _ => F.pure(s),
          arr => F.map(Traverse[Vector].traverse(arr)(f))(Json.fromValues),
          obj =>
            F.map(
              Traverse[List].traverse(obj.toList) { case (k, v) => F.map(f(v))(k -> _) }
            )(Json.fromFields),
        )

  // ----- builders ------------------------------------------------------------

  /** A balanced `App` tree of ~`leaves` `Var` leaves — "normal" depth (~log n). */
  def balancedExpr(leaves: Int): Expr =
    if leaves <= 1 then Expr.Var("x")
    else Expr.App(balancedExpr(leaves / 2), balancedExpr(leaves - leaves / 2))

  /** A left-leaning `Bin` spine of `depth` `Node`s — the degenerate deep shape. */
  def deepBin(depth: Int): Bin =
    var t: Bin = Bin.Leaf(0)
    var i = 0
    while i < depth do
      t = Bin.Node(t, Bin.Leaf(i))
      i += 1
    t

  /** A balanced JSON array tree of ~`leaves` string leaves — "normal" depth. */
  def balancedJson(leaves: Int): Json =
    if leaves <= 1 then Json.fromString("x")
    else Json.arr(balancedJson(leaves / 2), balancedJson(leaves - leaves / 2))

  // ----- hand-written visitor baseline ---------------------------------------
  // The recursive subterm collector you'd write WITHOUT optics — the "visitor
  // pattern" rendered as a pre-order walk into a buffer. Same enumeration as
  // `universe`; the contrast is hand-rolled recursion vs the optic. (Like a
  // classic OO visitor, it recurses on the call stack, so it shares the naive
  // depth ceiling — fine at these sizes, unlike the trampolined `universe`.)

  def visitExpr(e: Expr): List[Expr] =
    val buf = scala.collection.mutable.ListBuffer.empty[Expr]
    def go(x: Expr): Unit =
      buf += x
      x match
        case Expr.App(a, b)    => go(a); go(b)
        case Expr.Lam(_, body) => go(body)
        case Expr.Var(_)       => ()
    go(e)
    buf.toList

  def visitBin(b: Bin): List[Bin] =
    val buf = scala.collection.mutable.ListBuffer.empty[Bin]
    def go(x: Bin): Unit =
      buf += x
      x match
        case Bin.Node(l, r) => go(l); go(r)
        case Bin.Leaf(_)    => ()
    go(b)
    buf.toList

  def visitJson(j: Json): List[Json] =
    val buf = scala.collection.mutable.ListBuffer.empty[Json]
    def go(x: Json): Unit =
      buf += x
      x.fold(
        (),
        _ => (),
        _ => (),
        _ => (),
        arr => arr.foreach(go),
        obj => obj.values.foreach(go),
      )
    go(j)
    buf.toList
