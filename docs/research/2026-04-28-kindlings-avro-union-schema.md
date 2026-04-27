# Kindlings-avro-derivation: union/sum schema layout

Date: 2026-04-28 — empirical investigation feeding Unit 8 of the eo-avro
plan (the `.union[Branch]` macro).

The probe was a one-shot specs2 file (`UnionSchemaProbeSpec`, deleted
after Unit 8 landed) that derived `AvroSchemaFor[X]` for several
union/sum shapes and dumped the resulting `org.apache.avro.Schema` to
stdout. All findings below are direct stdout captures from
`avroIntegration/Test/testOnly`.

## 1. Shape coverage matrix

| Scala shape                               | Derives? | Schema type | Branch names                                  |
|------------------------------------------ |----------|-------------|-----------------------------------------------|
| `Option[Long]`                            | Yes      | UNION       | `null`, `long`                                 |
| `Option[String]`                          | Yes      | UNION       | `null`, `string`                               |
| `Long \| String` (Scala 3 union)          | **NO**   | —           | macro abort: kindlings does not derive these   |
| `sealed trait Payment { Cash, Card }`     | Yes      | UNION       | `Cash`, `Card` (simple class name, no ns)      |
| `enum Status { Active, Disabled, Pending(reason: String) }` | Yes | UNION | `Active`, `Disabled`, `Pending` (records all)  |

Take-aways:

- Kindlings does NOT support Scala 3 untagged union types. The macro
  has to abort with a clear message at compile time when the parent
  focus is one of those.
- Sealed traits and Scala 3 `enum`s both produce UNION schemas where
  every alternative is a `RECORD` (parameterless enum cases become
  empty records — `name=Active` of type RECORD with zero fields).
- `Option[T]` produces a `["null", T]` union. The `null` alternative
  has `Schema.getName == "null"` and `getFullName == "null"`; the
  primitive alternative has `getName == getFullName == "long"` etc.
- For sealed-trait alternatives, the branch's `getName` and
  `getFullName` agree because the probe ADTs were declared at
  top-level of the test file (no Scala package). With a real
  user-defined ADT in a package, `getFullName` would be
  `pkg.subpkg.Cash` and `getName` would still be `Cash`. **The walker
  uses `getFullName`, so the macro must compute `getFullName` to
  match — or the walker has to be relaxed to match either name.**

## 2. Encoded runtime values

| Source value                  | Encoded runtime class & shape                                                         |
|------------------------------ |--------------------------------------------------------------------------------------|
| `Some(42L)` for `Option[Long]`| `java.lang.Long` (no wrapping; the value at the union slot IS the long box)           |
| `None` for `Option[Long]`     | `null`                                                                                |
| `ProbePayment.Cash(100L)`     | `org.apache.avro.generic.GenericData$Record` with `getSchema.getFullName == "Cash"`   |
| `ProbePayment.Card("4242")`   | `GenericData$Record` with `getSchema.getFullName == "Card"`                           |

This means the existing walker's `unionBranchName` helper already does
the right thing: for record values it returns `rec.getSchema.getFullName`
which equals the schema-declared branch identifier.

## 3. Key implications for `.union[Branch]`

1. **Branch identifier convention.** For both sealed-trait and enum
   alternatives, the branch identifier is the schema's
   `getFullName` of that alternative. Kindlings emits the simple
   class name when the user's ADT lives at top-level (no package).
   When the user's ADT is in a package, the simple name and full
   name diverge. The macro should compute the schema's `getFullName`
   from the `Branch` type's symbol — or, more robustly, summon
   `AvroCodec[Branch]` and read `.schema.getFullName` off it. The
   second is robust to whatever convention kindlings uses; the
   first re-derives the convention and risks divergence.

2. **`Option[Branch]` quirk.** The `null` alternative has no Scala
   counterpart that a user can name in `.union[Null]`. `.union[Long]`
   on an `Option[Long]` field works (branch name `"long"` matches
   the schema). The macro should compute the branch name from the
   summoned `AvroCodec[Branch].schema.getName` (lowercase `"long"`
   for primitives, full-name for records).

3. **Compile-time validation surface.** What we can verify at the
   macro layer:
   - **`Branch`'s `AvroCodec` is summonable.** Already done by the
     skeleton.
   - **Parent `A` is a sealed trait, enum, or `Option[_]`.** Doable
     via Hearth's `Enum.parse[A]` for the first two; `Option[_]`
     can be detected by checking `TypeRepr.of[A] <:< TypeRepr.of[Option[_]]`.
   - **`Branch` is a known alternative of `A`.** For sealed/enum,
     iterate `Enum.parse[A].cases` and verify `Branch <:< caseType`
     for some case. For `Option`, verify `Branch =:= elementType`.
   - **Scala 3 untagged union (`A | B`)** — abort with "kindlings does
     not support untagged union types; use a sealed trait or
     `Option[T]` instead".

4. **Runtime-side validation.** What only the walker can verify
   (because it needs the actual schema, not just a Scala type):
   - The branch identifier is one of the schema's declared
     alternatives. (Possible but redundant — if compile-time
     verification passes, this should always succeed.)
   - The runtime value's schema-derived branch name matches the
     requested branch (this is the existing walker check; it's
     where `UnionResolutionFailed` fires).

5. **The plan's "Int index" suggestion.** The plan mentioned that
   the macro might compute a compile-time integer index. Cost-benefit
   is poor: the schema isn't visible at macro time (the walker must
   know which schema to look up, and only `AvroCodec[A].schema`
   provides it — but the `Branch`'s position within that union
   schema's `getTypes` list isn't known at compile time without
   doing the schema lookup at runtime). String-based identifiers
   are simpler and the cost is one `.indexOf` on a small list per
   call.

## 4. Decisions for Unit 8

- Keep `widenPathUnion[B](branchName: String)` in [`AvroPrism`].
- Macro behavior:
  - For `parent: AvroPrism[A]`, summon `AvroCodec[Branch]`.
  - Compute branch name as `AvroCodec[Branch].schema.getFullName` at
    runtime (the macro emits `summon[AvroCodec[Branch]].schema.getFullName`).
    This dodges any compile-time vs kindlings convention drift.
  - Compile-time check: parent `A` must be one of {sealed trait, enum,
    `Option[_]`, Scala 3 union type}. Scala 3 untagged unions abort
    with a clear "not supported by kindlings" error. The other shapes
    abort if `Branch` isn't one of the known alternatives.
- Walker: keep the existing `unionBranchName` heuristic but also
  consult the parent's union schema (looked up via the chain of
  parent schemas threaded through the walker) when the `cur` value
  is a record — the heuristic is already correct for the kindlings
  encoding.
