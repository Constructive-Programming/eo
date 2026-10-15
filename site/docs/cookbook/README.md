# Cookbook

Optics are probably the most powerful unused tool in our arsenal —
this section is the working proof. Runnable patterns for the questions
that come up most often, one page for each of the three jobs optics do
best:

1. **[Navigating structures](navigating-structures.md)** — reach one
   branch of a sum, every node of a recursive tree, several fields
   at once, or a field inside bytes you never decode. One `.andThen`
   surface for all of it.
2. **[Decoupling and modularity](decoupling.md)** —
   replace a direct dependence on a data type with an optic: depend
   on the *relationship*, ask for the *weakest capability* you need,
   and let independent modules interoperate at the call site.
3. **[Effect threading](effect-threading.md)** — turn control flow
   into type definitions: the optic pins *where*, the effect type
   decides *what happens* — abort, accumulate, audit, defer.

Two demos open the section: whole programs, with their own tests and
benchmarks, that put these recipes to work.

- **[Allocation-free stream triage](grpc-filter.md)** — an fs2 filter
  that classifies and filters a gRPC stream of 64-field JSON messages
  straight off a socket, reading the few values it needs with
  eo-jsoniter instead of decoding each message.
- **[Blockchain smart contract compilation](uplc-jvm.md)** — a compiler
  from Cardano's Plutus Core to JVM class files, built as a chain of
  lawful prisms and an iso, and checked against the reference
  evaluator on every program.

The framing follows Hansen — *We Need More Optics*
(<https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468>) and
Penner — *Optics By Example*
(<https://leanpub.com/optics-by-example/>). These are worked
examples, not a tutorial — for the optic families themselves see the
[Optics reference](../optics.md). Every fence is compiled by mdoc
against the current library version, and every recipe cites its
source.

If you arrive with a task rather than an optic in mind, start here:

| I want to…                                                  | Recipe |
|-------------------------------------------------------------|--------|
| edit one branch of a sum type, misses passing through       | [Visit contingent fields](navigating-structures.md#visit-contingent-fields) |
| apply one edit at every node of a recursive tree            | [Visit across whole trees](navigating-structures.md#visit-across-whole-trees) |
| update only the matching elements of a collection           | [Visit through arbitrary structure](navigating-structures.md#visit-through-arbitrary-structure) |
| fold several fields into one number                         | [Isolate what you need](navigating-structures.md#isolate-what-you-need) |
| rewrite every slot of a fixed shape, or summarise a batch   | [Compute aggregations](navigating-structures.md#compute-aggregations) |
| update a grid coordinate or preserve device labels           | [Indexed: Grates and Glasses](../indexed.md) |
| change a field deep in JSON without decoding the payload    | [Edit JSON without decoding](navigating-structures.md#edit-json-without-decoding) |
| depend on a relationship instead of a data type             | [Require the optic, not the type](decoupling.md#require-the-optic-not-the-type) |
| ask for the weakest capability a function needs             | [Depend only on what's needed](decoupling.md#depend-only-on-what-s-needed) |
| edit Avro bytes on a hot path, no decode                    | [Serdes free pipes](decoupling.md#serdes-free-pipes) |
| move one field between wire formats (Avro ↔ JSON)           | [Inspectable cross-format bridges](decoupling.md#inspectable-cross-format-bridges) |
| law-test the optics my module publishes                     | [Re-usable laws for testing](decoupling.md#re-usable-laws-for-testing) |
| make an in-place update fallible                            | [Validate-in-place with `modifyF`](effect-threading.md#validate-in-place-with-modifyf) |
| swap abort / accumulate / audit without touching the optic  | [Structure orthogonal to effects](effect-threading.md#structure-orthogonal-to-effects) |
| batch N+1 lookups through one traversal                     | [Batch-load nested IDs](effect-threading.md#batch-load-nested-ids) |
| stamp an effect's result back into the entity               | [Persist-and-stamp decode free](effect-threading.md#persist-and-stamp-decode-free) |

## Further reading

- [Concepts](../concepts.md) — the theory behind the unified Optic
  trait and carriers; the
  [composition lattice](../concepts.md#composition-lattice)
  diagram maps out every `Composer[F, G]` bridge.
- [Optics reference](../optics.md) — the full per-family tour,
  introduced by the
  [family taxonomy](../optics.md#family-taxonomy) diagram.
- [Generics](../generics.md) — macro-derived `lens[S](...)` and
  `prism[S, A]`.
- [Circe integration](../integrations/circe.md) — cursor-backed JSON optics;
  [failure flow](../integrations/circe.md#failure-flow) for the Ior decision
  tree.
- [Avro integration](../integrations/avro.md) — cursor-backed Avro optics;
  [failure flow](../integrations/avro.md#failure-flow) for the schema-driven
  Ior decision tree.
- [Migrating from Monocle](../migration-from-monocle.md) —
  side-by-side translation guide.
- Hansen — *We Need More Optics*, The Startup,
  <https://medium.com/swlh/we-need-more-optics-8ddf1d2d9468> — the
  article this section's three pages follow.
