package dev.constructive.eo

import scala.language.implicitConversions

import cats.Functor
import cats.instances.list.given
import org.specs2.mutable.Specification

import optics.{BijectionIso, Optic, Review, Unfold}
import data.Forget

/** One-layer pattern functor for the `Unfold.algebra` cases — `Functor` but deliberately NO
  * `Applicative` (`pure` cannot pick a constructor), the shape that motivates the constraint-free
  * [[Unfold.algebra]] factory.
  */
enum BinF[+A]:
  case LeafF(n: Int)
  case BranchF(l: A, r: A)

object BinF:

  given Functor[BinF] with

    def map[A, B](fa: BinF[A])(f: A => B): BinF[B] = fa match
      case BinF.LeafF(n)      => BinF.LeafF(n)
      case BinF.BranchF(l, r) => BinF.BranchF(f(l), f(r))

/** Behaviour of [[optics.Unfold]] — the build-only / many citizen (`embed: F[B] => T` on the
  * `Forget[F]` carrier) — and of the composition seams it opens:
  *
  *   - the fused `Review.andThen(Unfold)` / `Unfold.andThen(Review)` / `Unfold.andThen(Unfold)`
  *     members,
  *   - the generic `Morph`-routed `direct.andThen(unfold)` chain, whose build side executes
  *     `Composer.direct2forget`'s `from` — the branch that was a documented-unreachable `???` until
  *     `Unfold` inhabited the cell.
  */
class UnfoldSpec extends Specification:

  private val sum: Unfold[Int, Int, List] = Unfold((xs: List[Int]) => xs.sum)

  private val evalAlg: Unfold[Int, Int, BinF] = Unfold.algebra[Int, Int, BinF] {
    case BinF.LeafF(n)      => n
    case BinF.BranchF(l, r) => l + r
  }

  "Unfold.embed assembles one whole from a layer of parts" >> {
    sum.embed(List(1, 2, 3)) === 6
  }

  "Unfold.algebra carries a pattern-functor algebra (no Applicative anywhere)" >> {
    evalAlg.embed(BinF.BranchF(2, 3)) === 5
    evalAlg.embed(BinF.LeafF(7)) === 7
  }

  "Review.andThen(Unfold) post-processes the assembled whole (fused, constraint-free)" >> {
    val show = Review[String, Int](_.toString)
    val composite: Unfold[String, Int, BinF] = show.andThen(evalAlg)
    composite.embed(BinF.BranchF(2, 3)) === "5"
  }

  "Unfold.andThen(Review) pre-processes each part (fused, Functor only)" >> {
    val parse = Review[Int, String](_.toInt)
    val composite: Unfold[Int, String, BinF] = evalAlg.andThen(parse)
    composite.embed(BinF.BranchF("2", "3")) === 5
  }

  "Unfold.andThen(Unfold) layers algebras via the algebraic-lens pull (Applicative)" >> {
    val negate: Unfold[Int, Int, List] = Unfold((xs: List[Int]) => -xs.sum)
    // outer consumes the singleton re-lift of the inner's result: -(sum [1,2,3]) = -6
    negate.andThen(sum).embed(List(1, 2, 3)) === -6
  }

  "reversible outer ∘ unfold lands the fused Unfold (Iso build half re-homes the whole)" >> {
    val render: BijectionIso[Unit, String, Unit, Int] =
      new BijectionIso[Unit, String, Unit, Int](identity, _.toString)
    val composite: Unfold[String, Int, List] = render.andThen(sum)
    composite.embed(List(1, 2, 3)) === "6"
  }

  "unfold ∘ reversible inner mends each part through a Prism's build half" >> {
    val positive = optics.Prism[Int, Int](n => if n > 0 then Right(n) else Left(n), identity)
    val composite: Unfold[Int, Int, List] = sum.andThen(positive)
    composite.embed(List(1, 2, 3)) === 6
  }

  "the morph-routed Forget chain reaches direct2forget's formerly-??? from soundly" >> {
    // Route a Direct-carried builder through Composer[Direct, Forget[List]] explicitly, then
    // compose under the shared Forget[List] carrier (assocForgetMonad) — its composeFrom feeds
    // the lifted optic's from a pure-singleton, executing the branch that was `???`.
    val render: BijectionIso[Unit, String, Unit, Int] =
      new BijectionIso[Unit, String, Unit, Int](identity, _.toString)
    val lifted: Optic[Unit, String, Unit, Int, Forget[List]] = render.morph[Forget[List]]
    val composite: Optic[Unit, String, Unit, Int, Forget[List]] = lifted.andThen(sum)
    composite.from(Forget(List(1, 2, 3))) === "6"
  }

  "read-side operations on an Applicative Unfold degrade to the singleton layer" >> {
    // .modify routes Unit through the vestigial to = pure(()): embed(pure(b))
    sum.modify(_ => 5)(()) === 5
  }

  "read-side operations on a pattern-functor Unfold fail loudly" >> {
    evalAlg.modify(_ => 1)(()) must throwAn[UnsupportedOperationException]
  }
