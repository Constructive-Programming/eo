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
| `B` | `JvmProgram` | JVM-shaped code: static methods, local slots, explicit boxing |
| `T` | `Array[Byte]` | one self-contained class file |

and `modify(f) = reverseGet ∘ f ∘ get` is a compiler whenever `f: A => B` is a
lowering:

```scala
import dev.constructive.eo.demo.uplc.*

UplcJvm.iso: BijectionIso[Array[Byte], Array[Byte], TypedProgram, JvmProgram]

val translate: Array[Byte] => Array[Byte] = UplcJvm.iso.modify(Lower.lower)

Exec.validate(translate(script), scriptContext)   // Right(traces) | Left(reason)
```

Three things follow from the shape.

**The ends are fixed; the middle is a function.** Decoding and typing live in
`get`, bytecode emission in `reverseGet`. Anything `TypedProgram => JvmProgram`
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
<desc id="uplc-rt-desc">Top row, the optic: flat UPLC bytes go through iso.get to a typed expression, through Lower.lower to JVM-shaped code, and through iso.reverseGet to a class file. Bottom row, the check: the class file is loaded and run, its value is discharged back to a UPLC term, and that term must be alpha-equivalent to the term the Scalus CEK machine computes from the same bytes.</desc>
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
<text x="470" y="66" text-anchor="middle" style="fill: var(--text-color); font-weight: 600;">B · JVM code</text>
<text x="470" y="88" text-anchor="middle" style="fill: var(--text-color); font-size: 12px;">JvmProgram</text>
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

The listings in this section are printed, verbatim, by the demo's walkthrough —
all but the bytecode, which is `javap -c` on the class file it writes:

```sh
sbt "demoUplcJvm/Test/runMain dev.constructive.eo.demo.uplc.Demo <dir-for-the-class-file>"
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
- **multi-argument calls** — `case (constr 0 a1 … an) [f]`, the shape the
  Scalus compiler gives a call with several arguments (one `case` step applies
  them all);
- **`force (delay t)`** — just `t`.

Each rewrite is exact under the CEK machine's evaluation order: it only removes
steps nothing can observe (building a closure that is applied at once, delaying a
branch that is forced at once). Anything the typer doesn't recognise stays a
dynamic application and runs exactly as the machine would run it.

**Hybrid typing.** UPLC is untyped, yet most of a program's scalars are provable
locally — constants, typed builtin results, `let`s of those, branches that
agree. A type is a Scalus `DefaultUni`, and a builtin's signature is Scalus' own
`TypeScheme` for it. Integers, booleans, bytestrings and strings get Scala types
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
    case 1 => if equalsInteger(v4(unIData(headList(tailList(v6))), 0, 1), unIData({
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

The self-recursive helper `v4` is plain `if … then … else v4(v4, …)`; the
`unConstrData` / `sndPair` / `headList` chains are the script context being
decoded.

### Down: lowering (`A ⇒ B`)

`Lower.lower` is the function the optic is run with. It makes the JVM decisions:

- **Closure conversion.** Each lambda chain or `delay` becomes a static method;
  a closure captures exactly its free variables, in an order fixed by the
  *source* program, so the runtime can read the layout back.
- **Representation.** Typed values stay in unboxed locals and on the operand
  stack; boxing happens only where typed code meets dynamic code.
- **Fast paths.** Integer, bytestring and string builtins with a typed form
  become direct static calls on unboxed operands.
- **Method size.** Once a method has inlined enough code, the next `let` body
  moves to a method of its own (the JVM caps a method at 64 KB of bytecode).

For the small program, B is one method; `KU` is the class's table of unboxed
constants, indexed by UPLC node:

```
entry():
  s0: BigInt = KU[14]: BigInt
  s1: BigInt = multiplyInteger(s0, s0)
  box(addInteger(s0, s1))
```

### Back: JVM code to a class file (`reverseGet`)

`iso.reverseGet` writes one class file with
[Cojen Maker](https://github.com/cojen/Maker): the emitter writes each B node
as a variable and calls runtime methods by name, and the library picks the
overload, decides what lives in a local and what stays on the operand stack, and
computes the stack maps. The class has a static method per lambda chain and
`delay`, a switch per call arity for closure dispatch, and the flat program
itself, embedded as Base64 string constants. Here is the small program's `entry`
method from `javap -c` (constant-pool references shortened, comments added):

```
private static java.lang.Object entry();
   0: getstatic     KU                  // the class's unboxed constants
   3: bipush        14                  // the constant at UPLC node 14
   5: aaload
   6: checkcast     scala/math/BigInt
   9: astore_0                          // v0 = 6
  10: aload_0
  11: aload_0
  12: invokestatic  Rt.multiplyInteger  // (BigInt, BigInt) => BigInt
  15: aload_0                           // v1 = v0 * v0 never needs a local
  16: swap
  17: invokestatic  Rt.addInteger       // (BigInt, BigInt) => BigInt
  20: invokestatic  Rt.box              // the one box, at the result
  23: areturn
