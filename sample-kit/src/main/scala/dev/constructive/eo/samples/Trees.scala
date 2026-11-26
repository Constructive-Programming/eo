package dev.constructive.eo.samples

// Shared sample ADTs for the specs, benchmarks and docs. Top-level, not nested: the `plate[S]`
// and `lens` macros emit `new V(...)`, which loses its outer accessor inside a class. No givens
// here: each user derives the optics it needs (the docs teach exactly that derivation).

/** A small lambda calculus: variables, application, abstraction. */
enum Expr:
  case Var(name: String)
  case App(f: Expr, x: Expr)
  case Lam(bind: String, body: Expr)

/** A binary tree with `Int` leaves. */
enum Bin:
  case Leaf(value: Int)
  case Node(left: Bin, right: Bin)
