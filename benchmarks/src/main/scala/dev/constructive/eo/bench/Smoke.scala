package dev.constructive.eo
package bench

import kyo.Maybe
import kyo.Maybe.{Absent, Present}

import dev.constructive.eo.bench.fixture.*
import dev.constructive.eo.optics.{Iso as EoIso, Prism as EoPrism, Traversal as EoTraversal}

object Smoke:
  def main(args: Array[String]): Unit =
    val order = Domain.mkOrder(3)

    // Lens
    val idL = EoLens[Order, Long](_.id, (o, v) => o.copy(id = v))
    assert(idL.get(order) == order.id)
    assert(idL.replace(99L)(order).id == 99L)
    assert(idL.modify(_ + 1)(order).id == order.id + 1)

    // Lens andThen Lens (fused GetReplaceLens path)
    val custL = EoLens[Order, Customer](_.customer, (o, c) => o.copy(customer = c))
    val addrL = EoLens[Customer, Address](_.address, (c, a) => c.copy(address = a))
    val streetL = EoLens[Address, String](_.street, (a, s) => a.copy(street = s))
    val deep = custL.andThen(addrL).andThen(streetL)
    assert(deep.get(order) == order.customer.address.street)
    assert(deep.modify(_.toUpperCase)(order).customer.address.street ==
      order.customer.address.street.toUpperCase)

    // Prism (PickMend)
    val someP = EoPrism.optional[Option[Int], Int](Maybe.fromOption, Some(_))
    assert(someP.getOption(Some(7)) == Present(7))
    assert(someP.getOption(None) == Absent)
    assert(someP.reverseGet(5) == Some(5))
    assert(someP.modify(_ + 1)(Some(7)) == Some(8))
    assert(someP.modify(_ + 1)(None) == None)

    // Prism (MendTear) + prism∘prism fusion
    val rightP = EoPrism[Either[String, Int], Int](
      e => e.fold(s => kyo.Result.fail(Left(s)), i => kyo.Result.succeed(i)),
      i => Right(i),
    )
    assert(rightP.getOption(Right(4)) == Present(4))
    assert(rightP.getOption(Left("x")) == Absent)
    assert(rightP.modify(_ * 2)(Right(4)) == Right(8))
    assert(rightP.modify(_ * 2)(Left("x")) == Left("x"))

    val posP = EoPrism.optional[Int, Int](i => Maybe.when(i > 0)(i), identity)
    val fused = someP.andThen(posP)
    assert(fused.getOption(Some(3)) == Present(3))
    assert(fused.getOption(Some(-3)) == Absent)
    assert(fused.getOption(None) == Absent)
    assert(fused.modify(_ + 10)(Some(3)) == Some(13))
    assert(fused.modify(_ + 10)(Some(-3)) == Some(-3))

    // Iso
    val addr = order.customer.address
    val isoT = EoIso[Address, Address, (String, String), (String, String)](
      a => (a.street, a.city),
      t => addr.copy(street = t._1, city = t._2),
    )
    assert(isoT.get(addr) == (addr.street, addr.city))
    assert(isoT.reverseGet(("a", "b")).street == "a")

    // Traversal over List
    val each = EoTraversal.each[List, LineItem]
    val bumped = each.modify(li => li.copy(qty = li.qty + 1))(order.lines)
    assert(bumped.map(_.qty) == order.lines.map(_.qty).map(_ + 1))
    assert(each.length(order.lines) == order.lines.length)
    assert(each.headOption(order.lines) == Present(order.lines.head))

    // Lens ∘ Traversal (cross-carrier morph) — the composed MultiFocus path
    val linesL = EoLens[Order, List[LineItem]](_.lines, (o, ls) => o.copy(lines = ls))
    val qtyL = EoLens[LineItem, Int](_.qty, (li, q) => li.copy(qty = q))
    val allQty = linesL.andThen(each).andThen(qtyL)
    val bumpedOrder = allQty.modify(_ + 5)(order)
    assert(bumpedOrder.lines.map(_.qty) == order.lines.map(_.qty).map(_ + 5))
    assert(allQty.foldMap(identity)(order) == order.lines.map(_.qty).sum)

    // Plated transform/rewrite via selfChildren on a tiny expr tree
    enum T:
      case Leaf(n: Int)
      case Node(l: T, r: T)
    import dev.constructive.eo.data.PSVec
    given optics.Plated[T] = optics.Plated.fromChildrenVec[T](
      { case T.Leaf(_) => PSVec.empty; case T.Node(l, r) => PSVec.of(l, r) },
      { case (T.Leaf(n), _) => T.Leaf(n); case (T.Node(_, _), kids) => T.Node(kids(0), kids(1)) },
    )
    val tree = T.Node(T.Leaf(1), T.Node(T.Leaf(2), T.Leaf(3)))
    val doubled = optics.Plated.transform[T] { case T.Leaf(n) => T.Leaf(n * 2); case x => x }(tree)
    assert(doubled == T.Node(T.Leaf(2), T.Node(T.Leaf(4), T.Leaf(6))))
    val rewritten = optics.Plated.rewrite[T] {
      case T.Leaf(n) if n < 8 => Present(T.Leaf(n * 2)); case _ => Absent
    }(tree)
    assert(rewritten == T.Node(T.Leaf(8), T.Node(T.Leaf(8), T.Leaf(12))))

    println("smoke: all assertions passed")
