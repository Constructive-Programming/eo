package dev.constructive.eo
package bench
package fixture

/** Top-level sample ADTs for [[dev.constructive.eo.bench.GenericsBench]].
  *
  * The `generics` macros emit `new S(...)` setters that carry no outer accessor, so these types
  * must live at the top level (not nested inside the bench class) — the same constraint the
  * generics test suite documents.
  */
final case class GPerson(name: String, age: Int)

enum GShape:
  case Circle(r: Double)
  case Square(s: Double)
