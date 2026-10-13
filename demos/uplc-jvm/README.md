# UPLC → JVM bytecode, as one eo `Iso`

A compiler from Untyped Plutus Core (UPLC, the language Cardano smart contracts
run in) to JVM class files, built around a single cats-eo optic. Correctness is
established differentially: every program runs through the generated class AND
through the [Scalus](https://scalus.org) CEK machine, and the two must agree.

```scala
import dev.constructive.eo.demo.uplc.*

//            S             T             A             B
UplcJvm.iso: BijectionIso[Array[Byte], Array[Byte], TypedProgram, JvmProgram]

val classFile = UplcJvm.translate(flatScript)  // = UplcJvm.iso.modify(Lower.lower)
UplcJvm.iso.get(flatScript).body.show          // A, rendered as Scala-like code
UplcJvm.onChain.modify(Lower.lower)(cbor)      // cborScript andThen iso: on-chain CBOR in
Exec.validate(classFile, scriptContext)        // Right(traces) | Left(reason), CIP-117
```

| | type | what it is |
|---|---|---|
| `S` | `Array[Byte]` | a flat-encoded UPLC program |
| `A` | `TypedProgram` | the program read back as a typed, Scala-like expression tree |
| `B` | `JvmProgram` | JVM-shaped code: static methods, local slots, explicit boxing |
| `T` | `Array[Byte]` | one self-contained class file |

`get` decodes and types (S ⇒ A), `reverseGet` emits bytecode (B ⇒ T), and the
compiler is `modify` with the lowering `A ⇒ B`. The optic fixes both ends; the
function you run through it is replaceable — put an optimiser in front of
`Lower.lower` and nothing else changes. The iso is polymorphic (`S`/`T` and
`A`/`B` differ), so the round-trip laws of a monomorphic iso do not apply; the
law that matters is the commuting square the test-suite checks for every
program: *running `translate(s)` gives what the CEK machine gives for `s`*.

## Pipeline

```
flat bytes ──decode──▶ SourceProgram ──Typer──▶ A: Expr ──Lower──▶ B: Code ──Emit (ASM)──▶ class file
             (pre-order node ids,        (re-sugaring,       (closure conversion,
              shared with the runtime)    hybrid typing)       slots, boxing)
```

- **`SourceProgram`** numbers every UPLC node in pre-order. That numbering is
  the contract between compile time and run time: a closure carries only its
  node id; the class embeds the flat program and re-decodes it, so a result can
  be *discharged* back to a UPLC term exactly as the CEK machine does.
- **`Typer`** (S ⇒ A) recognises what UPLC compilers emit for constructs UPLC
  lacks, using rewrites that are exact under CEK evaluation order: `let`
  (immediately applied lambda chains), `if` (forced `ifThenElse` over delayed
  branches, and the other selector builtins), saturated builtin calls, direct
  calls of `let`-bound functions and of `let`-bound forced builtins, the
  `case (constr 0 args…) [f]` multi-argument call, `force (delay t)`.
- **Hybrid typing.** Integers, booleans, bytestrings and strings get unboxed
  representations (`BigInt`, `boolean`, `ByteString`, `String`) wherever the
  typer can prove them — constants, typed builtin results, `let`s of those,
  agreeing branches. Everything else — functions, constructors, `Data`, values
  that flow through lambda parameters — is a boxed Scalus `Constant` or a
  runtime closure. Boxing happens only where the two meet.
- **`Lower`** (A ⇒ B) converts closures (each captures exactly its free
  variables, in the order `SourceProgram.captures` fixes for both sides),
  allocates locals, inserts box/unbox, picks typed fast paths, and moves a `let`
  body into its own method once a method nears the JVM's 64 KB limit.
- **`Emit`** (B ⇒ T) writes one class: a static method per lambda chain /
  `delay`, a `lookupswitch` closure dispatcher, and the flat program as string
  constants.
- **Runtime** (`runtime/`): closures, partial builtins, constructors, `case`
  dispatch (including PV11 casing on builtin values), discharge. Builtins with a
  typed fast path call the same `scalus.uplc.builtin.Builtins` functions
  Scalus' own `CardanoBuiltins` wraps; the five selector builtins run natively;
  every other builtin is delegated to Scalus' implementation.

## Correctness

The semantics are pinned in `Semantics.scala`: Plutus V3 at protocol version 11
("van Rossem"), builtin semantics variant E — the configuration Scalus 1.3.0
runs the Plutus conformance suite under. The oracle and the generated code's
runtime both read it from there.

`sbt demoUplcJvm/test` runs:

- **`ConformanceSpec`** — the Plutus conformance suite (plutus `1.63.0.0`,
  vendored under `src/test/resources/plutus-conformance`, see its `SOURCE.md`).
  All 943 evaluation cases agree with the CEK machine; 55 parser tests and one
  program that is a bare free variable (no flat encoding exists for it) are
  skipped. The suite's `.expected` files additionally pin each case's
  success/failure, so the comparison cannot pass vacuously.
- **`ValidatorSpec`** — validators written in Scala and compiled to UPLC by the
  Scalus plugin (`demos/uplc-jvm-validators`: owner + greeting, hash preimage,
  time lock, 2-of-3 multisig, Fibonacci puzzle, HTLC), each with accepting and
  rejecting V3 script contexts. Every script is checked twice: compiled once
  from its CBOR form and applied to the context at run time, and translated
  with the context applied.
- **`CompilerSpec`** — one hand-written program per compilation path, error
  paths included, plus a 20 000-deep `let` chain.
- **`OpticSpec`** — the optic's shape (`translate` is `iso.modify`, the CBOR
  composition yields the identical class file) and evidence that hybrid typing
  fires on real validator code.

Every generated class also goes through ASM's bytecode verifier.

## Scope

- Results and failures are compared; **execution budgets are not metered**.
  Out-of-budget rejections, which the ledger applies, are a natural next step.
- Generated code recurses on the JVM stack where the CEK machine uses the heap,
  so `Exec` runs evaluations on a thread with a 512 MB stack.
- Traces are collected (`Exec.Outcome.logs`) but not compared.

## Running it

```sh
sbt demoUplcJvm/test
sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo"   # walkthrough + rough timing
demos/uplc-jvm/scripts/refresh-conformance.sh [plutus-tag]          # re-vendor the suite
```

`Demo` prints a validator's typed expression and times its validation through
both evaluators. On a development container the Fibonacci validator (`fib(400)`)
took ~0.45 ms per run on the CEK machine and ~0.045 ms through the generated
class. That is a single-threaded loop, not a JMH benchmark: read it as an order
of magnitude.

## Prior art

Scalus has its own experimental JIT (`scalus-uplc-jit-compiler`, built on
Scala 3 staging). This demo is independent of it: its point is the shape —
one optic from bytes to bytes, a typed intermediate form you can inspect, ASM
for the class file — and a differential harness that keeps it honest.
