package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import org.openjdk.jmh.annotations.*

/** Shared JMH-config base that every bench class inherits.
  *
  * '''2026-04-26 dedup.''' Every bench class used to declare an identical pre-amble:
  *
  * {{{
  *   @State(Scope.Benchmark)
  *   @BenchmarkMode(Array(Mode.AverageTime))
  *   @OutputTimeUnit(TimeUnit.NANOSECONDS)
  *   @Fork(3)
  *   @Warmup(iterations = 3, time = 1)
  *   @Measurement(iterations = 5, time = 1)
  *   class XBench: …
  * }}}
  *
  * JMH inherits class-level annotations from a `trait` parent — see the
  * [[org.openjdk.jmh.annotations.Benchmark]] processor's "annotated parent class" handling. Bench
  * classes now declare `class XBench extends JmhDefaults: …` and pick up the standard preamble for
  * free.
  *
  * Per-bench classes that want to override one annotation (e.g. a longer warmup for a slow fixture)
  * can re-declare just the relevant `@Warmup` / `@Measurement` on the subclass; JMH's processor
  * takes the most-specific annotation.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
trait JmhDefaults
