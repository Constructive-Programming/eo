# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [0.17.0] - 2026-09-23

**Binary-breaking: recompile against this release.** Two public signatures changed shape —
`cats-eo-jsoniter`'s string-path constructors now return `Either[String, …]`, and
`cats-eo-avro`'s `AvroVulcan.codec[A]` (the `using`-form) now returns
`Either[Exception, AvroCodec[A]]` — so jars built against 0.16.x fail at runtime against this
one. Both carry their migration under Changed.

### Added

- **`cats-eo-avro`: the derived whole-record builder — `AvroVulcan.recordBuilder` (#95)**:
  `A ⇒ GenericData.Record`, leaf by leaf, at hand-built cost with zero hand-maintained lines. The
  macro walks `A`'s case fields at expansion into a plain runtime `RecordShape` IR and emits ONE
  assembly call; construction resolves every case field's schema slot by NAME (all-or-nothing, the
  #105 doctrine via a new total `AvroWalk.recordSlots` rung) and validates every arm against the
  schema it writes into — so `toRecord` is pure positional puts. Per field: primitives put the value
  itself, nested case classes RECURSE into a sub-record level (the piece the positional builder the
  filer benchmarked and rejected lacked — theirs re-entered `Codec[Sub].encode`, keeping vulcan's
  per-sub-record composition, which measured 7.5x time / 16.9x allocation on the real nested
  ClickInfo), `None` puts null exactly as vulcan's `OptionCodec`, and everything else (enums, bytes,
  logical types, collections, sums, value classes) falls back to the field type's own
  `vulcan.Codec` — summoned at the derivation site, so a missing leaf codec is a compile error
  naming the field. Construction is TOTAL (`Exception | WholeRecordBuilder[A]`; `AvroWalk.recordSlots`
  returns the failure instead of throwing), self-recursive case classes terminate through the runtime
  level chain, and the one documented difference from `codec.encode` is schema-only columns
  (computed/derived fields keep their in-record default — the hand-built `.put` contract,
  round-trip-safe through the codec's decode). `WholeRecordBuilder.asAvroCodec` installs it as an
  `AvroCodec.encode` in one line (`given AvroCodec[ClickInfo] =
  clickBuilder.fold(e => throw e, _.asAvroCodec)`), keeping drilled `.field` reads and
  `codecPrism[...].record.reverseGet` on the same fast path. The bridge also gains the doctrine's
  shape overall: `AvroVulcan.codec` is two forms — `codec(schema)` (total; schema in hand) and
  `codec[A]: Either[Exception, AvroCodec[A]]` (resolves from the codec) — with no naked throws on
  any construction path; the opt-in given stays the one documented eager-failure site. Allocation
  gate: `benchmarks` `ClickRecordBench` (66-leaf nested ClickInfo) — derived 744 B/op vs hand-built
  768 vs the rejected positional 23,912 vs full codec 28,408.

### Changed

- **`cats-eo-jsoniter`: the string-path constructors now keep the failure in the type**
  (source- and binary-breaking). `JsoniterPrism.fromPath` and
  `JsoniterTraversal.fromPath` return `Either[String, …]` — a JSONPath is DATA (a config file,
  a CLI argument, a registry lookup), so an unparseable one is a value you handle rather than
  an exception you catch. The throwing `JsoniterPrism.fromPath` is **removed**, and with it
  `JsoniterTraversal.apply(path)`; a path-free root prism is still `JsoniterPrism[A]`. Migration:
  `JsoniterTraversal[A](path)` → `JsoniterTraversal.fromPath[A](path)`, then handle the `Left`
  (`JsoniterPrism.fromPath[A](path)` likewise).

- **`cats-eo-avro`: `AvroVulcan.codec` is two forms now** (source- and binary-breaking for the
  `using`-form). `codec(schema)` is total — the schema is in hand, nothing resolves, nothing can
  fail; `codec[A]` resolves the schema from the in-scope `vulcan.Codec` and returns
  `Either[Exception, AvroCodec[A]]` instead of throwing at construction, so the failure is a
  value at the call site like every other construction failure in this module. Migration:
  `AvroVulcan.codec[A]` → `AvroVulcan.codec[A].map(…)`, or hand it a schema you already hold via
  `codec(schema)`. Unchanged in shape: the opt-in `import dev.constructive.eo.avro.vulcan.given`,
  which still yields a total `AvroCodec[A]` and is now named as the one eager-failure site (a
  given must produce a value, so an unresolvable schema fails there).

- **`cats-eo-circe`'s fused JSON write walk no longer uses a control-flow exception.**
  `JsonWalk.modifyPath` takes `f: Json => WalkResult` (`JsonFailure | Json` — a union, not an
  `Either`, so no splice frame boxes anything) and short-circuits on the failure arm; the private
  `ControlThrowable` non-local exit (`MissSignal` / `miss`) is gone, and `JsonFocus` /
  `JsonTraversal` return their failures instead of raising them. No public behaviour change, and
  no allocation cost: measured on the depth-3 `OrderCirceBench.eoStreet` write shape, the hit
  path is unchanged at 1080 B/op and the miss path drops 80 → 40 B/op (the old design allocated a
  `MissSignal` Throwable). Full inventory and verdicts for the rest of the tree:
  `docs/research/2026-09-22-exception-audit.md`; the write-side silent pass-through that the same
  audit pins as intended behaviour is tracked as
  [#117](https://github.com/Constructive-Programming/eo/issues/117).

## [0.16.0] - 2026-09-18

### Added

- **`cats-eo-zio` grows the ZIO-ecosystem seams** (#92): the kyo-expansion playbook replayed
  across ZIO. `Chunks.each` / `at` / `eachNonEmpty` are the collection legs, built as
  constructors over PRIVATE `Traverse` adapters (the orphan given belongs to zio-interop-cats).
  Three `% Optional` sub-packages follow the avro/circe pattern — the caller adds the artifact,
  and this release names which: `dev.zio %% zio-schema % 1.7.5`, `dev.zio %% zio-json % 0.7.44`,
  `dev.zio %% zio-prelude % 1.0.0-RC41`. All three pin zio `2.1.19`–`2.1.21` transitively and the
  module's own direct `2.1.24` wins within the 2.1.x line, so adding one does not drag the runtime
  backwards. **`eo.zio.schema`** fills zio-schema's own `AccessorBuilder` extension point with
  `EoAccessorBuilder` (a Lens per record field, a Prism per enum case, a Traversal per
  collection, no macros on our side), adds `DynamicValues` — the untyped-tree kit over
  `DynamicValue` (constructor prisms, a generic `primitive(st)`, `field` / `at` / `key` /
  `variant` / `each` navigation, `Plated`) — plus `schema.dynamicPrism` typed ↔ untyped and
  `BinaryCodec.prism`, one byte face covering json / protobuf / avro / msgpack / thrift;
  **`eo.zio.json`** ports the circe playbook to `zio.json.ast.Json` (`JsonValues` prisms,
  navigation, `Plated`, a `text` String ↔ Json face, `JsonCodec.stringPrism`) and bridges
  zio-json's own typed paths through `JsonCursor.optional`; **`eo.zio.prelude`** adds
  `Validations.success` (an `Optional`, not a Prism — a prism-shaped write would drop the log)
  and `eachFailure`, an error-polymorphic traversal over the accumulated `NonEmptyChunk`.
  `TRef` / `TMap` focus ops return `USTM`, so focused updates across several transactional
  references compose into ONE atomic transaction — something the `Ref` ops structurally cannot
  express; the `TMap` ops are named `-At` because mixed-shape extension overloads break explicit
  `(using myLens)` calls. On the `Ref` side, **`Ref.Synchronized.updateFocusZIO`** adds the one op a
  plain `Ref` structurally cannot host — an EFFECTFUL focus rewrite — by routing `CanModifyA`
  through ZIO's own `updateZIO`, so the effect runs while the ref is held and the whole
  read-modify-write stays atomic. `CanModifyA` is the affine/many-focus face, so it covers a lens, a
  prism (a miss means the effect never runs) and a traversal (one effect per focus, sequenced left
  to right). The `Applicative[ZIO[R, E, *]]` is YOURS to supply — it lives in zio-interop-cats, and
  this module still declares no effect instances of its own.
- **Two core compositions the ZIO tree kits needed and the zoo could not name** (#92), both in
  `cats-eo` itself. **`MultiFocus.zipWith(fa, fb)(f)`** — and its `zip` pairing case — combines two
  containers POINTWISE, which is a Grate's reason for existing and the one operation
  `MultiFocus.representable` made possible but never exposed. A `Traversal` structurally cannot
  express it: it visits one focus at a time with no access to a second structure, and neither can
  `modify`. `Representable[F]` is exactly what makes it total — read both containers as index
  functions and tabulate their pointwise combination, so there is no shape to mismatch and every
  representation point exists in both; `zipWith(fa, fa)(f) == F.map(fa)(a => f(a, a))`. And the
  **fused `Prism.andThen(Traversal)` / `PickMend.andThen(Traversal)` overloads, with their
  `Traversal.andThen(Prism)` mirror** — the filtering composition, `traversal.andThen(shapePrism)`
  being "every element that is a Circle". A prism MISS contributes ZERO foci (its element is
  rebuilt untouched from the prism's leftover) and a hit contributes the inner traversal's. The
  generic `Morph`-routed extension already typechecked, but yielded an anonymous
  `Optic[…, MultiFocus[PSVec]]` — not nameable as a `Traversal` val, and without Traversal's fused
  `modify` / `replace` / streaming `foldMap`. That is why the untyped-tree kits had been
  hand-rolling `each` through `Traversal.selfChildren` with a `case other => other` arm
  re-encoding the prism's miss branch by hand.
- **`cats-eo-laws` gains a law family for navigation optics** (#92): `NavigationLaws[S, A]` plus the
  `NavigationTests[S, A]` discipline rule set (`"Navigation"`). Every untyped-tree kit ships the
  same `field(name)` / `at(i)` / `key(k)` / `variant(name)` shape and promises the same contract,
  which no existing suite states — so each kit re-asserted it by hand, and drift between them is
  exactly how a bug survives in one kit after being fixed in another. Two equations on top of
  `OptionalLaws` / `SeamLaws`. **put-get**: `OptionalLaws` deliberately omits it (Monocle parity),
  but for a KEYED navigation optic it is the law that pins WHICH occurrence a write targets — an
  optic that reads the first duplicate key and writes all of them satisfies it, one that reads the
  first and writes the LAST does not. **A write through a miss is the identity**: every kit
  documents "misses pass writes through untouched" and nothing checked it, so a carrier that
  INVENTS the missing node — appending an absent field, growing a sequence to reach an
  out-of-range index — went unnoticed, which is the real hazard for any kit tempted to make `field`
  an upsert. Run them on a DRILLED optic and with a generator that actually produces misses; with
  hits only the second law is vacuous. Equality is injected for the same reason `SeamLaws` injects
  it — some carriers' `S` has no lawful universal `==`, and some have one that is not even
  reflexive (zio-json's `Json.Obj` maps the left operand before comparing). Registered here by the
  kyo `StructureValues`, zio `DynamicValues` and zio `JsonValues` suites; published so downstream
  kits can register it too, which is the whole point of hosting it in `cats-eo-laws` rather than in
  one module's tests.
- **`atField` — At-style create/delete access, in all three untyped-tree kits** (#92):
  `StructureValues.atField` (kyo `Structure.Value`), `DynamicValues.atField` (zio-schema
  `DynamicValue`) and `JsonValues.atField` (zio-json `Json`). The focus is the `Option[…]` AT the
  name, so a write can create or delete: `Some(v)` updates the first occurrence and appends when
  absent, `None` removes every occurrence (every one, so a read after a delete cannot find a
  leftover twin). `field` structurally cannot do either — its focus is the value, so an absent
  field is a MISS and misses pass writes through. Partial only in "is this a record / object";
  within one it is total, presence living in the focus, so `getOption` yields `Some(None)` for a
  record lacking the field. **Lawful up to field ORDER, and no further** — this is stated rather
  than glossed. put-get, get-put and the miss contract hold unconditionally, but put-put does NOT:
  `replace(None)` destroys the field's position, so a following `Some(v)` appends where a direct
  `Some(v)` would have updated in place. Deletion cannot preserve a position that no longer exists,
  so it is inherent to At-style access over an ORDERED record — Monocle's `At` escapes it only
  because `Map` has no order. The kyo kit registers `NavigationTests` for `atField` and
  deliberately NOT `SeamTests`, whose `seamReplaceOverwrite` IS put-put and which the generator
  falsifies on sight — `Record((f, …), (k0, …))`, `None` then `Some(Integer(-510))`: same fields,
  different ORDER. The zio twins register `OptionalTests` / `SeamTests` / `NavigationTests` and
  pass all three, but only because their equalities ignore order (a `ListMap` compares as a `Map`,
  `Json.Obj.equals` normalises the left operand) — the BEHAVIOUR is identical there and only the
  visibility differs. If you re-encode a tree after a delete-then-insert, expect the key order to
  have moved.
- **Optic constructors from cats typeclasses, on the optic companions** (#91): three new bridges —
  `Traversal.first` / `second` / `both` (`Bitraverse` slot traversals over Either, Tuple2, Ior,
  Validated), `Modify.functor` (`Functor` — write-only, because `map` alone cannot read) and
  `Lens.representable(r)` (a lawful positional Lens at one representation point: tabulate to
  rebuild, siblings read back from the original — the one optic no whole-container bridge can
  produce). They join the bridges cats-eo already shipped and did not change here —
  `Traversal.each` (`Traverse`), `Fold[F, A]` (`Foldable`) and `MultiFocus.representable`
  (`Representable`) — so the companions now carry the set as one surface. New and old alike are
  CONSTRUCTORS, not givens: a cats container admits several lawful optics at once over the same
  `(F[A], A)` pair, so no single one can be canonical — clients declare their own given (bind a
  constructor, or write a direct SAM `Can*` instance). The `Comonad` ⇒ Getter and `Applicative` ⇒
  Review bridges are deliberately absent; the
  [Capabilities](https://eo.constructive.dev/capabilities.html) page
  (`site/docs/capabilities.md`) says why — they are left as exercises, and `Applicative` in
  particular belongs as a direct `CanReverseGet` given rather than a `Review` optic given.
- **`.fields(...)` no longer stops at 22 selectors** (#96): the macro-synthesised
  `NamedTuple` focus no longer has a *spelling* ceiling. Up to 22 selectors the value tuple is spelled
  `scala.TupleN` and hearth builds it with that tuple's constructor; at 23 and above the value
  tuple *is* a `*:` cons chain, and hearth 0.4.2 builds that through `Tuple.fromArray`. Before,
  hearth 0.4.0 called the cons chain's primary constructor — which takes no value parameters — so
  every 23+-selector `.fields` failed to compile with `wrong number of arguments at inlining …
  expected: 0, found: N`. Verified end-to-end (derive, encode, decode, positional read-back) at
  arity 23 and at arity 40 with mixed field types, since the `Tuple.fromArray` path boxes every
  element to `Object`. Both spellings remain load-bearing: hearth's sub-23 branch still cannot
  build a cons chain, so `MacroSelectors.tupleTypeOf` keeps emitting `TupleN` below 23
  permanently. **Two ceilings remain and are now documented rather than glossed:** 254 selectors
  is hard and permanent (`.fields` selects from a case class, and the JVM caps a parameter list at
  254 slots — a 255-field case class does not compile at all), and well below that the binding
  limit is the compiler thread's stack, since the derivation recurses per field. Measured on a
  full-cover probe, varying only `-Xss`: 1m (the JVM default) derives 32 and overflows at 36; 2m
  derives 66; 4m (sbt's launcher default) derives 150; 8m — this repo's `.jvmopts`, and the only
  reason its own suites reach 254 — derives 254. A downstream build inherits none of that, because
  the published artifact cannot carry an `-Xss`, so a wide `.fields` may need `-Xss` raised in the
  *consumer's* build.

### Changed

- **Behaviour change, `cats-eo-kyo`: `StructureValues.field` and `key` now write the FIRST match,
  not every match** (#92). Both read the first match and always did; the write used to update
  EVERY entry with that name (`fields.map((n, v) => if n == name then (n, b) else (n, v))`) and now
  updates only the one the read found (`indexWhere` + `updated`). Schema-produced records carry
  unique names, but `Value.Record` and `Value.MapEntries` are `Chunk`-backed, so duplicates are
  representable — and a whole-tree `platedValue` rewrite can even manufacture equal map keys. On
  such a value the old read-first/write-all pairing broke `modify(identity)`: writing back the
  value it had just read overwrote the twin as well, failing `OptionalLaws.modifyIdentity` and
  `SeamLaws.seamModifyIdentity`. This is a lawfulness fix, not a preference — a write-all optic is
  a Traversal, not an `Optional`, and these are `Optional`s. If you were relying on the old
  behaviour to fan a value out across duplicate names, reach for a traversal instead.
- **Dependency bump**: hearth `0.4.0` → `0.4.2` and kindlings-{avro,cats,circe}-derivation
  `0.3.0` → `0.3.2` — the releases that carry the NamedTuple arity fix above (hearth #313/#314,
  landed in 0.4.1). Transitively: apache-avro `1.12.1` → `1.12.2` (the explicit pin moves with it
  rather than silently downgrading the transitive), jackson-core/-databind `2.21.5` → `2.22.1`,
  jackson-annotations `2.21` → `2.22`, slf4j-api `2.0.17` → `2.0.18` (commons-lang3 stays at
  `3.18.0` — see the override removal below; commons-compress stays at `1.28.0`). No
  derived Avro schema text changed: record names, field names and field order are byte-identical
  across the bump (the 216-example avro suite, including every naming and union spec, passes
  unmodified).
- **Downstream consumers of `cats-eo-avro` move off a vulnerable jackson.** Resolving what the
  published POM actually declares, a consumer previously landed on **jackson-databind 2.20.0 — 5
  OSV advisories, 2 of them HIGH** (PolymorphicTypeValidator bypass via generic type parameters,
  array-subtype allowlist bypass, InetSocketAddress SSRF, per-property `@JsonIgnoreProperties`
  bypass, `@JsonIgnore` bypass on records). With avro 1.12.2 they now land on **2.22.1, which has
  none**. This was never covered by eo's `dependencyOverrides`: sbt's overrides are *build-local*
  and emit no `<dependencyManagement>`, and eo's published POMs carry none, so downstream always
  resolved jackson through avro's own parent BOM. eo's CI meanwhile compiled against the
  overridden version and `dependency-submission` reported it, which is why the exposure never
  raised an alert here.
- **The jackson / commons-lang3 `dependencyOverrides` are removed.** They protected no consumer
  (above) and had become a hazard: only two thirds of the BOM-managed jackson trio was ever
  overridden (`jackson-annotations` was not), so the next `jackson-bom` lift inside avro-parent
  would have moved annotations alone while core/databind stayed frozen. Measured, the jackson half
  is now a pure no-op — the avro compile classpath is jar-for-jar identical with and without it.
  Dropping the commons-lang3 override moves this build 3.20.0 → **3.18.0**, which is exactly what
  a consumer resolves (commons-lang3 arrives via `commons-compress 1.28.0`, whose POM declares
  3.18.0; avro-parent's 3.20.0 property manages only avro's own direct dependencies). 3.18.0 is
  the first release patched against CVE-2025-48924 and carries zero OSV advisories. The build now
  resolves avro's transitives exactly as consumers do, so `dependency-submission` reports the real
  consumer graph instead of a locally-sweetened one.
- **The kindlings macro-expansion timeout actually takes effect now.** The build spelled it
  `-Xmacro-settings:{circe,cats,avro}Derivation.timeout=30`, but kindlings parses that value with
  `^\s*(\d+)\s*(ms|millis|milliseconds|s|seconds?|m|minutes?)\s*$` and falls back to its 5 s
  default on no match — so a bare integer was silently discarded and every derivation had been
  running on 5 s, not 30 s. Verified by A/B: `=30` behaves byte-for-byte like passing no setting
  at all, while `=30s` raises the budget. All spellings gain the unit suffix (`=30s` build-wide,
  `=120s` for mdoc fences), which is what finally mitigates the recurring "derivation timed out"
  CI flake the setting was added for.
- **Behaviour change, avro**: a hand-written codec that PERMUTES the Scala names (writes case field
  `a` into a schema field literally named `b`, and vice versa) resolved correctly by position and
  now resolves by name, i.e. wrongly. No name transform can produce that shape — a transform is a
  function of the name alone — but a hand-written field list can. Use `.fieldNamed` there.
- **Recompile, do not re-jar**: the avro resolution signatures are `private[avro]`, but
  `transparent inline` bakes the accessor into CALLER bytecode, so downstream projects must
  recompile against this release rather than swapping the jar.

### Fixed

- **`.field(_.x)` no longer targets the wrong schema field when the codec's schema is not
  positionally 1:1 with the case class** (#95): resolution was `fields.get(declIdx).name` and
  nothing else — the field's NAME, its TYPE and the record's ARITY were never consulted. Sound for
  every kindlings-derived codec (1:1 by construction), unsound for a hand-written or `vulcan.Codec`
  field list, which can add a computed column, drop one, or reorder. On those, `.field(_.b)` against
  a schema `{a, computed, b, c}` read and wrote `computed`, producing **valid Avro bytes with wrong
  content** — and no law could see it, because a mis-targeted optic is a perfectly lawful `Optional`
  onto the wrong field. The same resolver backs `.fields`, `selectDynamic`, `.each.field` and
  `.each.fields`, so all six sites were affected. Resolution now tries a NAME rung first, and only
  when the codec has named the WHOLE case-field list onto distinct schema fields (total and
  injective, exact or up to `_`/`-`/`.` and case); otherwise it abstains and declaration position
  decides exactly as before — which is what keeps every name-transform codec (issue #35's
  population) resolving correctly. Still construction-time only: zero per-operation cost.
- **`.fieldNamed("typo")` is refused at construction instead of silently missing at runtime**
  (#95): the explicit-schema-name escape hatch appended the literal with no schema lookup at all,
  although the schema was in hand, so a typo — or the Scala field name passed where the schema name
  was meant — read `None` and wrote the payload back unchanged while reporting success. That is the
  same failure class the hatch exists to avoid, and it is what every "navigate by explicit schema
  name with `.fieldNamed`" error message points at. Map parents are carved out: `.fieldNamed` is
  also how a map KEY is addressed, and an absent key is data. Feature-detecting a RECORD field is
  still available via `codec.schema.getField(name)`.
- **A nested selector `.field(_.a.b)` is now a compile error** (#95): the selector parser shared by
  every cursor macro matched `Lambda(_, Select(_, name))` with ANY receiver, so `_.inner.y` parsed
  as the bare name `y` and the macro resolved it on the PARENT. Where the parent carries a field of
  that name — and a record holding a nested record often does — the result was a well-typed,
  perfectly lawful optic aimed at the wrong field: silent corruption on a 1:1, derived codec, with
  no schema divergence involved, and the macros' own "nested paths … chain them" abort unreachable
  for exactly the shape it was written for. A single-hop selector naming something that is not a
  case field of the parent (a no-arg `def`) is now a compile error too, instead of a literal field
  name that misses at runtime — that half is the new `MacroSelectors.requireCaseField`, which the
  resolvers need because the declaration index comes back `-1` both for "not a case field" and for
  the legitimate "this parent has no case fields at all", so the two can only be told apart at the
  selector. `AvroPrism` / `AvroTraversal`, `JsonPrism` / `JsonTraversal`
  (eo-circe) and `JsoniterPrism` / `JsoniterTraversal` (eo-jsoniter) all share the parser, so all
  six surfaces are covered.
- **Schema-field name resolution is linear in the record's field count, not quadratic** (#103): the
  nominal rung above called a per-case-field lookup that, on an exact-name MISS, linear-scanned
  every schema field with no early exit — it has to see a second match to call a name ambiguous. So
  a drilled hop cost `arity x fields.size x nameLength`, and it was charged to exactly the codecs
  the rung exists for, since a name transform (`withSnakeCaseFieldNames`, a custom
  `transformFieldNames`, a `vulcan.Codec` rename map) guarantees the miss; an identity-named codec
  answers from Avro's own name hash and never scanned. Measured on a 66-field snake_cased record:
  **486 us -> 8.8 us per resolution**, with the n -> 2n cost ratio falling from 3.97 (quadratic) to
  2.2 (linear). Resolution now builds one normalised-name index per record schema in a single pass
  and caches it under the schema's REFERENCE identity behind weak keys, so a k-hop chain into one
  record builds it once and a `def`-shaped optic — which re-resolves per operation — stops paying
  per operation. The doctrine is untouched: `NominalResolutionParitySpec` diffs every verdict of the
  new rung against the frozen pre-index implementation over 151,515 synthetic
  (schema x case-name-list) pairs plus every fixture behind the #95 suites, and the diff is empty.
  The exact-name fast path is unchanged and still allocation-free — an identity-named codec
  neither builds nor consults an index, and its per-resolution allocation is byte-identical before
  and after (24 / 64 / 152 / 280 B at n=2 / 12 / 33 / 66, all of it the `out` array). The index is
  resolved ONCE per resolution and threaded through the case fields, not re-fetched per field:
  per-field it would be a `ConcurrentHashMap` probe and a throwaway lookup key each, which is the
  same multiplier this issue removes, merely one level down. Two costs are traded for the time,
  both on the transformed path only and both measured with
  `com.sun.management.ThreadMXBean.getThreadAllocatedBytes`: the normalised key is a materialised
  `String`, so a resolution that misses the exact-name hash now allocates **~104 B per case field**
  where the pairwise cursor walk it replaces allocated nothing (24 -> 256 B at n=2, 280 ->
  7,168 B at n=66 per resolution); and building the index makes the FIRST resolution against a 1-2
  field record **slower** (n=1 ~+250 ns, n=2 ~+100 ns, once per schema), with the crossover at n=3
  and a 1.5x / 4.6x / 24x cold win at n=4 / 12 / 66.
- **The cached name index is handed out as an unmodifiable view** (#103): `private[avro]` is a
  Scala-only fence and the cached index is a process-wide singleton per schema, so returning the
  live `HashMap` meant one in-module `put` could silently corrupt field resolution for that schema
  for the life of the JVM. The wrapper, not the backing map, is what the cache stores, so the wrap
  costs one allocation per schema rather than one per lookup and repeated lookups still hand back
  the same instance.
- **`.fields(...)` compiles without a user-written `AvroCodec[NamedTuple[...]]` in scope** (#97):
  the macro spelled BOTH halves of the synthesised `NamedTuple` as a `*:` cons chain. That is
  `=:=` the `TupleN` spelling, so it type-checked — but hearth builds a sub-23 arity NamedTuple
  by calling the value tuple's primary constructor, and `*:` declares no value parameters, so
  any third-party derivation that had to BUILD the tuple (kindlings' decoder rule, reached
  through `AvroCodec.derived`) died at expansion with `wrong number of arguments at inlining`.
  It only ever worked because a hand-written given short-circuited the derivation.
  `LensMacro.namedTupleTypeOf` carried the identical pattern for the macro-lens focus and
  complement; both now route through the shared `MacroSelectors.tupleTypeOf`. The spellings are
  the same type, so existing user-written givens keep matching.

### Known limitations

Three codec shapes are still resolved to the wrong schema field, unchanged from 0.15.1 and pinned
as executable examples in `ResolutionResidualSpec`: a field list that both renames beyond
recognition and reorders; a schema column that bears a case field's name but holds a different
value (a derived public id, a stale legacy column); and two columns whose names normalise alike.
`.fieldNamed("schema_name")` reaches all of them.

## [0.15.1] - 2026-08-20

### Fixed

- **Bytes-face `.fields(...)` reads project the selected fields** — `AvroPrism.scan` handed the
  WHOLE parent datum to the NamedTuple codec for a Fields focus, so any divergence between the NT
  codec's schema and the parent layout (a codec name transform per issue #35, a schema-only extra
  field, a reordered selection) made the grouped byte-face read decode garbage or Miss outright.
  Fields focuses now route through `AvroBinaryCursor.decodeFieldsProjection`: decode the parent
  slice, project the selected fields by RESOLVED schema name via the record face's atomic
  `readFields`, then decode the projection — the read mirror of `encodeFieldsOverlay`. The record
  face and the bytes-face write were already correct; regression-pinned in `AvroFieldNamingSpec`.

## [0.15.0] - 2026-08-07

### Added

- **`Record.iso[T]` + `Record.lens[F]("name")` — kyo Records as optics** (#89): a quoted-macro
  `BijectionIso` between a kyo `Record` and a Scala 3 NamedTuple OR case class, with no arity
  ceiling (TupleXXL-backed above 22 fields), and per-field `GetReplaceLens`es via kyo's
  `Fields.Have` evidence. Requires kyo `1.0.0-RC6` (string-keyed Records).
- **`eo.kyo.schema` — optional kyo-schema bridge** (#89): kyo-schema is an `Optional`
  dependency (avro/circe pattern — add it yourself). Three seams: the **Focus bridge** maps
  kyo-schema's mode lattice onto eo carriers (`Focus.Id` → Lens, `Maybe` → Optional, `Chunk` →
  Traversal); the **codec byte faces** `Schema[A].prism[C]` / `stringPrism[C]` are Prisms
  between encoded payloads and `A` under any kyo codec (json, msgpack, protobuf, …); and
  **`StructureValues`** ports the circe module's playbook to the untyped `Structure.Value`
  tree — one constructor prism per case, sibling-preserving `field` / `at` / `key` navigation,
  an `each` traversal, `Plated[Value]` whole-document rewrites, `variant(name)` sum navigation
  (matches both the declared `VariantCase` spelling and the wrapper-record encoding RC6
  actually emits — getkyo/kyo#1860), and `Schema[A].valuePrism`, the typed ↔ untyped face.

### Changed

- **`cats-eo-kyo` now requires JDK 25** (#89): kyo `1.0.0-RC5+` ships Java-25-only bytecode.
  The module drops out of the root aggregate on older JVMs; the rest of eo keeps its JDK 17
  floor. CI tests on 17, 21, and 25 (25 is the primary lane).
- **`resultOptic` rebuilt on `Result.foldError`** (#89): the success prism for `Result[E, A]`
  folds failures AND panics into the miss arm directly — no `Maybe` → `Option` hop.

### Fixed

- **Docs-site kindlings derivation timeouts** (#90): mdoc's fence compiler never receives
  `-Xmacro-settings` from sbt properties; the budget now routes through mdoc's own
  `--scalac-options` channel, and heavy declared derivations live in compiled sample sources.
  The docs build drops from minutes (flaky) to ~30s (reliable).

## [0.14.0] - 2026-07-29

### Added

- **`AvroJsoniter` — structural Avro ↔ JSON-bytes bridge, no circe** (#85): the AST-free
  sibling of `AvroJson` in `eo.avro.jsoniter`. Same structural walk and rendering conventions,
  rendered straight to UTF-8 JSON bytes through jsoniter-scala's `JsonWriter` — no JSON AST or
  typed value on the render path. Mirrors the full prism family (`valuePrism` / `bytesPrism` /
  `recordPrism` / `record`) plus a strict schema-directed streaming parse for the reverse
  direction.
- **`.json` / `.avro` cursor faces on both bridges** (#85, #86): drill with the ordinary
  `AvroPrism` cursor sugar and flip the document into JSON at the end (`.json` — whole-doc
  reads/writes land in `JsoniterBytes` or `io.circe.Json`), or drill a JSON document with the
  `JsoniterPrism` / `JsonPrism` sugar and convert the **focus** to Avro binary (`.avro`).
  `AvroJsoniter.render[A]` / `AvroJson.render[A]` are the focus-as-standalone-JSON read
  terminals. New dependency-free seams underneath: `JsoniterPrism.raw` and `JsonPrism.raw`
  re-focus any drilled prism on its raw encoded slice.
- **`cats-eo-zio` + `cats-eo-kyo` DI integration modules** (#84): environment slot lenses
  (`ZEnvironment` / `TypeMap`), layer projection through `CanGet` (`focusLayer` /
  `Layer.focus`), `Ref` / `Var` focus ops as one atomic `CanModify` pass, and opt-in capability
  givens so `Exit` / `Maybe` / `Result` subjects satisfy generic capability demands. The ZIO
  slot lens measures cheaper than hand-written access on every gc-profiled row.

### Changed

- `cats-eo-avro` now takes `cats-eo-circe` and `cats-eo-jsoniter` as `Optional` dependencies
  (the faces name `JsonPrism` / `JsoniterPrism`); `cats-eo-jsoniter`'s avro back-reference is
  gone in every scope. `AvroBytes` is hoisted to the `eo.avro` package object and used across
  the bridges.

## [0.13.0] - 2026-07-23

### Added

- **Fused `Traversal` composition**: same-carrier `Traversal.andThen(Traversal)` and the
  `Traversal.andThen(Lens)` overloads (plus their Lens-family duals) now return the concrete
  `Traversal` class via the named `ComposedTraversal` / `TraverseTraversal` classes, so a
  composed chain keeps the fused inline `modify` / `replace` members and per-site monomorphic
  `to` / `from` dispatch.
- **Streaming leaf folds**: `Traversal.foldMap` is an overridable member the leaf constructors
  (`pEach`, `selfChildren`) override with folds that never build the focus vector; member twins
  `headOption` / `length` / `exists` ride the same path. Composed folds deliberately keep the
  materialize-then-fold walk — streamed variants measured worse in every regime that matters.
- **`PSVec.from` universal constructors**: one `from` for `IterableOnce` (PSVec identity
  pass-through, `ArraySeq.ofRef` zero-copy alias, structural `List` fast path), an `Array`
  overload (one `Array.copyAs` defensive copy), and a `Foldable` overload; replaces
  `fromIterable`. `PSVec` now extends `IterableOnce` (`iterator` / `knownSize`).

### Changed

- **BREAKING vs 0.12.x**: `Affine` is covariant in its focus and `Affine.Miss` drops its phantom
  `B` type parameter entirely (`Miss[A] <: Affine[A, Nothing]`) — retyping a miss across a focus
  change is now a compiler-verified upcast and the `widenB` cast helper is deleted.
  `PSVec.fromIterable` is removed (use `from`), `toAnyRefArray` is renamed `toAnyArray`, and the
  internal erased storage currency is `Array[Any]` end-to-end.

### Fixed

- **POMs always carry `<scm>`** (#72): `scmInfo` is pinned explicitly instead of derived from
  the CI environment, so every published artifact keeps the section Scaladex needs regardless
  of where it is built.

### Performance

- **circe path writes allocate less than 0.12** (`-prof gc`, size=8): fused walk + rebuild in
  `JsonWalk` deletes the parents vector entirely (`eoStreet` 2 968 → 2 720 B/op,
  `eoNames` 11 216 → 10 944), on top of removing its casts and the walk/rebuild correlation
  invariant.
- **`mfAssocPSVec.composeTo` now inlines**: the 432-byte monolith sat past C2's 325-byte
  hot-inline ceiling on every release since the kernel landed; split into a 123-byte dispatcher
  plus per-branch bodies, all under the ceiling.
- **Small-container traversal modify**: `TraversalBench.eoModify` size=8 688 → 608 B/op via the
  `PSVec.from` List fast path; jsoniter depth-3 reads 152 → 128 B/op via the single-outer
  zero-copy composition branch.

## [0.12.0] - 2026-07-21

### Fixed

- **avro union resolution for enum / fixed / bytes leaves**: `AvroWalk`'s union-branch
  resolution only knew records and unwrapped primitives, so a `union` alternative holding a
  `GenericEnumSymbol`, `GenericFixed`, or generic-decoded `bytes` (`ByteBuffer`) ALWAYS failed
  with `UnionResolutionFailed` even when the branch matched. Named leaves now resolve by their
  schema's full name; `ByteBuffer` resolves the `"bytes"` branch.
- **`AvroFailure.BadEnumSymbol` is now reachable**: union-branch resolution validates the
  runtime enum symbol against the schema's declared set (`EnumSymbol` doesn't validate at
  construction), refusing corrupt hand-built payloads with the symbol + declared set instead of
  passing the leaf downstream. Previously the case was never constructed anywhere.

### Changed

- **BREAKING (binary) vs 0.11.x**: core `Traversal` constructors and
  `Optional.readOnly` / `selectReadOnly` return types narrowed from `Optic[...]` to the concrete
  `Traversal` class / `PickFold` — the same narrowing move 0.11 made for the jsoniter cursors.
  Source-breaking: the `type AffineFold` alias is removed.
- **`Traversal.two` / `three` / `four` are macro-generated `inline def`s**: one
  `TraversalArityMacro` generator replaces the three hand-written tabulations. Expansion happens
  per call site (monomorphic `to`/`from`, literal selector/reverse lambdas beta-reduced in);
  the methods can no longer be eta-expanded.
- `AvroCodec.decodeResolvedRecord` / `decodeResolvedValue` gained a
  `threadLocalStorage: Boolean = true` parameter (binary-breaking descriptor change);
  `ConfluentWire`'s private line-for-line copies of the same decode are gone.

### Removed

Repo-wide over-engineering audit — dead or speculative surface with zero call sites, all cuts
grep-verified and the one perf-relevant cut B/op-verified:

- core: `Affine.apply` (legacy Either constructor), the `.affine` extension, and
  `Affine.ofLeft` / `ofRight` — construct `Affine.Miss` / `Affine.Hit` directly.
- circe: the `JsonFieldsPrism` / `JsonFieldsTraversal` compatibility aliases (they were
  `JsonPrism` / `JsonTraversal` since the JsonFocus unification).
- circe + avro: the dead `OnMissingField.Lenient` walk policy (no main-code caller); walks are
  strict, full stop.
- avro: unreachable `AvroFocus.navigateRaw` / `decodeFrom` seam.
- generics: delegate-only macro entry hops (`LensMacro.deriveMulti`, `PrismMacro.derive`,
  `PlateMacro.derive`) — the package-object inline defs splice the `*Impl` methods directly.
- jsoniter: `fromSteps` factories privatized (no external callers).
- schemes: the dedicated `unfoldFold` engine trio — `hylo` routes through the one `Coalg`
  engine; measured B/op-identical on `SchemesBench.eoHylo` (the per-node tuple + closure is
  escape-analysis-elided).

### Added

- **Cookbook — "Re-use an optic across representations"**: `Optic.outerProfunctor` /
  `innerProfunctor` documented as the re-aiming seam (UTC ↔ wall-clock foci via inner `dimap`,
  domain record ↔ wire tuple via outer `dimap`), with the fused-overload erasure caveat.
- **`generics.MacroSelectors.fieldsSelectorNT` / `caseFieldType`**: the shared validation +
  SELECTOR-order NamedTuple synthesis backbone behind the avro / circe `.fields` macros and all
  three modules' Dynamic sugar (~130 duplicated lines collapsed into the module each already
  depended on).
- **`sbt unusedCode` wired into quality.yml**: xuwei-k's `WarnUnusedCode` scan (unused PUBLIC
  classes/objects/methods) now runs as a non-gating report on every release tag.

### Infrastructure

- MiMa setup and the per-minor breaking-change history moved into `mima.sbt`.
- The generated per-push `clean.yml` artifact-deletion job is disabled
  (`githubWorkflowIncludeClean := false`) — report workflows set explicit `retention-days`.
- Three dead test files deleted; QA report generator de-duplicated (identity module map,
  hand-rolled tally → `collections.Counter`).

## [0.11.0] - 2026-07-20

### Added

- **jsoniter typed cursors** (#79, #80):
  - **`JsoniterPrism[A]`** — root `Prism[Array[Byte], A]`: whole-document decode via the codec,
    `reverseGet` via `writeToArray`. String paths move to `JsoniterPrism.fromPath[A]("$...")`.
  - **Compile-time field drilling**: `.field(_.x)` / `.at(i)` / `.each` and Dynamic sugar
    (`JsoniterPrism[Person].address.street`), checked against the case-class schema; drilled
    cursors are identical to their JSONPath twins. jsoniter now depends on `cats-eo-generics`.
  - **Capability seam**: a `JsoniterPrism` given serves derived `CanGetOption` / `CanModify`
    evidence for `T = Array[Byte]`.
  - **Docs**: optics-as-evidence migration recipe (JsonCodecMaker model → byte holder +
    leaf-codec prisms), capability-consumption examples, layer-on-codec vs replace-the-model
    guidance.

### Changed

- **BREAKING (binary) vs 0.10.x**: `JsoniterPrism` / `JsoniterTraversal` factory return types
  narrowed from `Optic[...]` to the new concrete classes (hence 0.11).

## [0.10.0] - 2026-07-16

### Removed

- **Public-surface pruning in `cats-eo-avro` — duplicate ways to do the same thing** (0.10, with
  the binary-compat bump already paid by this release):
  - `ConfluentWire.resolvingRecord` — unused; for a known writer schema, compose `strip` +
    `AvroCodec.decodeResolvedRecord` (its per-message twin `recordReader` remains).
  - `AvroCodec#decodeUnsafe` — unused throw-on-failure convenience; use
    `decodeEither(x).fold(throw _, identity)`.
  - The `AvroFieldsPrism` / `AvroFieldsTraversal` compatibility type aliases — they were
    `= AvroPrism[A]` / `= AvroTraversal[A]`; use the real names.

### Changed

- **Decode-path allocation reuse in `AvroCodec` (perf, decode output unchanged).** The binary
  decode helpers now reuse a per-thread `GenericDatumReader` per distinct `(writer, reader)`
  schema pair and a per-thread `BinaryDecoder` (decoding straight from the byte array), instead of
  allocating both per call — the top allocators on per-record consume paths like
  `ConfluentWire.recordReader`. Records are still fresh datums (no `Utf8`/bytes aliasing).
  `AvroCodec`'s decode helpers keep their existing signatures; the opt-out is set at optic
  construction — `threadLocalStorage = false`, a new defaulted parameter on the `ConfluentWire`
  decode constructors (`resolving`, `resolvingBytes`, `reader`, `recordReader`), captured as a
  field of the built optic / reader.
- **Single binary read/write path in `cats-eo-avro`.** Every binary Avro decode now funnels
  through one internal engine on the byte cursor — `AvroBinaryCursor`'s typed `DatumReaders[D]`
  caches (`records` / `leaves`, so no unchecked datum narrowing anywhere) — and every binary
  encode through its `writeDatum`: the `AvroCodec` root-payload helpers, the prism/traversal slice
  decodes, and `AvroJson`'s circe-bridge parses — which previously each carried their own
  reader/decoder incantation with ad-hoc reuse policies. The slice and circe paths inherit the
  per-thread reader/decoder reuse, and `AvroJson` no longer holds a single `GenericDatumReader` in
  a closure shared across every thread using the optic. `AvroCodec` carries no reuse machinery or
  internal seams — its helpers are plain failure-wrapped adapters over the cursor.

## [0.9.0] - 2026-07-14

### Added

- **Produce-side gated / resolving graft — `ConfluentWire.graftGated` / `graftResolving`** (#75,
  #76). The write twins of the consume-side gate/translate surface: splice a stored,
  Confluent-framed fragment into an outgoing encode at a prism focus, with the frame strip +
  registry resolve + parsing-canonical-form fingerprint gate + union-branch-index synthesis +
  per-writer-id cache all contained inside the combinator. The caller composes the prism from
  plain Scala types (`codecPrism[Conversion].field(_.clickInfo).union[ClickInfo]`) and supplies the
  registry hook — no frame math or `AvroFailure` juggling leaks into caller code. `graftGated`
  refuses drift with distinct causes to meter (`SchemaMismatch` permanent-structural vs
  `SchemaResolutionFailed` transient); `graftResolving` absorbs compatible drift by resolve-decoding
  writer → focus schema and re-encoding, so a compatible-but-not-identical fragment grafts as
  correct bytes rather than the silent garbage a raw byte splice would produce. Lookup failures are
  never cached.

### Changed

- **BREAKING: `AvroPrism.graftBytes` and `AvroPrism.sliceBytes` now return `Either[AvroFailure, …]`**
  instead of `Ior[Chain[AvroFailure], …]`. Both locate a single span and yield exactly one failure
  — never a `Both`, and the `Chain` was always length 1 — so the `Ior` / `Chain` wrapper was
  ceremony. Call sites that pattern-matched `Ior.Right` / `Ior.Left` move to `Right` / `Left`. The
  `IndexedRecord`-carried `.record` face keeps its genuinely-accumulating `Ior` diagnostic surface
  unchanged.

## [0.8.2] - 2026-07-13

### Added

- **`eo.avro.vulcan.AvroVulcan` — bridge a `vulcan.Codec[A]` into an `AvroCodec[A]`** (#73). A new
  sub-package of `cats-eo-avro` on the `AvroJson`/circe pattern: vulcan is an `Optional`
  dependency (the API surface *names* `vulcan.Codec`, so callers already depend on vulcan
  directly), and `import dev.constructive.eo.avro.vulcan.given` makes every in-scope vulcan codec
  usable wherever eo demands `AvroCodec[A]` evidence — `codecPrism`, `AvroPrism.field` /
  `widenPath*`, `AvroTraversal`, the `AvroJson` diagonals. Error mapping: the schema resolves once
  at construction and fails fast if invalid; encode errors throw (eo's `encode` is total — an
  encode failure under a matching schema is a codec-definition bug); decode errors surface as
  `Left` via vulcan's own `AvroError.throwable`. Kills both downstream patterns from #73: the
  per-bench hand-rolled adapter and the throw-stub codecs fabricated for navigation-only prisms.

## [0.8.1] - 2026-07-11

### Added

- **`MendTearPrism.tearFrom` / `mendFrom` (core).** The input-side mapping pair on the concrete
  prism: `tearFrom(f: S1 => S)` pre-composes the tear's source, `mendFrom(g: B1 => B)` pre-composes
  the mend's focus; `T` and `A` stay fixed. Unlike `Optic.outerProfunctor` / `innerProfunctor`,
  whose `dimap` erases to an anonymous `Optic`, both return the concrete `MendTearPrism` and so
  keep the fused-compose overloads and capability surface.
- **`AvroJson.valuePrism[A]` — the fundamental codec diagonal.** A
  `MendTearPrism[Any, Json, A, Any]`: the tear runs the codec's decode on an Avro generic runtime
  value, and a decode miss surrenders the *structural Json view* of the value (never the raw
  input), so a payload that is valid Avro but not a valid `A` still lands somewhere inspectable;
  the mend renders any generic value back as `Json`. Every other diagonal is this prism with its
  inputs pre-composed via `tearFrom` / `mendFrom`:
  - `pPrism[A]: MendTearPrism[Array[Byte], Json, A, IndexedRecord]` — tear payload bytes into a
    typed `A`, mend a generic record out as `Json`;
  - `bytesPrism[A]: MendTearPrism[Array[Byte], Json, A, A]` — typed both ways, so
    `modify(f: A => A): Array[Byte] => Json` works in one hop; plus a `bytesPrism[A](writer)`
    overload that Avro-resolves a drifted (compatible) writer schema before the decode;
  - `recordPrism[A]: MendTearPrism[IndexedRecord, Json, A, A]` — for streams already resolved to
    generic records (e.g. `ConfluentWire.recordReader` output);
  - `pRecord(schema): MendTearPrism[Array[Byte], Json, IndexedRecord, IndexedRecord]` — untyped
    and codec-free (a trivial per-schema `AvroCodec[IndexedRecord]` reuses the typed family);
    effectively `bytesToJson` upgraded to a writable prism.

### Removed

- **`AvroJson.codecPrism[A]: Prism[Json, A]`** — superseded by the `valuePrism` family the same
  day it shipped. The `Json`-sourced typed parse it provided is the one diagonal the family cannot
  express lawfully (a poly miss needs a *total* `S => T`, and `Json` renders into nothing totally),
  and its two halves survive: the strict schema-guided parse via `AvroJson.record(schema)` +
  `AvroCodec`, the render via `valuePrism`'s mend. Binary-breaking, shipped in a patch release
  deliberately: 0.8.0 was cut earlier the same day and MiMa remains disabled build-wide.

## [0.8.0] - 2026-07-11

### Added

- **`ConfluentWire.resolvingBytes` — the framed → framed drift-translating diagonal.**
  The Confluent surface had every corner except one: a `Array[Byte] => Either[AvroFailure,
  Array[Byte]]` that does per-message writer-schema resolution (like `recordReader` / `confluent`)
  AND *translates* writer→reader drift (like `resolvingRecord` — Avro's `ResolvingDecoder`, never
  the `resolve` fingerprint gate), handing back reader-layout framed bytes rather than a typed `A`
  or an `F[A]`. Per payload: strip the header, look the writer schema up by id, resolve-decode the
  body writer→reader, re-encode under `readerSchema` and re-frame under `frameId`. Because the
  output is reader-layout, it is stable across writer-schema evolution within a reader generation —
  the property the gating `resolve` (which refuses drift with `SchemaMismatch`) cannot give. A
  factory in the `confluent` mould (compute once at construction, cheap per call): the returned
  function closes over a `ConcurrentHashMap` keyed by writer id, so `schemaById` is consulted once
  per distinct writer id and every later payload under a seen id reuses the cached bridge. Failures
  are `Left` per the existing taxonomy — `NotConfluentFramed` (bad/`null` frame),
  `SchemaResolutionFailed` (the hook threw), `ResolveFailed` / `EncodeFailed`.
- **`eo.avro.circe.AvroJson` — structural Avro ↔ circe bridge, a lawful `Prism`.** A new
  sub-package of `cats-eo-avro` bridging Avro's generic value model and `io.circe.Json` with no
  typed case class in the middle. `AvroJson.record(schema): Prism[Json, IndexedRecord]` is the
  bidirectional entry point — and `AvroJson.codecPrism[A]: Prism[Json, A]` its typed counterpart
  (schema off the `AvroCodec`, no `IndexedRecord` at the call site; decode failure is the same
  prism miss): `reverseGet` is the total structural walk (record → object in
  schema-declaration order, map → object, list → array, `Utf8`/`CharSequence` → string, int/long →
  `fromLong`, double/float → `fromDoubleOrNull`/`fromFloatOrNull`, enum → string,
  `ByteBuffer`/`GenericFixed` → array of signed byte ints, resolved `null` branch → `Json.Null`);
  `getOption` is the strict schema-guided inverse (exact field cover — no extras, no defaults;
  `toInt`/`toLong` integrality and range; enum symbols and fixed lengths checked; unions
  first-branch-that-parses, `Json.Null` only ever matching a `null` branch), so the two prism
  round-trip laws hold (property-pinned). Also `avroToJson(record): Json` directly and the
  `bytesToJson(schema): Getter[Array[Byte], Json]` read optic (parse-to-record fused with the walk
  via `Getter.andThen`). Logical types and encoder-specific string-transforms are explicit
  non-goals (the bridge sees only the runtime value). circe rides on `cats-eo-avro` as an
  `Optional` dependency: it never reaches downstream classpaths transitively, and any caller of
  this sub-package already depends on circe directly — its API surface *names* `io.circe.Json`.

### Changed

- **`ConfluentWire.reader` / `recordReader` are strict on the frame.** A payload that does not
  parse as a Confluent frame now raises `AvroFailureException(NotConfluentFramed)` in `F` instead
  of silently falling back to a direct decode under the codec's schema — the fallback could
  accidentally succeed on corrupt bytes and yield garbage. Consumers of topics with mixed framed /
  unframed producers opt into their own fallback by catching that failure and decoding directly
  (`AvroCodec.decodeValue[A]` / `AvroCodec.decodeRecord`). Behavioural break for anyone relying on
  the auto-detect; correctness over convenience.

### Fixed

- **`ConfluentWire.strip(null)` is a defined failure, not an NPE.** A `null` payload (a Kafka
  tombstone or a mis-produced record) previously dereferenced `bytes.length` and threw
  `NullPointerException` out of the header strip; it now returns
  `Left(AvroFailure.NotConfluentFramed(...))` like any other malformed frame, so a downstream
  consumer rejects it diagnosably. `strip`'s parameter widens to `Array[Byte] | Null` (source- and
  binary-compatible under the module's `-Yexplicit-nulls`).

## [0.7.0] - 2026-07-09

### Added

- **Capability traits at `dev.constructive.eo`** — `CanGet`, `CanGetOption`,
  `CanReverseGet`, `CanModifyP`/`CanModify`, `CanFold`, `CanPutP`/`CanPut`,
  `CanModifyFP`/`CanModifyF`, `CanModifyAP`/`CanModifyA`, `CanPlace`,
  `CanTransform`: the carrier-free consuming surface of an optic, usable as
  `using` evidence (`def adjustTimes[T](using cm: CanModify[T, Instant])`).
  Concrete optic classes implement their capabilities directly (hot path);
  a derived given per companion serves optics bound at the generic
  `Optic[…, F]` type. `CanFold.foci` is the carrier-free counterpart of the
  `all` extension. See the new [Capabilities](https://eo.constructive.dev/capabilities.html)
  page and `CapsBench` (capability call ≈ direct call on CI).
- **Fused `SplitCombineLens.andThen(SplitCombineLens)`** — macro-derived lens
  chains (`lens[Person](_.address).andThen(lens[Address](_.street))`) now
  compose into a concrete class, keeping the fused read/write paths and the
  capability mixins instead of falling back to an anonymous `Optic`.

### Changed

- **Binary-breaking (recompile required):** stored-function `val`s whose names
  collide with capability kernels became private constructor params + methods:
  `Getter.get`, `GetReplaceLens.get`, `SplitCombineLens.get`,
  `BijectionIso.get`/`reverseGet`, `Review.reverseGet`;
  `GetReplaceLens.replace` went niladic → unary. Source-compatible via
  auto-eta (`xs.map(g.get)` still compiles), but jars compiled against
  ≤ 0.6.x fail at runtime against 0.7.0 core — the `inline` fused `andThen`s
  spliced the old accessors into caller bytecode. Recompile downstream.

### Added

- **`Getter`s now compose with `Getter`s via `andThen`.** `g1.andThen(g2)` reads
  `s => g2.get(g1.get(s))` and yields a `Getter`, matching how `Iso` / `Lens`
  compose through their fused subclasses. `Getter.apply` now returns a concrete
  `DirectGetter` carrying the fused `andThen` (previously a bare anonymous `Optic`
  with no `andThen`, forcing callers to hand-nest `get` calls). As part of this,
  a `Getter`'s back-focus slot is now `Unit` (`Optic[S, Unit, A, Unit, Direct]`,
  was `Optic[S, Unit, A, A, Direct]`) — making the read-only-ness explicit in the
  type and lining the inner `T` up with the outer `B` for composition. Pre-1.0,
  no published baseline, so no MiMa break; external code that ascribed the full
  `Optic[S, Unit, A, A, Direct]` type updates the final type argument to `Unit`.

- **`Plated` — recursive self-traversal + recursion combinators.** A new
  `optics.Plated[S]` (the cats-eo analogue of Haskell `lens`'s `Plated`) whose
  `plate` is a `Traversal[S, S]` over the immediate same-typed children of a
  recursive ADT, plus stack-safe combinators `transform` (bottom-up rewrite),
  `rewrite` (Option-rule fixpoint), `children`, and `universe`. The headline is
  `Plated.everywhere` — `transform` in *composable optic* form: a `Setter` whose
  `modify` is the recursive `transform`, so `everywhere.andThen(prism).andThen(lens).modify(f)`
  applies `f` at the focus *at every depth* (e.g. uppercase every variable in an
  expression tree). It reuses the existing `SetterF` carrier and `Morph`/`Composer`
  bridges — no new carrier — so it composes with any inner optic (Lens / Prism /
  Optional / …) as the outer of `.andThen`. All are stack-safe on deep trees —
  `transform` recurses on the call stack while shallow and falls back to a
  heap-stack machine past a depth bound, `universe` / `children` use a worklist,
  `rewrite` a `cats.Eval` trampoline.
  Build one with `Plated.fromChildren`, derive it with `generics.plate[S]`
  (focuses every exact-`S`-typed field across all cases; enums, sealed
  hierarchies, recursive case classes), or call the combinators directly on any
  self-traversal optic via the `.transformAll` / `.universeOf` extensions. New
  `Traversal.selfChildren` constructor builds the underlying `MultiFocus[PSVec]`
  self-traversal from an explicit children view. The carrier is PSVec-native end
  to end — `fromChildrenVec` / `generics.plate[S]` speak the `MultiFocus[PSVec]`
  focus vector directly, with a `List`-shaped `fromChildren` kept as a
  hand-writing convenience — so neither path pays a `List ↔ PSVec` round-trip.
  `universe` reads children straight off the carrier (no `List` per node) and
  `transform` is hybrid — a direct call-stack recursion while shallow, a
  heap-stack machine past a depth bound (via `childrenVec` / `rebuild`, no
  `to`/`from` tuple per node; leaves applied in place) instead of an `Eval`
  trampoline; both stay stack-safe (deep + 100k trees) while running
  within ~2–3× of a hand-written recursive visitor and ahead of Monocle's
  `Plated` (which is not stack-safe). See the
  [benchmarks](https://github.com/Constructive-Programming/eo/blob/main/site/docs/benchmarks.md).
- **Universal `Plated` instances for the JSON and Avro carriers.**
  `dev.constructive.eo.circe.platedJson` makes `io.circe.Json` a recursive
  self-traversal (children = an array's elements / an object's field values), so
  `transform` / `rewrite` / `universe` walk a whole document — redact every
  field at any depth, rewrite every string, rename keys throughout — no decode.
  `dev.constructive.eo.avro.platedAvro` is the Avro mirror over `IndexedRecord`
  (children = directly-record-valued fields; records nested inside array / map /
  union fields are leftover skeleton in this version).

### Changed

- **Optic composition is `inline` at the fused composers — deep chains no longer
  hit the JIT's recursive-inline cap.** The same-carrier fused `andThen` on
  `DirectGetter` / `GetReplaceLens` / `BijectionIso` / `SetterOptic` /
  `MendTearPrism` / `PickMendPrism` / `Optional` is now `inline`. A plain `def`
  compiled the composer's `s => inner.get(get(s))` lambda once, so a depth-N
  runtime chain reused that single bytecode and HotSpot's C2 treated the cascade
  as recursion — capping inlining at `MaxRecursiveInlineLevel` and leaving the deep
  tail as virtual `Function1.apply`. `inline` splices a *distinct* lambda per
  compose site (distinct synthetic methods per level), so the chain inlines fully
  with no JVM flag — the effect Monocle gets from a fresh anonymous class per
  compose. CI (`-f 3 -wi 3 -i 5`) at depth 6: Getter 27.2→11.8 ns, Setter 44→26 ns,
  Optional `modify` 139→94 ns — EO now **1.3–2.3× faster** than the equivalent
  Monocle composition (was 1.3–2.8× slower). Terminal mixed-carrier overloads
  (`Lens→Optional` etc.) fire once per chain and stay plain `def`. Behaviour
  identical; pre-1.0, no MiMa baseline.

- **circe JSON array-write traversal now matches hand-written AST surgery.**
  `JsonWalk.walkPath` / `rebuildPath` (the per-element backbone of `JsonTraversal`
  writes) converted the immutable `path: Array[PathStep]` to a `Vector` /
  `IndexedSeq` and ran an `Either`-monad `foldLeftM` / `zip` + `foldRight` on every
  call — i.e. once per array element. Rewritten as manual index loops
  (behaviour-identical). `OrderCirceBench` `lines[*].name` write (CI, `-f 3 -wi 3
  -i 5`) at 512 elements: 868→655 KB/op allocation (−25%, now 1.07× the hand-rolled
  `direct` `JsonObject` surgery, was 1.42×) and 277→249 µs (now on par with the
  `direct` / `hcursor` hand forms and faster than decode→Monocle→encode). Helps
  every JSON optic op, not just traversals.

- **`Setter` composition is now allocation-free per hop (fused `andThen`).**
  `Setter.apply` returns a concrete `SetterOptic` that stores its writer directly
  and overloads `andThen(SetterOptic)` to compose writers outright
  (`s1.andThen(s2).modify(f) == s1.modify(s2.modify(f))`), building the chain once
  at compose time — the same fused-subclass shape as `Lens` / `Iso` / `Getter`.
  Previously two `Setter`s composed only through the generic
  `AssociativeFunctor[SetterF]`, which allocated a fresh `SetterF` per hop at
  *modify* time. `SetterBench` (CI, `-f 3 -wi 3 -i 5`) allocation goes from ~2.8×
  Monocle at depth to parity: depth-3 368→168 B/op, depth-6 800→288 B/op (==
  Monocle). The fused composer is also `inline` (see the composition entry above),
  which removes the dispatch overhead too — net, Setter composition is now *faster*
  than Monocle at depth (depth-6 26 vs 60 ns). Cross-carrier composition
  (`lens.andThen(setter)` via
  `Morph[Tuple2, SetterF]`) still routes through `assocSetterF`. Pre-1.0, no MiMa
  baseline; `Setter.apply`'s return type widened from `Optic[…, SetterF]` to the
  subtype `SetterOptic[…]` (source-compatible).

- **Carrier typeclass methods take named parameters (were point-free / curried).**
  `Accessor.get`, `ReverseAccessor.reverseGet`, `ForgetfulFold.foldMap`, and
  `ForgetfulTraverse.traverse` were declared with zero value parameters, returning
  a curried `Function1` / `PolyFunction` chain (e.g. `foldMap[X,A,M]: (A => M) =>
  F[X,A] => M`). They now take their arguments directly (`foldMap[X,A,M](f, fa)`),
  matching the already-flat `ForgetfulFunctor.map` and simplifying the dispatch
  the JIT sees. Behaviour-identical and allocation-neutral (the intermediate
  closures were already eliminated by `inline` + escape analysis); source-breaking
  only for code that *implements* a custom carrier or calls these SPI methods
  directly — update the overrides/calls to the named-parameter arity. Pre-1.0, no
  MiMa baseline.

- **`Direct` is now an `opaque type`, not a transparent alias.** `Direct[X, A]`
  was `type Direct[X, A] = A`, which the compiler dealiased to `A` during implicit
  search — so `object Direct`'s givens (`Accessor[Direct]`, `AssociativeFunctor
  [Direct, …]`, …) were *not* in the implicit scope of a bare `A`, and same-carrier
  composition (`iso.andThen(iso)` via the generic path, `getter.andThen(getter)`)
  couldn't summon them. `opaque type Direct[X, A] = A` makes `Direct` a distinct
  type whose companion *is* in scope, so those givens resolve with no import and
  the generic `andThen` works for `Direct`-carrier optics. It still erases to `A`
  (zero runtime cost); `Direct.apply` (wrap) / `.value` (unwrap) are
  `transparent inline` identities that compile away entirely, and exist only so
  construction sites outside `object Direct` satisfy the type.
  `Forget[F]` is decoupled from `Direct` (now `[X, A] =>> F[A]` directly, the same
  type it always was) so the Fold/Forget machinery is unaffected. No behaviour
  change; pre-1.0, no MiMa baseline.

- **Identity carrier renamed `Forgetful` → `Direct`.** The carrier behind `Iso`
  and `Getter` (`type Direct[X, A] = A`) now reads as what it does — direct
  function application, no leftover — rather than naming the category-theory
  mechanism. The `Forgetful*` typeclasses (`ForgetfulFunctor`, `ForgetfulFold`,
  `ForgetfulTraverse`, `ForgetfulApplicative`) keep their names, and the
  scaladoc still notes `Direct` *is* the forgetful functor. Mechanical rename
  for any external code: `Optic[…, Forgetful]` → `Optic[…, Direct]`,
  `import …data.Forgetful.given` → `…data.Direct.given`. Pre-1.0 / no published
  baseline (`tlMimaPreviousVersions` is empty), so no MiMa break.

### Added

- **`getOption` for `Either`-carrier optics.** Previously `getOption` was
  defined only for the `Affine` carrier (`Optional` / `AffineFold`). A `Prism`
  surfaced as the bare `Optic[S, T, A, B, Either]` — e.g. a derived
  `generics.prism`, whose static type is the base `Optic`, not the concrete
  `Prism` subclass — had no `getOption`. An `Either`-carrier overload now fills
  that gap (`@targetName`-disambiguated; the concrete `Prism`'s own member still
  wins at its static type), so a read on a derived prism reads through the same
  `getOption` call as everything else.

### Changed

- **Internal carrier unification — `AlgLens[F]` + `Kaleidoscope` collapse
  into `MultiFocus[F]`.** The two pre-0.1.0 carriers had identical
  `(X, F[A])` value shapes and only differed in their encoding (parameter
  vs path-dependent F; cats-`Functor` vs project-local `Reflector`).
  `MultiFocus[F][X, A] = (X, F[A])` is now the single home for
  classifier-shaped optics + aggregation universals. The
  `Reflector[F]` typeclass is deleted. The K3 `collectViaReflect` law is
  replaced by a carrier-wide `collectViaMap` (Functor-broadcast); the
  `Reflector[List]` cartesian-singleton story survives at the call site as
  the `MultiFocus[List].collectList` extension. Pre-0.1.0 — no published
  artifact to break, but the rename is mechanical for any external code
  on the spike branch (`AlgLens[F]` → `MultiFocus[F]`, `Kaleidoscope` →
  `MultiFocus[F]`, `kal.collect[F, B](agg)` → `mf.collectMap[B](agg)` or
  `mf.collectList(agg)`).

## [0.1.0] - 2026-04-25

### Added

- First public release.
- Full discipline-checked law coverage for every public optic (Iso,
  Lens, Prism, Optional, AffineFold, Getter, Setter, Fold, Traversal,
  Grate, AlgLens[F], Kaleidoscope) and every shipped carrier (Affine,
  SetterF, Vect / PSVec, PowerSeries, FixedTraversal[N], AlgLens[F],
  Grate, Kaleidoscope).
- mdoc-verified docs site with optic-family taxonomy, composition
  lattice, and Ior failure-flow Mermaid diagrams.
- Benchmarks vs. Monocle for Lens / Prism / Iso / Traversal / Fold /
  Getter / Optional / Setter; EO-only benches for PowerSeries / Grate /
  AlgLens / Kaleidoscope.
- circe integration — `JsonPrism` / `JsonTraversal` / `JsonFieldsPrism`
  / `JsonFieldsTraversal` with **observable-by-default** failure
  surface (`Ior` — see plan 005 in `docs/plans/`).
- Avro integration (`cats-eo-avro`) — `AvroPrism` / `AvroTraversal` /
  `AvroFieldsPrism` / `AvroFieldsTraversal` with the same Ior surface,
  plus `.union[Branch]` for schema unions (Option, sealed traits,
  Scala 3 enums) and a triple-input shape
  (`IndexedRecord | Array[Byte] | String`) covering parsed records,
  binary wire payloads, and Avro JSON wire format. Codec backend is
  [kindlings-avro-derivation](https://github.com/MateuszKubuszok/kindlings)
  (auto-derived `AvroEncoder` / `AvroDecoder` / `AvroSchemaFor`,
  combined into one project-internal `AvroCodec[A]`). Hot-path walks
  beat the kindlings-codec round-trip baseline by 3-7&times; on the
  shipped JMH benches.
- `lens` / `prism` Scala 3 macros via [Hearth](https://github.com/MateuszKubuszok/hearth):
  multi-field NamedTuple focus and full-cover Iso upgrade.
- Sonatype Central Portal publishing via `sbt-typelevel-ci-release`.

### Changed

- Pre-`1.0`, the public surface is refined as we learn from real
  users. Each non-bugfix release will list breaking changes here.
  `0.1.0` is the first publish, so there is no prior surface to break.

### Fixed

- N/A for the first release.

### Known issues

- circe walk-and-rebuild duplication (~1200 LoC) — flagged in
  [`docs/research/2026-04-23-code-quality-review.md`](docs/research/2026-04-23-code-quality-review.md)
  §F1 as a `0.1.1` refactor target. The user-facing surface should not
  change; the cleanup is internal.

### Composition coverage

The full composition matrix scoreboard is **96 N / 90 M / 39 U / 0 ?**
across 225 cells (196 Optic&times;Optic + 28 standalone borders + 1
JsonTraversal&times;Review corner). See:

- The "Composition limits" subsection in
  [`site/docs/optics.md`](site/docs/optics.md) — manual-idiom (M) and
  unsupported-by-design (U) pairs are documented inline next to each
  optic family.
- The resolution scoreboard in
  [`docs/research/2026-04-23-composition-gap-analysis.md`](docs/research/2026-04-23-composition-gap-analysis.md)
  &sect;7 (and the per-cell ledger in &sect;1.1 / &sect;3 / &sect;4).

[Unreleased]: https://github.com/Constructive-Programming/eo/compare/v0.17.0...HEAD
[0.17.0]: https://github.com/Constructive-Programming/eo/compare/v0.16.0...v0.17.0
[0.16.0]: https://github.com/Constructive-Programming/eo/compare/v0.15.1...v0.16.0
[0.1.0]: https://github.com/Constructive-Programming/eo/releases/tag/v0.1.0
