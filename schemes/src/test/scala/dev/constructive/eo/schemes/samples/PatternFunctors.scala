package dev.constructive.eo.schemes.samples

import cats.{Applicative, Eval, Traverse}
import dev.constructive.eo.schemes.Basis

/** Sample recursive ADTs paired with their **pattern functors** for the typed recursion-scheme
  * specs ([[dev.constructive.eo.schemes.Schemes.cataF]] / `anaF` / `hyloF`). Top-level — NOT nested
  * in a spec class — to mirror the other samples and stay clear of the generics outer-accessor
  * rule.
  *
  * Each pattern functor carries its `Traverse` and a `Basis` (`Project` + `Embed`) in its
  * companion, so the schemes resolve them with no extra import — exactly the shape a real user
  * writes. The `Traverse.foldRight` instances are `Eval`-based (cats requires it) so they stay lazy
  * under the driver's trampoline.
  */

// ----- Bin: a binary tree, and its pattern functor BinF (recursion → A) -----

enum Bin:
  case Leaf(n: Int)
  case Branch(l: Bin, r: Bin)

enum BinF[+A]:
  case LeafF(n: Int)
  case BranchF(l: A, r: A)

object BinF:

  given traverse: Traverse[BinF] with

    def traverse[G[_]: Applicative, A, B](fa: BinF[A])(f: A => G[B]): G[BinF[B]] =
      fa match
        case BinF.LeafF(n)      => Applicative[G].pure(BinF.LeafF(n))
        case BinF.BranchF(l, r) => Applicative[G].map2(f(l), f(r))(BinF.BranchF(_, _))

    def foldLeft[A, B](fa: BinF[A], b: B)(f: (B, A) => B): B =
      fa match
        case BinF.LeafF(_)      => b
        case BinF.BranchF(l, r) => f(f(b, l), r)

    def foldRight[A, B](fa: BinF[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      fa match
        case BinF.LeafF(_)      => lb
        case BinF.BranchF(l, r) => f(l, Eval.defer(f(r, lb)))

  given basis: Basis[BinF, Bin] = Basis(
    {
      case Bin.Leaf(n)      => BinF.LeafF(n)
      case Bin.Branch(l, r) => BinF.BranchF(l, r)
    },
    {
      case BinF.LeafF(n)      => Bin.Leaf(n)
      case BinF.BranchF(l, r) => Bin.Branch(l, r)
    },
  )

// ----- Rose: an N-ary tree (wide-and-deep), and its pattern functor RoseF -----

final case class Rose(label: Int, kids: List[Rose])

final case class RoseF[+A](label: Int, kids: List[A])

object RoseF:

  given traverse: Traverse[RoseF] with

    def traverse[G[_]: Applicative, A, B](fa: RoseF[A])(f: A => G[B]): G[RoseF[B]] =
      Applicative[G].map(Traverse[List].traverse(fa.kids)(f))(ks => RoseF(fa.label, ks))

    def foldLeft[A, B](fa: RoseF[A], b: B)(f: (B, A) => B): B =
      Traverse[List].foldLeft(fa.kids, b)(f)

    def foldRight[A, B](fa: RoseF[A], lb: Eval[B])(f: (A, Eval[B]) => Eval[B]): Eval[B] =
      Traverse[List].foldRight(fa.kids, lb)(f)

  given basis: Basis[RoseF, Rose] = Basis(
    r => RoseF(r.label, r.kids),
    fr => Rose(fr.label, fr.kids),
  )
