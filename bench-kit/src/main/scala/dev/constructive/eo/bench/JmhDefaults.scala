package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit
import org.openjdk.jmh.annotations.*

/** The project's shared JMH configuration: every benchmark class extends it.
  *
  * JMH reads `@State`, `@BenchmarkMode`, `@OutputTimeUnit`, `@Fork`, `@Warmup` and `@Measurement`
  * from the benchmark class and its SUPERCLASSES (JMH's own `JMHSample_24_Inheritance`), but not
  * from interfaces. So this must stay an `abstract class`: as a Scala `trait` it compiles to an
  * interface, JMH sees none of the annotations, and every bench fails with "declared within a class
  * not having @State annotation". A subclass overrides a setting by carrying its own annotation.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
abstract class JmhDefaults
