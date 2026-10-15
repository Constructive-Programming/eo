# UPLC → JVM bytecode, as eo optics

A compiler from Untyped Plutus Core (UPLC, the language Cardano smart contracts
run in) to JVM class files, built from cats-eo optics. Correctness is
established differentially: every program runs through the generated class AND
through the [Scalus](https://scalus.org) CEK machine, and the two must agree.

```scala
import dev.constructive.eo.demo.uplc.*

val script = CborScript.of(program)            // or CborScript.parse(bytes): Either[String, CborScript]

UplcJvm.toTyped     // CborScript ⇀ TypedProgram: the program as a typed, Scala-like expression
UplcJvm.toJvmClass  // CborScript ⇀ JvmClass: its JVM class, in memory (ASM's class tree)
UplcJvm.toBytecode  // CborScript ⇀ JvmClassScript: its class file

UplcJvm.toBytecode.getOption(script) match      // the compiler
  case Some(classFile) =>
    UplcJvm.toBytecode.reverseGet(classFile)    // == script: the class carries its script
    Exec.validate(classFile, scriptContext)     // Right(traces) | Left(reason), CIP-117
  case None => // a Plutus Core version the compiler does not know, or a class too large for a file
```

The three optics a caller wants all start at the script as the ledger stores it,
and each is a chain of the compiler's stages — all three prisms:

```
CborScript ⇀ TypedProgram ≅ JvmClass ⇀ JvmClassScript
           toTyped        lowering   compiling
```

| type | what it is |
|---|---|
| `CborScript` | a script as the ledger stores it: the flat-encoded program in a CBOR bytestring (opaque, over Scalus' `ByteString`) |
| `TypedProgram` | the program read back as a typed, Scala-like expression tree |
| `JvmClass` | the script's JVM class in memory — [ASM](https://asm.ow2.io)'s `ClassNode`, inspectable before it is a file (opaque; only the compiler and `UplcJvm.decompile` make one) |
| `JvmClassScript` | a class file the compiler wrote (opaque, over Scalus' `ByteString`) |

Every stage keeps the script it came from: a typed program its source, a class
the script itself, embedded in its constants. So each arrow can be walked back.
`lowering` is a lawful iso on what the compiler produces — there and back is the
identity, both ways. The other two are prisms, total on the way back:

- `toTyped`: a script can declare any Plutus Core version, and Scalus decodes
  them all; the ledger only takes the ones Plutus V3 has at protocol version 11,
  1.0.0 and 1.1.0 (`Semantics.versions`). A script of another version has no
  typed program.
- `compiling`: the JVM caps a method at 64 KB of bytecode and a class at 65 535
  constants, so a class can be too large for a class file, and then there is
  none (`UplcJvm.compile` says which limit, as a `Left`).

So the compiler, `toBytecode`, is a prism too. The suite checks the prism laws
with cats-eo's own `PrismTests`, and the round trips on every program it
compiles. None of the four types hands out what it holds — `bytes`
and `tree` are copies — so a value cannot change after the compiler made or
checked it, and the way back always works. The other property that matters is
the commuting square: *running `toBytecode.getOption(s)` gives what the CEK
machine gives for `s`*.

## Pipeline

```
CborScript ──decode──▶ SourceProgram ──Typer──▶ TypedProgram ──Lower──▶ JvmClass ──ClassWriter──▶ JvmClassScript
             (Scalus Terms, pre-order    (re-sugaring,           (closure conversion,      (stack map
              ids shared with runtime)    hybrid typing)           boxing, direct calls)     frames)
```

- **`SourceProgram`** is Scalus' own decoded `Term` tree with every node
  numbered in pre-order. That numbering is the contract between compile time and
  run time: a closure carries only its node id; the class embeds the script
  and re-decodes it, so a result can be *discharged* back to a UPLC term
  exactly as the CEK machine does.
- **`Typer`** (`toTyped.getOption`) recognises what UPLC compilers emit for constructs UPLC
  lacks, using rewrites that are exact under CEK evaluation order: `let`
  (immediately applied lambda chains), `if` (forced `ifThenElse` over delayed
  branches, and the other selector builtins), saturated builtin calls, direct
  calls of `let`-bound functions and of `let`-bound forced builtins, the
  `case (constr k args…) [… f …]` multi-argument call, `force (delay t)`, and
  recursion tied by self-application (`let f = λself … in … [f f] …`, which
  Scalus emits for recursive functions) — its calls become direct calls too.
- **Hybrid typing.** A static type is a Scalus `DefaultUni`, and builtin
  signatures are Scalus' own `TypeScheme`s: a polymorphic builtin's result type
  is the scheme instantiated at its arguments' types, so `fstPair (unConstrData
  d)` is an `integer` and a `case` on it is a JVM switch. Integers, booleans,
  bytestrings and strings get unboxed representations (`BigInt`, `boolean`,
  `ByteString`, `String`) wherever the typer can prove them — constants, typed
  builtin results, `let`s of those, agreeing branches. Everything else —
  functions, constructors, `Data`, values that flow through lambda parameters —
  is a boxed Scalus `Constant` or a runtime closure. Boxing happens only where
  the two meet.
- **`Lower`** (`lowering.get`) writes bytecode straight into ASM's tree with
  asm-commons' `InstructionAdapter`. It converts closures (each captures exactly
  its free variables, in the order `SourceProgram.captures` fixes for both
  sides), inserts box/unbox, calls builtins directly, and moves a `let` body into
  its own method once a method nears the JVM's 64 KB limit. The class holds a
  static method per lambda chain / `delay`, a switch per call arity for closure
  dispatch, and the script as Base64 string constants — what
  `lowering.reverseGet` reads back.
- **`compiling`** is ASM's `ClassWriter` (`UplcJvm.compile`, which computes the
  stack map frames, or says the class is too large) one way and `ClassReader`
  (`UplcJvm.decompile`) the other. A class file survives being read and written
  again byte for byte, because the generated code has no unreachable
  instructions: an expression that can only fail is never followed by code.
- **Stack safety.** Compiling cannot overflow the JVM stack, however deep the
  program. Decoding, typing and lowering are walks of `Step`s (`Step.scala`):
  plain calls while a walk is shallow; from 128 calls deep, the rest runs on
  the heap, from `cats.Eval`'s loop. A program nested 100 000 deep compiles on
  a thread's default 1 MB stack, and a shallow one pays next to nothing for
  it.
- **Runtime** (`runtime/`): closures, partial builtins, constructors, `case`
  dispatch (including PV11 casing on builtin values), discharge. A saturated
  builtin is a plain JVM call of the function Scalus' own `CardanoBuiltins`
  wraps (`scalus.uplc.builtin.Builtins`), on unboxed values where its signature
  allows. The list and pair builtins are runtime natives that read `Data` lazily:
  `unConstrData` / `unListData` do not convert a whole field list to constants,
  only the elements read. Every other builtin — and any builtin used as a value
  — runs Scalus' CEK meaning (`BuiltinRuntime`). Failures are Scalus'
  `MachineError`, and `Exec.run` returns Scalus' evaluation `Result` — the types
  the CEK machine reports with.

## Correctness

The semantics are pinned in `Semantics.scala`: Plutus V3 at protocol version 11
("van Rossem"), builtin semantics variant E — the configuration Scalus 1.3.0
runs the Plutus conformance suite under. The oracle and the generated code's
runtime both read it from there.

`sbt demoUplcJvm/test` runs the suites below. Every program any of them runs is
also checked for the round trips: its script comes back out of its class
(`toBytecode.reverseGet`), and its class file survives ASM reading and writing it
again (`compiling`).

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
  and applied to the context at run time, and compiled with the context
  applied.
- **`CompilerSpec`** — one hand-written program per compilation path, error
  paths included, plus a 20 000-deep `let` chain.
- **`OpticSpec`** — the prism laws of `toTyped` and `toBytecode`, with
  cats-eo-laws' `PrismTests` (a script of an unknown version and a program too
  large for a class file among the scripts); that scripts, classes and class
  files hand out copies; programs nested 100 000 deep compiled on a 1 MB stack;
  and evidence that hybrid typing fires on real validator code.

The JVM's bytecode verifier checks every generated class when `Exec.load`
defines it.

## Scope

- **No optimisation.** The compiler translates UPLC, it does not optimise it:
  no constant folding, inlining, dead-code elimination, common-subexpression
  elimination, specialisation or tail-call elimination. Every operation the
  program performs still happens at run time, in the CEK machine's order; the
  typer's rewrites read encodings back, they don't change the program. The
  speed-ups come from running as JVM bytecode under the JIT, with values
  unboxed and calls made directly where the types allow. The demo's own Scala
  code is built with Scala 3's bytecode optimizer (`-opt -opt-inline:**,!scala.**`;
  inlining from the Scala library measured slower — see `build.sbt`).
- Results and failures are compared; **execution budgets are not metered**.
  Out-of-budget rejections, which the ledger applies, are a natural next step.
- Methods are split at `let` boundaries only — the shape compilers produce —
  not inside one huge expression. A class that still exceeds a JVM limit has no
  class file: `toBytecode.getOption` is `None`.
- Compiling is stack safe, but Scalus' flat decoder and encoder, which
  `CborScript.parse`, `CborScript.of` and `toTyped` call, recurse: on a 1 MB
  stack they overflow between 2 000 and 20 000 levels deep, depending on how
  warm the JIT is.
- Generated code recurses on the JVM stack where the CEK machine uses the heap,
  so `Exec` runs evaluations on a thread with a 512 MB stack.
- Traces are collected (`Result.logs`) but not compared.

## Running it

```sh
sbt demoUplcJvm/test
sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo"   # the round trip, stage by stage
sbt "demoUplcJvmBench/Jmh/run -prof gc"                             # JMH: CEK vs generated class
demos/uplc-jvm/scripts/refresh-conformance.sh [plutus-tag]          # re-vendor the suite
```

`Demo` prints the round trip stage by stage: UPLC text, the typed expression,
the lowered bytecode (as ASM prints it), the class file, and the discharged
result next to the CEK machine's. Pass a directory to also write the
class file there for `javap`.

## Benchmarks

`demos/uplc-jvm-bench` is a JMH project (outside the root aggregate and the
library's nightly sweep): the CEK machine against the generated class for every
validator, the Fibonacci puzzle at growing `n`, and what translation itself
costs. The numbers and how they were taken are on the
[docs page](../../site/docs/cookbook/uplc-jvm.md#benchmarks).

```sh
sbt "demoUplcJvmBench/Jmh/run -prof gc"
```

## Prior art

Scalus has its own experimental JIT (`scalus-uplc-jit-compiler`, built on
Scala 3 staging). This demo is independent of it: its point is the shape —
one optic from bytes to bytes, a typed intermediate form you can inspect — and
a differential harness that keeps it honest.
