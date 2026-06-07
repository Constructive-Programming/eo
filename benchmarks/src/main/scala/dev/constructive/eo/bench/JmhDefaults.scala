package dev.constructive.eo
package bench

/** Marker trait for the project's benchmark classes.
  *
  * '''2026-04-27 walk-back.''' This trait once carried the shared JMH preamble — `@State`,
  * `@BenchmarkMode`, `@OutputTimeUnit`, `@Fork`, `@Warmup`, `@Measurement` — on the assumption that
  * JMH's processor inherits class-level annotations from a `trait` parent. It does not: Scala 3
  * does not propagate trait annotations onto the implementing class's bytecode, and JMH's
  * `BenchmarkProcessor` reads annotations off the concrete bench class only (the JMH annotations
  * lack `@Inherited`, so even an `abstract class` parent wouldn't help). The result was a broken
  * `benchmarks/Jmh/run` for every bench in the suite — every fixture field surfaced as "declared
  * within a class not having @State annotation".
  *
  * The 6-annotation preamble lives directly on each subclass again. The trait stays as a marker so
  * `extends JmhDefaults` reads as documentation ("this is a JMH bench class with the project's
  * default config") and so a future "actually shareable" base method has somewhere to land.
  *
  * '''2026-06-07 — OQ1 (plan 009, Phase 4) settled by probe.''' The other escape hatch — a single
  * `@BenchDefaults` meta-annotation carrying the six JMH annotations — was tested empirically (a
  * throwaway `@BenchDefaults class MetaProbeBench`). It does '''not''' work, and fails worse than
  * loudly: JMH's reflection generator does not resolve an annotation's own annotations through to
  * the annotated class (a Scala `StaticAnnotation` isn't even classfile-retained), so it silently
  * fell back to '''every default''' — all `@BenchmarkMode`s generated (Throughput at runtime, not
  * `AverageTime`), seconds not nanos, default scope/forks — without erroring on the missing
  * `@State`. Stripping the preamble to lean on the CLI would therefore produce silently wrong-mode
  * numbers, not a failure. Conclusion: the per-class preamble is structurally required and is kept
  * deliberately. The one duplication that '''is''' safe to remove — the run invocation itself —
  * lives in the `bench` / `benchQuick` sbt aliases (see `build.sbt`).
  */
trait JmhDefaults
