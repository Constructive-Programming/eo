package dev.constructive.eo
package bench

import scala.collection.immutable.ArraySeq
import scala.compiletime.uninitialized

import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit

import cats.instances.arraySeq.given
import cats.instances.list.given

import dev.constructive.eo.data.MultiFocus.given

/** Nested-traversal PowerSeries bench — tree-of-trees shape.
  *
  * Fixture: `Company(departments: List[Department(employees: ArraySeq[Employee])])`. Every
  * employee's `active` flag is toggled. The optic chain is five hops deep with two separate
  * `Traversal.each` fan-out levels:
  *
  * {{{
  *   Lens[Company, List[Department]]
  *     .andThen(Traversal.each[List, Department])
  *     .andThen(Lens[Department, ArraySeq[Employee]])
  *     .andThen(Traversal.each[ArraySeq, Employee])
  *     .andThen(Lens[Employee, Boolean])
  * }}}
  *
  * This is the shape where a trie carrier would most clearly dominate the flat `(xo, PSVec)`
  * representation: the two traversal levels force the current design to flatten a logical tree into
  * parallel arrays and re-nest on the way out. A trie keeps the shape explicit and substitutes
  * leaves recursively.
  *
  * Total elements traversed = `departmentCount × size` (4 × {4, 32, 256}). Inner `size` matches the
  * flat-bench param so comparisons are direct.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class PowerSeriesNestedBench extends JmhDefaults:

  import PowerSeriesNestedBench.*

  @Param(Array("4", "32", "256"))
  var size: Int = uninitialized

  private val departmentCount = 4

  var company: Company = uninitialized

  private val companyAllActive =
    EoLens[Company, List[Department]](_.departments, (c, ds) => c.copy(departments = ds))
      .andThen(EoTraversal.each[List, Department])
      .andThen(
        EoLens[Department, ArraySeq[Employee]](
          _.employees,
          (d, es) => d.copy(employees = es),
        )
      )
      .andThen(EoTraversal.each[ArraySeq, Employee])
      .andThen(EoLens[Employee, Boolean](_.active, (e, b) => e.copy(active = b)))

  @Setup(Level.Iteration)
  def init(): Unit =
    company = Company(
      "ACME",
      List.tabulate(departmentCount)(d =>
        Department(
          s"d-$d",
          ArraySeq.tabulate(size)(i => Employee(i, s"e-$d-$i", i % 2 == 0)),
        )
      ),
    )

  @Benchmark def eoModify_nested: Company =
    companyAllActive.modify(!_)(company)

  @Benchmark def naive_nested: Company =
    company.copy(
      departments = company
        .departments
        .map(d => d.copy(employees = d.employees.map(e => e.copy(active = !e.active))))
    )

object PowerSeriesNestedBench:

  case class Employee(id: Int, name: String, active: Boolean)
  case class Department(name: String, employees: ArraySeq[Employee])
  case class Company(name: String, departments: List[Department])