```

The integers never leave their unboxed form until the result is returned.

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
conformance suite under. Its runtime delegates builtins without a typed fast
path to Scalus' own implementations; the five that hand back one of their
arguments untouched (`ifThenElse`, `chooseList`, `chooseUnit`, `chooseData`,
`trace`) run natively, since those arguments may be closures.

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
OpenJDK 21.0.12, Scalus 1.3.0, Cojen Maker 2.8.4; three forks, 5 × 1 s measurement after 3 × 1 s
warm-up (5 × 2 s for the Fibonacci scaling runs, whose slow operations took
longer to reach steady state). A shared container is not a quiet benchmark
machine: read the ratios, not the absolute values.

### Validating a transaction

| validator | CEK machine (µs) | generated class (µs) | speed-up | CEK alloc/op | class alloc/op |
|---|--:|--:|--:|--:|--:|
| `helloOwner` | 4.70 ± 0.24 | 1.49 ± 0.09 | **3.2×** | 13.2 KB | 5.4 KB |
| `preimage` | 6.44 ± 0.25 | 2.19 ± 0.18 | **2.9×** | 17.8 KB | 7.5 KB |
| `timeLock` | 6.16 ± 0.30 | 2.21 ± 0.13 | **2.8×** | 17.7 KB | 7.8 KB |
| `multisig` | 13.4 ± 1.0 | 3.93 ± 0.25 | **3.4×** | 37.4 KB | 13.1 KB |
| `fibonacci` | 30.6 ± 1.7 | 1.52 ± 0.07 | **20.1×** | 76.9 KB | 5.4 KB |
| `htlc` | 8.41 ± 0.54 | 3.20 ± 0.16 | **2.6×** | 23.0 KB | 10.5 KB |

Most validators spend their time decoding the script context — `unConstrData`,
`headList`, `sndPair` and friends. Those builtins run the same Scalus code in
both evaluators, so compilation saves only the interpretation *around* them:
**2.6–3.4×**, with 2–3× less allocation. Where the work is the script's own
logic, interpretation is most of the cost, and typed fast paths and direct calls
remove it: the Fibonacci puzzle (`n = 30`) validates **~20×** faster and
allocates 14× less.

### Scaling with the work: the Fibonacci puzzle

| `n` | CEK machine (µs) | generated class (µs) | speed-up | CEK alloc/op | class alloc/op |
|--:|--:|--:|--:|--:|--:|
| 10 | 12.5 ± 0.5 | 1.08 ± 0.06 | **11.6×** | 31.8 KB | 3.8 KB |
| 100 | 89.5 ± 4.8 | 3.45 ± 0.10 | **25.9×** | 235.1 KB | 12.2 KB |
| 1000 | 976.8 ± 73.9 | 52.1 ± 2.2 | **18.8×** | 2,502.3 KB | 185.6 KB |

The speed-up grows from 12× to 26× as fixed per-validation costs (building the
script's closures, decoding the context) are amortised over more recursion. At
`n = 1000` the integers reach ~700 bits; `BigInt` arithmetic — identical in both
evaluators — takes a larger share of the time, and the speed-up settles at ~19×.

### What translation costs

| validator | flat UPLC (S) | class file (T) | translate (µs) | translate + load (µs) | break-even (validations) |
|---|--:|--:|--:|--:|--:|
| `helloOwner` | 147 B | 5,158 B | 220.4 ± 24.3 | 767.8 ± 97.7 | 239 |
| `preimage` | 184 B | 5,640 B | 292.8 ± 31.2 | 944.0 ± 99.1 | 222 |
| `timeLock` | 181 B | 6,110 B | 352.0 ± 41.0 | 950.6 ± 130.4 | 241 |
| `multisig` | 200 B | 6,446 B | 341.9 ± 74.9 | 997.0 ± 103.4 | 106 |
| `fibonacci` | 124 B | 4,423 B | 162.3 ± 22.8 | 681.4 ± 112.8 | 23 |
| `htlc` | 307 B | 9,859 B | 584.8 ± 50.6 | 1,343.0 ± 76.8 | 258 |

`translate` is the optic (`iso.modify(Lower.lower)`), measured warm;
`translate + load` adds defining the class, verifying it, and running its static
initialiser, which re-decodes the embedded program. Loading is two thirds of the
total. Translating costs roughly 3× what it did with a raw-ASM emitter twice the
size: Cojen Maker's conveniences (resolving calls by reflection, allocating
locals) are paid at translation time, once per script. The break-even column
divides `translate + load` by the time each validation saves: a script must be
validated **~25–260 times** before compiling it pays for itself. That fits
scripts that run over and over — a node re-validating the same reference
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
