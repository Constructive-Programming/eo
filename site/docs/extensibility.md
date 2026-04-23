# Extensibility

How you build a custom optic tuned for your domain's hot path,
keep full integration with the rest of the Optic universe, and
don't re-invent typeclass plumbing.

## The lever

cats-eo's central trait is:

```scala
trait Optic[S, T, A, B, F[_, _]]:
  type X
  def to:   S      => F[X, A]
  def from: F[X, B] => T
```

Every built-in family — Lens, Prism, Iso, Optional, Setter,
Traversal, Fold — is a concrete subclass that differs only in
which carrier `F[_, _]` it picks. The operations (`.get`,
`.modify`, `.modifyA`, `.foldMap`, `.andThen`, …) are
extensions on `Optic` that dispatch through carrier-specific
typeclasses (`Accessor[F]`, `ForgetfulFunctor[F]`,
`ForgetfulTraverse[F, Applicative]`, `AssociativeFunctor[F, X,
Y]`, …).

That's the extension point. When you ship an optic tuned for
your domain, you don't rewrite the operations — you pick which
existing carrier to reuse and which inline methods to shadow
for the hot path. Everything else plugs in automatically.

## Four axes of extension

From cheapest to heaviest:

| Axis | What you write | What you get | Typical cost |
|------|----------------|--------------|--------------|
| 1. **New concrete subclass on an existing carrier** | `class MyOptic[...] extends Optic[S, T, A, B, F]` — your own `type X`, `to`, `from` | Every generic extension the carrier already supports (`.get` / `.modify` / `.modifyA` / `.foldMap`), plus `.andThen` with every optic family that shares `F` — and, via cross-carrier `Morph`, with every family whose carrier has a `Composer[F, G]` bridge | Minutes. Zero runtime overhead versus the built-in subclasses — same typeclass dispatch |
| 2. **Shadow generic extensions with fused class-level methods** | `inline def modify(f: A => B): S => T = …` on the concrete subclass | Callers who bind to the concrete type get the fused code; callers who bind to the generic `Optic` trait still route through the carrier. Both paths type-check — pick which one you need | Per hot-path method. Lets you skip the carrier round-trip without losing the trait contract |
| 3. **New `Composer[F, G]` bridge** | A `given Composer[F, G]` that expresses an `Optic[…, F]` as an `Optic[…, G]` | `.andThen` between any optic in `F` and any optic in `G`, in either direction, via the stock `Morph` instances | Hours. You pick the direction of expression; `Morph` picks the direction of composition |
| 4. **New carrier + full typeclass instance set** | A new `F[_, _]` plus `AssociativeFunctor[F, X, Y]` and whichever `Forgetful*` typeclasses the operations you want require | A new carrier that's a first-class citizen alongside `Tuple2` / `Either` / `Affine` / `PowerSeries` | Days. Only when the existing carriers genuinely can't express your shape |

Axis 1 + 2 together is the sweet spot for most domain optics —
you use what already works, and you override only the methods
where you've measured a specific cost.

## Worked example: JsonPrism

`cats-eo-circe` ships a `JsonPrism[A]` that edits a leaf inside
circe's `Json` without decoding the enclosing record. It's a
case study for axes 1 and 2 at their most productive.

### The problem

The classical "edit one leaf in a JSON document" pattern is:

```scala
json.as[Person]                                 // decode everything
    .map(p => p.copy(name = p.name.toUpperCase))
    .map(_.asJson)                              // re-encode everything
```

For a forty-field record where you want to upper-case one
string, thirty-nine fields get decoded and re-encoded for no
reason. A cursor-based edit could touch only the leaf — but
circe's `HCursor.set.top` forces a conversion of its internal
`LinkedHashMap`-backed `JsonObject` to a `MapAndVector`
representation per hop, which on the narrow-record benchmark
accounted for ~27% of runtime before the optimisation.

The fix is a concrete Optic subclass that walks the JSON
directly via `Json.asObject` and `JsonObject.add`, bypassing
`HCursor` entirely.

