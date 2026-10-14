# UPLC → JVM bytecode

> **A demo, not a module.** The code lives in
> [`demos/uplc-jvm`](https://github.com/Constructive-Programming/eo/tree/main/demos/uplc-jvm)
> and is not published. It shows what a single optic buys a real program: a
> compiler from Untyped Plutus Core (UPLC) — the language Cardano smart
> contracts run in — to JVM class files, checked against the
> [Scalus](https://scalus.org) CEK machine on every program it compiles.

## The idea: a compiler is an optic

An `Iso[S, T, A, B]` is two functions, `get: S => A` and `reverseGet: B => T`,
and nothing requires `S` to equal `T` or `A` to equal `B`. Pick the four types
like this:

| | type | what it is |
|---|---|---|
| `S` | `Array[Byte]` | a flat-encoded UPLC program |
| `A` | `TypedProgram` | the program read back as a typed, Scala-like expression |
| `B` | `ClassNode` | the bytecode, as [ASM](https://asm.ow2.io)'s in-memory class |
| `T` | `Array[Byte]` | one self-contained class file |

and `modify(f) = reverseGet ∘ f ∘ get` is a compiler whenever `f: A => B` is a
lowering:

```scala
import dev.constructive.eo.demo.uplc.*

UplcJvm.iso: BijectionIso[Array[Byte], Array[Byte], TypedProgram, ClassNode]

val translate: Array[Byte] => Array[Byte] = UplcJvm.iso.modify(Lower.lower)

Exec.validate(translate(script), scriptContext)   // Right(traces) | Left(reason)
```

Three things follow from the shape.

**The ends are fixed; the middle is a function.** Decoding and typing live in
`get`, writing the class file in `reverseGet`. Anything `TypedProgram => ClassNode`
can run through the optic, so an optimisation pass is a function composed in
front of the lowering — `iso.modify(optimise andThen Lower.lower)` — and
neither end changes.

**It composes like any other optic.** Scripts on chain are flat bytes wrapped in
a CBOR bytestring. Unwrapping them is another polymorphic iso, and `andThen`
yields a single optic from on-chain bytes to class files:

```scala
val cborScript: BijectionIso[Array[Byte], Array[Byte], Array[Byte], Array[Byte]] =
  Iso(cbor => Cbor.decode(cbor).to[Array[Byte]].value, classFile => classFile)

val onChain = cborScript.andThen(UplcJvm.iso)       // CBOR in, class file out
onChain.modify(Lower.lower)(script.cborEncoded)    // the same class file as translate(flat)
```

**The law is a commuting square.** A polymorphic iso has no round-trip law to
check: with `S ≠ T` there is nothing to round-trip to. The property that matters
is semantic — *running `translate(s)` gives what a reference evaluator gives for
`s`* — and the demo checks exactly that, against the Scalus CEK machine, for
every program in its test-suite ([Correctness](#correctness-the-cek-machine-as-oracle)).

## Back and forth

<div class="uplc-roundtrip" style="max-width: 780px; margin: 1.5em auto;">
<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 780 320" width="100%" role="img" aria-labelledby="uplc-rt-title uplc-rt-desc" style="font-family: inherit;">
<title id="uplc-rt-title">The UPLC to JVM round trip</title>
<desc id="uplc-rt-desc">Top row, the optic: flat UPLC bytes go through iso.get to a typed expression, through Lower.lower to bytecode (an ASM ClassNode), and through iso.reverseGet to a class file. Bottom row, the check: the class file is loaded and run, its value is discharged back to a UPLC term, and that term must be alpha-equivalent to the term the Scalus CEK machine computes from the same bytes.</desc>
<defs><marker id="uplc-rt-arrow" viewBox="0 0 10 10" refX="9" refY="5" markerWidth="7" markerHeight="7" orient="auto-start-reverse"><path d="M0,0 L10,5 L0,10 z" style="fill: var(--primary-color);"/></marker></defs>
<rect x="6" y="24" width="768" height="100" rx="14" style="fill: none; stroke: var(--primary-medium); stroke-dasharray: 6 5;"/>
<text x="18" y="16" style="fill: var(--primary-color); font-size: 13px; font-weight: 600;">one eo Iso · the compiler is iso.modify(Lower.lower)</text>
<g style="font-size: 14px;">
<rect x="20" y="40" width="140" height="68" rx="8" style="fill: var(--component-area-bg); stroke: var(--component-border);"/>
<text x="90" y="66" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">S · flat UPLC</text>
<text x="90" y="88" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">Array[Byte]</text>
<rect x="210" y="40" width="140" height="68" rx="8" style="fill: var(--component-area-bg); stroke: var(--component-border);"/>
<text x="280" y="66" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">A · typed expr</text>
<text x="280" y="88" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">TypedProgram</text>
<rect x="400" y="40" width="140" height="68" rx="8" style="fill: var(--component-area-bg); stroke: var(--component-border);"/>
<text x="470" y="66" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">B · bytecode</text>
<text x="470" y="88" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">ClassNode</text>
<rect x="620" y="40" width="140" height="68" rx="8" style="fill: var(--component-area-bg); stroke: var(--component-border);"/>
<text x="690" y="66" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">T · class file</text>
<text x="690" y="88" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">Array[Byte]</text>
<rect x="20" y="230" width="140" height="68" rx="8" style="fill: var(--bg-color); stroke: var(--component-border);"/>
<text x="90" y="256" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">UPLC term</text>
<text x="90" y="278" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">CEK result</text>
<rect x="210" y="230" width="140" height="68" rx="8" style="fill: var(--primary-light); stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="280" y="256" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">must agree</text>
<text x="280" y="278" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">α-equal, or both fail</text>
<rect x="400" y="230" width="140" height="68" rx="8" style="fill: var(--bg-color); stroke: var(--component-border);"/>
<text x="470" y="256" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">UPLC term</text>
<text x="470" y="278" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">discharged</text>
<rect x="620" y="230" width="140" height="68" rx="8" style="fill: var(--bg-color); stroke: var(--component-border);"/>
<text x="690" y="256" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">runtime value</text>
<text x="690" y="278" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">closure, constant, …</text>
</g>
<g style="font-size: 12px; fill: var(--primary-color);">
<line x1="162" y1="74" x2="206" y2="74" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="184" y="66" text-anchor="middle">get</text>
<line x1="352" y1="74" x2="396" y2="74" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="374" y="66" text-anchor="middle">lower</text>
<line x1="542" y1="74" x2="616" y2="74" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="579" y="66" text-anchor="middle">reverseGet</text>
<line x1="690" y1="110" x2="690" y2="226" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="698" y="173" text-anchor="start">load + run</text>
<line x1="618" y1="264" x2="544" y2="264" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="580" y="256" text-anchor="middle">discharge</text>
<line x1="398" y1="264" x2="354" y2="264" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<line x1="162" y1="264" x2="206" y2="264" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<line x1="90" y1="110" x2="90" y2="226" marker-end="url(#uplc-rt-arrow)" style="stroke: var(--primary-color); stroke-width: 1.6;"/>
<text x="98" y="165" text-anchor="start">Scalus CEK</text>
<text x="98" y="181" text-anchor="start">machine</text>
</g>
<text x="375" y="173" text-anchor="middle" style="fill: var(--text-color); font-size: 12px; opacity: 0.75;">forth along the top, back along the bottom</text>
</svg>
</div>

The listings in this section are printed, verbatim, by the demo's walkthrough
(give it a directory and it also writes the class file there, for `javap`):

```sh
sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo [dir-for-the-class-file]"
```

### Forth: bytes to a typed expression (`get`)

UPLC has no `let`, no `if` and no multi-argument call; compilers encode them in
lambdas, `delay`/`force` and builtins. `let x = 6 in let y = x * x in x + y`
arrives as:

```
(program 1.1.0 [(lam x [(lam y [(builtin addInteger) x y]) [(builtin multiplyInteger) x x]]) (con integer 6)])
```

`iso.get` decodes the flat bytes into Scalus' own `Term` tree and reads it back
as a typed, Scala-like expression:

```scala
val v0: BigInt = 6
val v1: BigInt = multiplyInteger(v0, v0)
addInteger(v0, v1)
```

The typer (`Typer.scala`) recognises the encodings UPLC compilers emit and
records them as what they mean:

- **`let`** — an immediately applied lambda chain, `(λx1 … λxn. body) a1 … an`;
- **`if`** — `force (ifThenElse c (delay a) (delay b))`, and the same idiom for
  `chooseList`, `chooseUnit` and `chooseData`;
- **builtin calls** — a builtin applied to exactly its forces and arguments;
- **direct calls** — of `let`-bound functions and of `let`-bound, already forced
  builtins;
- **recursion** — `let f = λself … in … [f f] …`, the knot Scalus ties for a
  recursive function: when `f` and `self` are only ever applied to themselves,
  `self` always holds the function, and its calls are direct calls too;
- **multi-argument calls** — `case (constr k a1 … an) [… f …]`, the shape the
  Scalus compiler gives a call with several arguments (one `case` step applies
  them all), when `f` is a lambda or a known function;
- **`force (delay t)`** — just `t`.

Each rewrite is exact under the CEK machine's evaluation order: it only removes
steps nothing can observe (building a closure that is applied at once, delaying a
branch that is forced at once). Anything the typer doesn't recognise stays a
dynamic application and runs exactly as the machine would run it.

**Hybrid typing.** UPLC is untyped, yet most of a program's scalars are provable
locally — constants, typed builtin results, `let`s of those, branches that
agree. A type is a Scalus `DefaultUni`, and a builtin's signature is Scalus' own
`TypeScheme` for it, instantiated at its arguments' types — `fstPair` of the
`pair integer (list data)` that `unConstrData` returns is an `integer`, so the
`case` that matches a constructor tag on it compiles to a JVM switch. Integers,
booleans, bytestrings and strings get Scala types
(`BigInt`, `Boolean`, `ByteString`, `String`) and an unboxed JVM
representation. Everything else — functions, constructors, `Data`,
whatever flows through a lambda parameter — is a `Value`: a boxed Scalus
`Constant` or a runtime closure.

On real code — the Fibonacci puzzle validator, written in Scala and compiled to
UPLC by the Scalus plugin — the typed expression makes the program's structure
readable again:

```scala
val v0: Value = builtin.fstPair.force.force
val v1: Value = builtin.headList.force
val v2: Value = builtin.sndPair.force.force
val v3: Value = builtin.tailList.force
val v4: Value = {
  val v4: Value = (v4: Value, v5: Value, v6: Value, v7: Value) =>
    if lessThanEqualsInteger(v5, 0) then v6
    else v4(v4, subtractInteger(v5, 1), v7, addInteger(v6, v7))
  v4(v4)
}
(v5: Value) =>
  val v6: Value = sndPair(unConstrData(v5))
  val v7: Value = unConstrData(headList(dropList(2, v6)))
  fstPair(v7) match
    case 0 => error()(3)
    case 1 => if equalsInteger(Constr0(unIData(headList(tailList(v6))), 0, 1) match
        case 0 => v4, unIData({
        val v8: Value = unConstrData(headList(tailList(sndPair(v7))))
        fstPair(v8) match
          case 0 => headList(sndPair(v8))
          case 1 => error()
      })) then ()
      else error()
    case 2 => error()
    case 3 => error()
    case 4 => error()
    case 5 => error()
```

The self-recursive helper `v4` is plain `if … then … else v4(v4, …)`, and its
recursive call is a direct JVM call; the `unConstrData` / `sndPair` /
`headList` chains are the script context being decoded, and the `match`es on
`fstPair` are switches on constructor tags. The one call into `v4` goes through
a value the typer cannot see into (`v4(v4)`, a partial application), so it stays
a dynamic `case` — fields first, then the branch — exactly as the machine runs
it.

### Down: lowering (`A ⇒ B`)

`Lower.lower` is the function the optic is run with. It writes bytecode
straight into ASM's tree (`ClassNode`, with asm-commons' `InstructionAdapter`)
and makes the JVM decisions on the way:

- **Closure conversion.** Each lambda chain or `delay` becomes a static method;
  a closure captures exactly its free variables, in an order fixed by the
  *source* program, so the runtime can read the layout back.
- **Representation.** Typed values stay in unboxed locals and on the operand
  stack; boxing happens only where typed code meets dynamic code.
- **Direct calls.** A saturated builtin is a plain call of the function Scalus'
  own `CardanoBuiltins` wraps (`scalus.uplc.builtin.Builtins`), on unboxed
  operands where its signature allows; calls of known functions are
  `invokestatic`s of their body methods.
- **Method size.** Once a method has inlined enough code, the next `let` body
  moves to a method of its own (the JVM caps a method at 64 KB of bytecode).

For the small program, B is one method, as ASM prints it; `K` is the class's
table of constants, indexed by UPLC node:

```
    GETSTATIC dev/constructive/eo/demo/uplc/generated/UplcProgram.K : [Ljava/lang/Object;
    BIPUSH 14
    AALOAD
    INVOKESTATIC dev/constructive/eo/demo/uplc/runtime/Rt.unInt (Ljava/lang/Object;)Lscala/math/BigInt;
    ASTORE 1
    GETSTATIC scalus/uplc/builtin/Builtins$.MODULE$ : Lscalus/uplc/builtin/Builtins$;
    ALOAD 1
    ALOAD 1
    INVOKEVIRTUAL scalus/uplc/builtin/AbstractBuiltins.multiplyInteger (Lscala/math/BigInt;Lscala/math/BigInt;)Lscala/math/BigInt;
    ASTORE 2
    GETSTATIC scalus/uplc/builtin/Builtins$.MODULE$ : Lscalus/uplc/builtin/Builtins$;
    ALOAD 1
    ALOAD 2
    INVOKEVIRTUAL scalus/uplc/builtin/AbstractBuiltins.addInteger (Lscala/math/BigInt;Lscala/math/BigInt;)Lscala/math/BigInt;
    INVOKESTATIC dev/constructive/eo/demo/uplc/runtime/Rt.boxInt (Lscala/math/BigInt;)Ljava/lang/Object;
    ARETURN
```

The constant is unboxed once, the arithmetic is Scalus' own, and the one box is
at the result.

### Back: bytecode to a class file (`reverseGet`)

`iso.reverseGet` hands the `ClassNode` to ASM's `ClassWriter`, which computes
the stack map frames and serialises it. The class has a static method per lambda
chain and `delay`, a switch per call arity for closure dispatch, and the flat
program itself, embedded as Base64 string constants and re-decoded by the static
initialiser.

### And back again: values to UPLC terms

A UPLC program can evaluate to a closure, and to compare such a result with the
CEK machine's it must become a term again. The compiler numbers every UPLC node
in pre-order; a runtime closure carries only the number of its source node, plus
the values of its free variables. The class re-decodes the program it embeds,
so the runtime can *discharge* a closure — rebuild its source node with every
captured variable replaced by its own discharged value — exactly as the CEK
machine discharges its results:

```
(program 1.1.0 [(lam x (lam y x)) (con integer 5)])

run + discharge: (lam i0 (con integer 5))
CEK machine:     (lam i0 (con integer 5))
α-equal:         true
```

## Correctness: the CEK machine as oracle

The demo pins one semantics (`Semantics.scala`): Plutus V3 at protocol version
11, builtin semantics variant E — the configuration Scalus 1.3.0 runs the Plutus
conformance suite under. Builtins without a direct implementation, and builtins
used as values, run Scalus' own CEK meanings (`BuiltinRuntime`); a meaning that
hands back one of its arguments (`ifThenElse`, `chooseList`, …) hands back the
closure or constructor it was given.

`sbt demoUplcJvm/test` checks the commuting square on:

| suite | what | result |
|---|---|---|
| Plutus conformance | the [`plutus-conformance`](https://github.com/IntersectMBO/plutus/tree/1.63.0.0/plutus-conformance) UPLC evaluation cases, plutus `1.63.0.0` (vendored) | **943 / 943** evaluation cases agree; 55 parser-only tests and 1 program with no flat encoding skipped |
| validators | owner + greeting, hash preimage, time lock, 2-of-3 multisig, Fibonacci puzzle, HTLC — accepting *and* rejecting V3 transactions | **20 / 20** scenarios reach the CEK machine's verdict, both compiled once and applied at run time and translated with the context applied |
| compiler paths | one hand-written program per path, error paths included, a 20 000-deep `let` chain | all agree |

The JVM's own verifier checks every generated class as it is loaded, and each
conformance case's `.expected` file pins whether it should succeed or fail — so
the harness cannot pass by having both evaluators fail everything. Both sides
report in Scalus' types: the generated code fails with a `MachineError`, as the
CEK machine does, and `Exec.run` returns an evaluation `Result`.

## Benchmarks

JMH, in
[`demos/uplc-jvm-bench`](https://github.com/Constructive-Programming/eo/tree/main/demos/uplc-jvm-bench).
Both evaluators validate the same accepting transaction (setup checks that both
accept it before anything is timed):

- **CEK machine** — Scalus' `PlutusVM.evaluateScript` on the script applied to
  its context, with no budget accounting (`NoBudgetSpender`). The generated code
  meters nothing either; with the restricting budget spender the ledger uses,
  the CEK machine would do a little more work per step.
- **generated class** — translated and loaded once, in setup. Each operation
  evaluates the script and applies it to the context.

Average time per operation in **µs (lower is better)**, error = 99.9 %
confidence half-width; allocation per operation from `-prof gc`
(`gc.alloc.rate.norm`). Measured on a 4-vCPU Intel Xeon (2.1 GHz) container,
OpenJDK 21.0.12, Scalus 1.3.0, ASM 9.11; three forks, 5 × 1 s measurement after 3 × 1 s
warm-up (5 × 2 s for the Fibonacci scaling runs, whose slow operations took
longer to reach steady state). A shared container is not a quiet benchmark
machine: read the ratios, not the absolute values.

### Validating a transaction

| validator | CEK machine (µs) | generated class (µs) | speed-up | CEK alloc/op | class alloc/op |
|---|--:|--:|--:|--:|--:|
| `helloOwner` | 5.01 ± 0.23 | 0.32 ± 0.01 | **15.7×** | 13.3 KB | 1.0 KB |
| `preimage` | 7.11 ± 0.56 | 0.63 ± 0.04 | **11.3×** | 17.8 KB | 1.7 KB |
| `timeLock` | 6.96 ± 0.67 | 0.33 ± 0.02 | **20.9×** | 17.7 KB | 0.9 KB |
| `multisig` | 14.6 ± 0.9 | 1.07 ± 0.10 | **13.6×** | 37.4 KB | 2.7 KB |
| `fibonacci` | 34.2 ± 2.0 | 0.54 ± 0.03 | **63.2×** | 76.8 KB | 1.7 KB |
| `htlc` | 9.27 ± 0.85 | 1.16 ± 0.05 | **8.0×** | 23.0 KB | 2.9 KB |

Most validators spend their time decoding the script context — `unConstrData`,
`headList`, `sndPair` and friends. The CEK machine runs each one as a builtin
meaning over CEK values, and `unConstrData` turns a constructor's whole field
list into constants. The generated class calls the functions those meanings
wrap directly, reads `Data` lazily — only the fields a script touches become
constants — and matches constructor tags with JVM switches: **8–21×** faster,
with 8–20× less allocation. Where the work is the script's own logic,
interpretation is nearly all of the cost: the Fibonacci puzzle (`n = 30`), a
self-recursive function compiled to direct calls on unboxed integers, validates
**~63×** faster. Against the demo's first, raw-ASM version (same harness, an
earlier run), validation is **2.9–7.1×** faster.

### Scaling with the work: the Fibonacci puzzle

| `n` | CEK machine (µs) | generated class (µs) | speed-up | CEK alloc/op | class alloc/op |
|--:|--:|--:|--:|--:|--:|
| 10 | 15.3 ± 1.6 | 0.27 ± 0.02 | **56.7×** | 31.8 KB | 0.9 KB |
| 100 | 106.0 ± 5.4 | 2.03 ± 0.15 | **52.3×** | 235.2 KB | 5.8 KB |
| 1000 | 1,045.1 ± 45.2 | 49.8 ± 3.4 | **21.0×** | 2,356.0 KB | 143.9 KB |

The generated code does almost nothing but arithmetic, so the speed-up stays
above 50× until the integers grow: at `n = 1000` they reach ~700 bits, `BigInt`
addition — identical in both evaluators — is most of the time, and the speed-up
settles at ~21×.

### What translation costs

| validator | flat UPLC (S) | class file (T) | translate (µs) | translate + load (µs) | break-even (validations) |
|---|--:|--:|--:|--:|--:|
| `helloOwner` | 147 B | 4,565 B | 80.8 ± 9.9 | 380.8 ± 58.7 | 81 |
| `preimage` | 184 B | 5,035 B | 124.3 ± 44.1 | 468.3 ± 64.5 | 72 |
| `timeLock` | 181 B | 5,109 B | 113.5 ± 52.3 | 431.6 ± 47.9 | 65 |
| `multisig` | 200 B | 6,238 B | 148.3 ± 13.3 | 628.4 ± 47.6 | 46 |
| `fibonacci` | 124 B | 4,347 B | 68.9 ± 6.5 | 337.1 ± 45.1 | 10 |
| `htlc` | 307 B | 6,666 B | 172.2 ± 16.6 | 666.2 ± 79.6 | 82 |

`translate` is the optic (`iso.modify(Lower.lower)`), measured warm;
`translate + load` adds defining the class, verifying it, and running its static
initialiser, which re-decodes the embedded program. Loading is three quarters of
the total. Most of `translate` is ASM's `ClassWriter` computing stack map frames;
lowering straight into ASM's tree keeps the rest small, and translation costs
what the demo's first, raw-ASM emitter did — about a third of what a version
built on a higher-level bytecode library (Cojen Maker) cost. The break-even
column divides `translate + load` by the time each validation saves: a script
must be validated **~10–80 times** before compiling it pays for itself. That
fits scripts that run over and over — a node re-validating the same reference
scripts, an emulator, a test-suite — rather than one-off evaluation.

Reproduce with `sbt "demoUplcJvmBench/Jmh/run -prof gc"`.

## Limits

- **No budget metering.** Results and failures are compared, execution budgets
  are not, so an out-of-budget script — which the ledger rejects — is accepted
  here. Metering the same machine-step costs the CEK machine charges is the
  natural next step.
- **The JVM stack.** Generated code recurses on the JVM stack where the CEK
  machine uses the heap; `Exec` runs evaluations on a thread with a 512 MB stack.
- **Method splitting** happens at `let` boundaries only — the shape compilers
  produce — not inside one huge expression.

Scalus has its own experimental JIT (`scalus-uplc-jit-compiler`, built on Scala 3
staging). This demo is independent of it; its point is the shape — one optic
from bytes to bytes, a typed middle you can read — and a differential harness
that keeps it honest.

## Running it

```sh
# the differential suite
sbt demoUplcJvm/test
# the walkthrough above
sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo"
# the benchmarks
sbt "demoUplcJvmBench/Jmh/run -prof gc"
# re-vendor the conformance suite
demos/uplc-jvm/scripts/refresh-conformance.sh [plutus-tag]
```

The demo and its benchmarks sit outside the root aggregate (Scalus brings JNI
crypto bindings with it); CI runs the differential suite as its own step.
