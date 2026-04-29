---
name: cats-eo
description: Use cats-eo (existential optics for Scala 3) to specify pinpointed read/write operations on nested case classes, JSON byte buffers, Avro records, or circe ASTs. One Optic[S, T, A, B, F] trait, several carriers, uniform .get/.modify/.replace/.foldMap/.andThen surface.
---

# cats-eo skill

Fast-decision reference for an agent using cats-eo to get work done.
Pair with `site/docs/optics.md` (full tour) and `CLAUDE.md` (toolchain).

## What cats-eo is in one paragraph

cats-eo is an **existential optics** library for Scala 3. Every optic
family (Lens, Prism, Iso, Optional, Traversal, Setter, Getter, Fold,
AffineFold) is the same `Optic[S, T, A, B, F[_, _]]` trait
parameterised by a *carrier* `F`. The carrier picks how the focus is
reached and rebuilt; everything else (read/write/compose) is uniform.
You name the focus and the operation; the carrier implements the
plumbing. One side of the mirror, agent-supplied; the other,
carrier-supplied.

## Module decision tree

```
Source/target shape:
  Case classes (in-memory)      → core + generics
  circe Json AST                → cats-eo-circe
  Apache Avro IndexedRecord     → cats-eo-avro
  Raw JSON Array[Byte]          → cats-eo-jsoniter
  Scala collection (List, ...)  → core (Traversal.each)
```

## Optic taxonomy + carriers

| Family       | Carrier              | Use when                                     |
|--------------|----------------------|----------------------------------------------|
| Iso          | `Forgetful`          | Bijection between two reps                   |
| Lens         | `Tuple2`             | Always-present field of a product            |
| Prism        | `Either`             | One branch of a sum                          |
| Optional     | `Affine`             | Conditionally-present focus                  |
| AffineFold   | `Affine` (T=Unit)    | Read-only "this might not be there"          |
| Getter       | `Forgetful` (T=Unit) | Pure projection                              |
| Fold         | `Forget[F]`          | Sum / count / search over `Foldable[F]`      |
| Traversal    | `MultiFocus[PSVec]`  | Modify every element of a collection         |
| Setter       | `SetterF`            | Write-back without observing the focus       |
| MultiFocus[F]| `MultiFocus[F]`      | Custom multi-focus shapes                    |

Reference pages: `site/docs/optics.md`, `site/docs/multifocus.md`.
Composition matrix: `docs/research/2026-04-23-composition-gap-analysis.md`.

## Recipe pack

### A1. Nested case-class field

```scala
import dev.constructive.eo.optics.Optic.*
import dev.constructive.eo.generics.lens

case class Address(street: String, zip: Int)
case class Person(name: String, age: Int, address: Address)

val streetL = lens[Person](_.address).andThen(lens[Address](_.street))
streetL.modify(_.toUpperCase)(alice)
```

`lens[S](_.field)` works on case classes, Scala 3 enums, and union
types. For sum types use `prism[S, A]`.

### A2. Optional / predicate-gated

```scala
import dev.constructive.eo.data.Affine.given
import dev.constructive.eo.optics.Optional

case class Contact(email: Option[String])
val emailOpt = Optional[Contact, Contact, String, String, Affine](
  getOrModify = c => c.email.toRight(c),
  reverseGet  = { case (c, s) => c.copy(email = Some(s)) },
)
```

For predicate reads: `Optional.readOnly(p => Option.when(...)(...))`.

### A3. Traversal over a collection

```scala
import dev.constructive.eo.data.MultiFocus.given
import dev.constructive.eo.optics.Traversal

val each = Traversal.each[List, Int]
each.foldMap(identity)(List(1, 2, 3))    // 6
each.modify(_ + 1)(List(1, 2, 3))        // List(2, 3, 4)
```

### A4. circe JSON AST edit

```scala
import dev.constructive.eo.circe.codecPrism

val streetP = codecPrism[Person].address.street
streetP.modifyUnsafe(_.toUpperCase)(json)
```

`.address.street` sugar is macro-driven. `.fields(_.a, _.b)` for
multi-field NamedTuple, `.each` for arrays. Default surface returns
`Ior[Chain[JsonFailure], Json]`; `*Unsafe` is silent pre-v0.2.

### A5. JSON byte-buffer edit (no AST)

```scala
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec
import dev.constructive.eo.data.Affine.given
import dev.constructive.eo.jsoniter.JsoniterPrism

given JsonValueCodec[Long] = JsonCodecMaker.make
val idP = JsoniterPrism[Long]("$.payload.user.id")

idP.foldMap(identity[Long])(bytes)   // ~50 ns/op (16× eo-circe)
idP.replace(99L)(bytes)              // ~100 ns/op (14× eo-circe)
```

