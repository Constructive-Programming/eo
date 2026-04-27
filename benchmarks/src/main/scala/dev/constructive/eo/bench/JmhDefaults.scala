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
  */
trait JmhDefaults
