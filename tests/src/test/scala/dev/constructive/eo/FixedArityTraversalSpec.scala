package dev.constructive.eo

import cats.instances.list.given
import org.specs2.mutable.Specification

import optics.{Lens, Optic, Prism, Traversal}
import optics.Optic.*
import data.{MultiFocus, PSVec}

/** Composition coverage for the fixed-arity traversals (`Traversal.two` / `.three` / `.four`).
  *
  * Every constructor in `Traversal.scala` is expected to compose through the standard `.andThen` —
  * the fixed-arity family included: past `each`, past a Lens, past a Prism, and with each other.
  * The blocks below pin that surface plus the read side (`foldMap`) and the composed-carrier
  * identity (`MultiFocus[PSVec]`) the cookbook's `adjustTimes` signature relies on.
  */
class FixedArityTraversalSpec extends Specification:

  case class Tx(issue: Int, ack: Int)
  case class Ledger(froms: List[Tx], tos: List[Tx])

  val bothLists: Optic[Ledger, Ledger, List[Tx], List[Tx], MultiFocus[PSVec]] =
    Traversal.two[Ledger, Ledger, List[Tx], List[Tx]](_.froms, _.tos, Ledger(_, _))

  val bothTimes: Optic[Tx, Tx, Int, Int, MultiFocus[PSVec]] =
    Traversal.two[Tx, Tx, Int, Int](_.issue, _.ack, Tx(_, _))

  "Traversal.two composes past each (two ∘ each)" >> {
    val everyTx = bothLists.andThen(Traversal.each[List, Tx])
    everyTx.modify(tx => Tx(tx.issue + 1, tx.ack + 1))(
      Ledger(List(Tx(1, 2)), List(Tx(10, 20)))
    ) === Ledger(List(Tx(2, 3)), List(Tx(11, 21)))
  }

  "each composes past Traversal.two (each ∘ two)" >> {
    val everyTime = Traversal.each[List, Tx].andThen(bothTimes)
    everyTime.modify(_ + 1)(List(Tx(1, 2), Tx(3, 4))) === List(Tx(2, 3), Tx(4, 5))
  }

  "the full two ∘ each ∘ two chain modifies every leaf and stays on MultiFocus[PSVec]" >> {
    val ledgerTimes: Optic[Ledger, Ledger, Int, Int, MultiFocus[PSVec]] =
      bothLists
        .andThen(Traversal.each[List, Tx])
        .andThen(bothTimes)
    ledgerTimes.modify(_ + 100)(Ledger(List(Tx(1, 2)), List(Tx(3, 4)))) ===
      Ledger(List(Tx(101, 102)), List(Tx(103, 104)))
  }

  "a Lens drills into Traversal.two with siblings preserved" >> {
    case class Wrap(label: String, tx: Tx)
    val txL = Lens[Wrap, Tx](_.tx, (w, t) => w.copy(tx = t))
    val drilled = txL.andThen(bothTimes)
    drilled.modify(_ + 1)(Wrap("keep-me", Tx(1, 2))) === Wrap("keep-me", Tx(2, 3))
  }

  "Traversal.two composes into a Lens" >> {
    val issueOfBoth =
      Traversal
        .two[(Tx, Tx), (Tx, Tx), Tx, Tx](_._1, _._2, (_, _))
        .andThen(Lens[Tx, Int](_.issue, (t, i) => t.copy(issue = i)))
    issueOfBoth.modify(_ + 1)((Tx(1, 2), Tx(3, 4))) === (Tx(2, 2), Tx(4, 4))
  }

  "Traversal.two composes into a Prism (sparse fixed-arity)" >> {
    val someP = Prism[Option[Int], Int](
      {
        case Some(i) => Right(i)
        case None    => Left(None)
      },
      Some(_),
    )
    val bothSomes =
      Traversal
        .two[(Option[Int], Option[Int]), (Option[Int], Option[Int]), Option[Int], Option[
          Int
        ]](_._1, _._2, (_, _))
        .andThen(someP)
    bothSomes.modify(_ + 1)((Some(1), None)) === (Some(2), None)
  }

  "Traversal.three and four compose past a Lens" >> {
    val threeIssues =
      Traversal
        .three[(Tx, Tx, Tx), (Tx, Tx, Tx), Tx, Tx](_._1, _._2, _._3, (_, _, _))
        .andThen(Lens[Tx, Int](_.issue, (t, i) => t.copy(issue = i)))
    val fourIssues =
      Traversal
        .four[(Tx, Tx, Tx, Tx), (Tx, Tx, Tx, Tx), Tx, Tx](
          _._1,
          _._2,
          _._3,
          _._4,
          (_, _, _, _),
        )
        .andThen(Lens[Tx, Int](_.issue, (t, i) => t.copy(issue = i)))
    (threeIssues.modify(_ + 1)((Tx(1, 0), Tx(2, 0), Tx(3, 0))) ===
      (Tx(2, 0), Tx(3, 0), Tx(4, 0))).and(
      fourIssues.modify(_ + 1)((Tx(1, 0), Tx(2, 0), Tx(3, 0), Tx(4, 0))) ===
        (Tx(2, 0), Tx(3, 0), Tx(4, 0), Tx(5, 0))
    )
  }

  "foldMap reads the fixed-arity foci in getter order" >> {
    bothTimes.foldMap(List(_))(Tx(7, 9)) === List(7, 9)
  }
