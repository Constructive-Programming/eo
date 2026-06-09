package dev.constructive.eo.spike

import cats.{Functor, Monad, Traverse, Applicative, Eval}
import cats.syntax.all.*
import higherkindness.droste.{scheme, AlgebraM, CoalgebraM}

/** arbo-shaped demonstrator: an **effectful, fused, fail-able** unfold→fold — the real
  * recursion-scheme shape (`~/workspace/crypto/arbo` `Calculator.selection` via `elgotM`),
  * not the pure toy. Computes a Fibonacci-style sum over a tree whose expansion is an
  * effectful, fail-able "options" lookup (`M = Either[String, *]`): seed 13 is "blocked"
  * (`Left`), so any seed whose expansion reaches 13 fails — exactly like arbo pruning /
  * a failing I/O call. Built THREE ways so the comparison is real.
  *
  * Purpose: refute the earlier (mistaken) claim that eo's closure encoding is "pure only".
  * eo threads the effect exactly as `Plated.rewrite` threads `Eval` — via a monadic
  * traverse over the children. The effectful case is no obstacle.
  */
object EffectfulDemo:

  type M[A] = Either[String, A]

  /** The shared effectful, fail-able expansion (the "I/O" all three implementations use).
    * Leaf when no options; failure at the blocked seed; otherwise the two child seeds. */
  def options(seed: Int): M[List[Int]] =
    if seed == 13 then Left("blocked at 13")
    else if seed <= 1 then Right(Nil) // leaf
    else Right(List(seed - 1, seed - 2))

  // ---- 1. droste: pattern functor + Functor + Traverse + AlgebraM + CoalgebraM + hyloM ----

  enum GameF[+A]:
    case Tip(score: Int)
    case Branch(label: String, kids: List[A])

  given Functor[GameF] with
    def map[A, B](fa: GameF[A])(f: A => B): GameF[B] = fa match
      case GameF.Tip(s)         => GameF.Tip(s)
      case GameF.Branch(l, ks)  => GameF.Branch(l, ks.map(f))

  given Traverse[GameF] with
    def traverse[G[_]: Applicative, A, B](fa: GameF[A])(f: A => G[B]): G[GameF[B]] = fa match
      case GameF.Tip(s)        => Applicative[G].pure(GameF.Tip(s))
      case GameF.Branch(l, ks) => ks.traverse(f).map(GameF.Branch(l, _))
    def foldLeft[A, B](fa: GameF[A], b: B)(f: (B, A) => B): B = fa match
      case GameF.Tip(_)        => b
      case GameF.Branch(_, ks) => ks.foldLeft(b)(f)
    def foldRight[A, B](fa: GameF[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] = fa match
      case GameF.Tip(_)        => lb
      case GameF.Branch(_, ks) => ks.foldRight(lb)(f)

  private val coalgM: CoalgebraM[M, GameF, Int] = CoalgebraM { n =>
    options(n).map(ks => if ks.isEmpty then GameF.Tip(1) else GameF.Branch(s"n$n", ks))
  }
  private val algM: AlgebraM[M, GameF, Int] = AlgebraM {
    case GameF.Tip(s)        => Right(s)
    case GameF.Branch(_, ks) => Right(ks.sum)
  }
  val droste: Int => M[Int] = scheme.hyloM(algM, coalgM)

  // ---- 2. eo encoding-A, effectful: closure-carrying hyloM (no pattern functor) ----
  //
  // Threads the effect `M` via a monadic traverse over the child seeds — the exact shape
  // `Plated.rewrite` uses (`plate.modifyA[Eval](go)`). No `GameF`, no `Functor`/`Traverse`
  // instances. The real stack-safe engine would replace the naive recursion (U1b); this is
  // the encoding-A sketch the gate measures, now effectful.

  private def hyloM[N[_]: Monad, Seed, A](
      coalg: Seed => N[(List[Seed], List[A] => A)]
  ): Seed => N[A] =
    def go(seed: Seed): N[A] =
      coalg(seed).flatMap { (childSeeds, combine) =>
        childSeeds.traverse(go).map(combine)
      }
    go

  val eo: Int => M[Int] =
    hyloM[M, Int, Int] { n =>
      options(n).map { ks =>
        if ks.isEmpty then (List.empty[Int], (_: List[Int]) => 1)
        else (ks, (rs: List[Int]) => rs.sum)
      }
    }

  // ---- 3. hand-written effectful recursion ----

  def hand(seed: Int): M[Int] =
    options(seed).flatMap { ks =>
      if ks.isEmpty then Right(1)
      else ks.traverse(hand).map(_.sum)
    }
