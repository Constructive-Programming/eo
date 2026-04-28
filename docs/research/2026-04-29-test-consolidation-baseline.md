# Test consolidation baseline (2026-04-29)

Branch: `consolidate-tests-2026-04-29` (off `main` @ `a41e144`).

## Per-spec baseline counts (specs2 reported, NOT grep)

| Module | Spec | Examples |
|--------|------|---------:|
| core | FoldSpec | 1 |
| core | MultiFocusFunction1Spec | 6 |
| core | MultiFocusSpec | 5 |
| **core total** | | **12** |
| tests | FusedAndThenSpec | 4 |
| tests | InternalsCoverageSpec | 7 |
| tests | PowerSeriesSpec | 4 |
| tests | examples/CrudRoundtripSpec | 3 |
| tests | OpticsBehaviorSpec | 30 |
| tests | EoSpecificLawsSpec | 27 |
| tests | OpticsLawsSpec | 104 |
| tests | (OpticsSpecs aggregate filter — same count) | (counted once) |
| **tests total** | | **180** |
| circe | CrossCarrierCompositionSpec | 6 |
| circe | JsonFailureSpec | 5 |
| circe | NamedTupleCodecSpike | 4 |
| circe | StringInputSpec | 4 |
| circe | JsonFieldsTraversalSpec | 5 |
| circe | JsonTraversalSpec | 6 |
| circe | JsonPrismSpec | 15 |
| circe | FieldsMacroErrorSpec | 9 |
| circe | JsonFieldsPrismLawsSpec | 10 (5 forAll + 5 discipline) |
| **circe total** | | **64** |
| avro | StringInputSpec | 4 |
| avro | AvroFieldsTraversalSpec | 5 |
| avro | AvroTraversalSpec | 6 |
| avro | AvroPrismSpec | 7 |
| avro | CrossCarrierCompositionSpec | 5 |
| avro | AvroWalkSpec | 11 |
| avro | AvroUnionSpec | 11 |
| avro | AvroFieldsPrismSpec | 9 |
| avro | FieldsMacroErrorSpec | 8 |
| avro | AvroFailureSpec | 12 |
| avro | laws/AvroPrismLawsSpec | 16 (11 forAll + 5 discipline) |
| **avro total** | | **94** |
| generics | MacroErrorSpec | 8 |
| generics | GenericsSpec | 14 |
| **generics total** | | **22** |
| **GRAND TOTAL** | | **372** |

## Coverage baseline (sbt-scoverage, post `coverage; test; coverageReport; coverageAggregate`)

Per-module:

| Module | Statement % | Branch % |
|--------|-----------:|---------:|
| laws | 100.00 | 100.00 |
| core | 85.23 | 83.33 |
| generics | 82.96 | 81.97 |
| circe | 77.93 | 73.26 |
| avro | 67.43 | 59.83 |

**Aggregate (target/scala-3.8.3/scoverage-report/scoverage.xml):**
- **Statement: 80.20%**
- **Branch: 71.15%**

## Consolidation budget

Current: **372** examples. Target: **≤ 199**. Drop: **≥ 173 examples**.

## Hard constraint reminder

- Branch coverage must NOT drop below **71.15%** (rounding tolerance ±0.5%).
- Discipline `*Tests` law calls (`checkAll(...)`) preserved 1:1.
- Every consolidation gets a `// covers: …` reverse-index comment.
