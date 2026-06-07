package dev.constructive.eo
package bench
package fixture

import scala.collection.immutable.ArraySeq

/** Carrier-stress shapes for the PowerSeries / MultiFocus traversal benches.
  *
  * These are the shapes the canonical [[Order]] deliberately does **not** have — each isolates a
  * specific PSVec-carrier cost that `Order.lines` (a single `List` level) can't express, so they
  * live here as shared, named fixtures rather than as bench-private inner classes:
  *
  *   - [[Person]] / [[Phone]] — an `ArraySeq`-backed collection. `PowerSeriesBench` uses `ArraySeq`
  *     (not `Order`'s `List`) on purpose: `Functor[ArraySeq].map` is a native array walk, so the
  *     numbers reflect the optic machinery rather than `List`'s pointer-chasing.
  *   - [[Company]] / [[Department]] / [[Employee]] — a tree-of-trees (a `List` of `ArraySeq`) with
  *     two separate `Traversal.each` fan-out levels; the canonical schema has only one array level.
  *   - [[Result]] — a sum type, the vehicle for the Prism-after-Traversal sparse-hit bench. `Order`
  *     is all products, and plan 009 keeps Prisms on a dedicated ADT.
  */
final case class Phone(isMobile: Boolean, number: String)
final case class Person(name: String, phones: ArraySeq[Phone])

final case class Employee(id: Int, name: String, active: Boolean)
final case class Department(name: String, employees: ArraySeq[Employee])
final case class Company(name: String, departments: List[Department])

enum Result:
  case Ok(value: Int)
  case Err(msg: String)