Wildcards `[*]` route through `JsoniterTraversal[A]`. Path subset is
`$`, `$.foo`, `$[i]`, `$[*]`, dotted chains; no filters / recursive
descent.

### A6. Avro record edit

```scala
import dev.constructive.eo.avro.codecPrism
val nameP = codecPrism[Person].name
nameP.modifyUnsafe(_.toUpperCase)(record)
```

### A7. Cross-family compose

```scala
val pageP =
  lens[Person](_.address)
    .andThen(emailOpt)
    .andThen(prism[String, Refined])
```

`.andThen` lifts via auto-summoned `Composer[F, G]`. Lens × Prism =
Optional; Optional × Traversal = Traversal; any read-only chain
lands on Fold.

## Pitfalls

### "No given instance of type ForgetfulFunctor[…]" / "ForgetfulFold[…]"

Missing carrier import. Add the right `.given`:

| Carrier            | Import                                            |
|--------------------|---------------------------------------------------|
| `Forgetful`        | `import dev.constructive.eo.data.Forgetful.given` |
| `Forget[F]`        | `import dev.constructive.eo.data.Forget.given`    |
| `Affine`           | `import dev.constructive.eo.data.Affine.given`    |
| `MultiFocus[F]`    | `import dev.constructive.eo.data.MultiFocus.given`|
| `SetterF`          | `import dev.constructive.eo.data.SetterF.given`   |

`Tuple2` / `Either` instances are auto-discovered.

### "No Composer[F, G]" on `.andThen`

Some carrier pairs are deliberately U (structurally absent). Either
lift one side via `.morph[F]`, restructure the chain, or check the
matrix. Read-only chains often need `Forget[F]` as terminus — drop
the write side first.

### Match-type errors on `Snd[X]` / `Fst[X]`

X is path-dependent and abstract. Either destructure inside the
optic factory where X is concrete, or refine via a fresh ascription
that pins X to a concrete Tuple2 shape.

### "applyDynamic is not a member" on JsonPrism

The macro-driven `.field.field` sugar lives on `JsonPrism` /
`JsonFieldsPrism` / `JsonTraversal`, not on the Optic-trait return
type. Ascribe to the concrete class to keep the sugar firing.

## Build / test commands

```sh
sbt compile                                # full project compile
sbt test                                   # full root-aggregate test
sbt "<module>/test"                        # one module
sbt scalafmtCheckAll                       # CI-style format check
sbt scalafmtAll                            # in-place format
sbt "docs/mdoc"                            # compile every mdoc:silent block
sbt "docs/tlSitePreview"                   # serve the Laika site
sbt "benchmarks/Jmh/run -i 5 -wi 3 -f 3 .*<Pattern>.*"   # JMH bench
```

Pre-commit runs `scalafmtCheckAll` + `docs/mdoc` + `docs/laikaSite`;
pre-push runs `sbt test`. Don't `--no-verify` unless asked.

## Structural-change checklist

1. Update `core/src/main/scala/dev/constructive/eo/`.
2. Update or add a behaviour spec in
   `tests/src/test/scala/dev/constructive/eo/OpticsBehaviorSpec.scala`.
3. Update or add a discipline law spec.
4. New Composer / AssociativeFunctor / typeclass instance? Update
   `docs/research/2026-04-23-composition-gap-analysis.md`.
5. Update `site/docs/optics.md` (or relevant module page) with a
   runnable mdoc example.
6. `sbt scalafmtAll; tests/test; docs/mdoc` before commit.

## New-integration-module checklist

**Reuse an existing carrier** — adding a new one costs ~40 matrix
cells. Affine for read-write Prism; `MultiFocus[PSVec]` for
traversal; `Forget[F]` for read-only fold.

The eo-jsoniter design spike at
`docs/research/2026-04-29-eo-jsoniter-spike.md` documents the
methodology end-to-end:

- Reuse Affine and `MultiFocus[PSVec]`; ship zero new typeclass
  instances.
- Phase the work: read-only Prism → Traversal → write-back splice.
  Each phase is hours, not days, when the carrier reuse cuts the
  typeclass + matrix tax to zero.
- Bench-gate phase 2: ≥3× on read-scalar `.foldMap` greenlights
  write-back; <1.5× abandons. eo-jsoniter clears at 16×.

## External lookups

```sh
cellar get-external org.typelevel:cats-core_3:2.13.0 cats.Monad
cellar list-external org.typelevel:cats-core_3:2.13.0 cats.data
cellar search-external org.typelevel:cats-core_3:2.13.0 Traverse
```

For project-local symbols use the `metals` MCP server already wired
in `.mcp.json`. See `CLAUDE.md` for the full toolchain reference.