### The design

```scala
final class JsonPrism[A] private[circe] (
    private[circe] val path:    Array[PathStep],   // flat root-to-leaf path
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Optic[Json, Json, A, A, Either]:
  type X = (DecodingFailure, HCursor)              // miss branch carries diagnostics
```

Look at what's picked here and what's not.

- **Carrier.** `Either` — the same one `Prism` uses. No new
  carrier, no new typeclass instances. Every capability
  `Either` already ships — `Accessor`, `ReverseAccessor`,
  `ForgetfulFunctor`, `ForgetfulTraverse`,
  `AssociativeFunctor` — applies verbatim.
- **Composition.** JsonPrism composes with every other optic
  family through the existing `Morph[Either, G]` /
  `Morph[G, Either]` instances. A Lens chain over native
  Scala values can `.andThen` a JsonPrism over Json; a
  JsonPrism can `.andThen` an Optional inside the decoded
  value — the cross-carrier machinery handles both directions,
  without a single line of new `Composer` code in
  `cats-eo-circe`.
- **Storage.** An `Array[PathStep]` — a cheap flat
  representation that copies-and-extends on each `.field(_.x)`
  step. Stored once at construction; no closure allocation
  per call.
- **Existential `X`.** The miss-branch's `(DecodingFailure,
  HCursor)` gives generic-path callers both a diagnostic
  (`DecodingFailure.history`) and the raw original Json
  (`HCursor.top`) so `Optic.from` can forward the input
  unchanged on a miss.

The full code is
[`circe/src/main/scala/eo/circe/JsonPrism.scala`](https://github.com/Constructive-Programming/eo/blob/main/circe/src/main/scala/eo/circe/JsonPrism.scala).

### The fused hot path

The class body shadows the generic Optic extensions with
`inline` methods that walk the path directly:

```scala
inline def modify[X](f: A => A): Json => Json =
  json => modifyImpl(json, f)

inline def transform[X](f: Json => Json): Json => Json =
  json => transformImpl(json, f)

inline def place[X](a: A): Json => Json =
  json => placeImpl(json, a)

inline def reverseGet(a: A): Json = encoder(a)

inline def getOption(json: Json): Option[A] =
  val c = navigateCursor(json)
  c.as[A](using decoder).toOption
```

`modifyImpl` is a pair of `while` loops: walk down collecting
parent `JsonObject`s and `Vector[Json]`s into an
`Array[AnyRef]`, decode the leaf, apply `f`, re-encode, unwind
via `JsonObject.add` / `Vector.updated` (each of which keeps
circe's internal `LinkedHashMap` representation).

Zero `HCursor` allocation. Zero carrier `Either`
materialisation. One `JsonObject.add` per path step on the way
up.

### Two tiers, both correct

A JsonPrism caller binds in one of two ways:

**Concrete binding** — you get the fused code:

```scala
val streetP: JsonPrism[String] =
  codecPrism[Person].address.street

streetP.modifyUnsafe(_.toUpperCase)(json)
//       ^— dispatches to JsonPrism.modifyImpl: the fused path,
//          byte-identical to the pre-v0.2 silent `modify`.
```

The default-Ior equivalent — `streetP.modify(_.toUpperCase)(json)` —
returns `Ior[Chain[JsonFailure], Json]` and surfaces per-step
failures via the observable-by-default surface introduced in v0.2.

**Trait binding** — you get the generic carrier path:

```scala
val streetP: Optic[Json, Json, String, String, Either] =
  codecPrism[Person].address.street

streetP.modify(_.toUpperCase)(json)
//       ^— dispatches to the generic Optic.modify extension,
//          which routes through ForgetfulFunctor[Either] and
//          returns Json => Json (pre-v0.2 silent shape).
```

Both type-check. Concrete binding picks up the Ior-bearing default
or the `*Unsafe` escape hatch; trait binding keeps the generic
extension's `Json => Json` shape. You pick concrete binding when
you care about the hot-path cost or the diagnostic surface;
otherwise the generic extension is there unchanged.

The concrete class's inline overrides don't *replace* the
trait surface — they sit *alongside* it. That's why JsonPrism
still composes with every non-JSON optic through the generic
`.andThen`: the trait's abstract `to` / `from` keep satisfying
the Either-carrier contract that cross-carrier composition
relies on.

## Worked example: JsonTraversal

Some domain optics don't need the full Optic trait integration.
`JsonTraversal` — the multi-focus edit under `.each` — is one
of them.

### Why not PowerSeries

cats-eo's generic multi-focus carrier is `PowerSeries`. You
could express `codecPrism[Basket].items.each.name` as a
PowerSeries-carrier optic — but PowerSeries pays per-element
bookkeeping to support arbitrary downstream composition, and
for the JSON-array case you know the structure: a flat array
under a known prefix, with a fixed per-element suffix.

### The chosen shape

```scala
final class JsonTraversal[A] private[circe] (
    private[circe] val prefix:  Array[PathStep],   // root → array
    private[circe] val suffix:  Array[PathStep],   // element → leaf
    private[circe] val encoder: Encoder[A],
    private[circe] val decoder: Decoder[A],
) extends Dynamic                                  // NOT Optic
```

`JsonTraversal` does **not** extend `Optic`. It has its own
surface — `modify`, `transform`, `getAll` — and its own path
walker that traverses the prefix once, maps the array, and
unwinds. For a traversal whose only integration surface is
"call me from a JsonPrism via `.each`" and whose exit point is
a normal `Json`, stepping outside the Optic trait is the
correct call: you pay zero overhead for a contract you weren't
going to use.

This is axis 4 — a new domain-specific carrier — but the
"carrier" here is JSON-specific and doesn't need to integrate
with `AssociativeFunctor` or `Morph` because the use site is
closed. The handoff from JsonPrism to JsonTraversal happens at
`.each`, which is a plain method, not an `Optic.andThen`.

So the library-design principle is: **extend as little as you
need**. The JsonPrism path goes through axis 1+2 for full
Optic integration. The JsonTraversal path goes off-piste
because nothing downstream of it needs the integration.

## Measurable outcome

Both domain optics deliver the expected constant factor over
the decode / re-encode baseline —
[`JsonPrismBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsonPrismBench.scala)
lands at ~2× across depths 1/2/3 and ~1× at the "wide"
(28-field) shape where the naive decoder already touches
every field;
[`JsonTraversalBench`](https://github.com/Constructive-Programming/eo/blob/main/benchmarks/src/main/scala/eo/bench/JsonTraversalBench.scala)
holds ~2× across array sizes 8 / 64 / 512. See the
[Benchmarks](benchmarks.md) page's JsonPrism and JsonTraversal
sections for the full tables.

## When to reach for this pattern

**Yes**, if all four of these are true:

- You have a measurable hot path where the generic carrier
  round-trip is a real cost.
- Your domain has a representation you can walk cheaper than
  the carrier does. (JSON's `JsonObject`, a binary format's
  `ByteBuffer`, a typed record walked via reflection ahead of
  time, etc.)
- You want callers to stay inside the Optic universe — be
  able to `.andThen` your optic with a Lens, a Prism, a
  traversal — without bridge code.
- The performance delta justifies the code you're about to
  write.

**No**, or **not yet**:

- It's a one-off utility. Just write a function.
- The hot path isn't measured. Profile first, or you'll
  over-specialise.
- Your optic's shape doesn't match any existing carrier. That
  points to axis 4 (new carrier), which is heavier than
  JsonPrism's story — make sure axis 1 + 2 don't cover it
  first.

## See also

- [Concepts](concepts.md) — what a carrier is and which
  typeclasses unlock which operations.
- [Circe integration](circe.md) — how to *use* the
  JsonPrism / JsonTraversal optics described here as a design
  case study.
- [Benchmarks](benchmarks.md) — measured numbers for both
  JsonPrism and JsonTraversal against the naive decode /
  re-encode baseline.
