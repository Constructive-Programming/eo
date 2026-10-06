# Indexed: Grates and Glasses

Use `Indexed` when a focus has a **fixed index space**: settings for every
environment, readings at every channel, or a grid formed by nesting those
shapes. Reads address a coordinate; writes supply a value for every coordinate.
This is different from a traversal over a variable number of elements.

The optic lives in `dev.constructive.eo.optics.Indexed`. Its carrier, defined
in `data/Glass.scala`, is the named tuple:

```scala
type GlassF[I] = [X, A] =>> (context: X, values: I => A)
```

`Optic.to` decomposes a source into residual context `X` and a tabulation
`I => A`; `Optic.from` reconstructs the target from context and new values.
An **indexed Glass** may retain source metadata in `X`. A **Grate** needs no
source leftover: `Indexed.Grate[S, T, A, B, I]` specializes `X = Unit`.
`Indexed.Aux[S, T, A, B, I, X0]` exposes a particular residual type.

## Environment-dependent settings

A Boolean reader is a small, useful product: `false` is staging and `true`
is production. Rather than choose one environment, a Grate can rewrite the
settings for both. Cats' `Representable` provides inverse `index` and
`tabulate` operations. Pass an explicit `Representable.Aux` witness so the
concrete index remains `Boolean`, not an abstract `Representation`.

```scala mdoc:silent
import cats.Representable
import cats.instances.function.given
import dev.constructive.eo.optics.Indexed
import dev.constructive.eo.data.GlassF

type ByEnvironment[A] = Boolean => A

val environments: Representable.Aux[ByEnvironment, Boolean] =
  summon[Representable.Aux[ByEnvironment, Boolean]]

val settings: Indexed.Grate[
  ByEnvironment[Int], ByEnvironment[Int], Int, Int, Boolean
] = Indexed.representable[ByEnvironment, Int, Int](environments)

val timeout: ByEnvironment[Int] = production => if production then 30 else 5
```

`at` reads one coordinate. Indexed `modify` supplies **both the index and
the old value**; `replace` broadcasts to the entire index space.

```scala mdoc:silent
val tuned = settings.modify((production, seconds) =>
  if production then seconds + 10 else seconds
)(timeout)
val maintenance = settings.replace(60)(timeout)
```

```scala mdoc
settings.at(true)(timeout)
(tuned(false), tuned(true))
(maintenance(false), maintenance(true))
```

The target need not have the same element type. A type-changing write can
render the whole product as user-facing settings:

```scala mdoc:silent
val renderSettings = Indexed.representable[ByEnvironment, Int, String](environments)
val rendered = renderSettings.modify((production, seconds) =>
  s"${if production then "production" else "staging"}: ${seconds}s"
)(timeout)
require(rendered(false) == "staging: 5s")
require(rendered(true) == "production: 30s")
val disabled = renderSettings.replace("disabled")(timeout)
```

```scala mdoc
(rendered(false), rendered(true))
(disabled(false), disabled(true))
```

With `X = Unit`, reconstruction does not need a previous source:

```scala mdoc:silent
val fresh = settings.from((context = (), values = (production: Boolean) =>
  if production then 45 else 10
))
```

```scala mdoc
(fresh(false), fresh(true))
```

## Nested settings: the full grid

Now each environment contains two channel settings, ordinary (`false`)
and priority (`true`). Nesting two Boolean readers creates four coordinates,
not just the two where the Boolean indexes agree.

```scala mdoc:silent
val rows = Indexed.representable[
  ByEnvironment, ByEnvironment[Int], ByEnvironment[Int]
](environments)
val grid = rows.andThen(settings)

val timeouts: ByEnvironment[ByEnvironment[Int]] = production => priority =>
  (if production then 30 else 5) + (if priority then 2 else 0)

def gridValues(g: ByEnvironment[ByEnvironment[Int]]): (Int, Int, Int, Int) =
  (g(false)(false), g(false)(true), g(true)(false), g(true)(true))
```

The dedicated `andThen` takes the **product index** `(Boolean, Boolean)`.
An off-diagonal update can therefore reach staging's priority channel:

```scala mdoc:silent
val priorityStaging = grid.modify((coordinate, seconds) =>
  if coordinate == (false, true) then seconds + 100 else seconds
)(timeouts)
val allMaintenance = grid.replace(60)(timeouts)
require(gridValues(priorityStaging) == (5, 107, 30, 32))
require(gridValues(allMaintenance) == (60, 60, 60, 60))
```

```scala mdoc
grid.at((false, true))(timeouts)
gridValues(priorityStaging)
gridValues(allMaintenance)
```

The equality test is this application's choice for Boolean coordinates,
not a requirement of `Indexed`. `at` is a read, **not a one-coordinate
setter**: arbitrary index types need not supply coordinate equality.

Generic composition retains residual context
`(outer.X, I => inner.X)`. Even though both components here are Grates,
the composite's `X` is `(Unit, Boolean => Unit)`, not literally `Unit`.
Unit-context normalization is still open; do not annotate this result
as `Indexed.Grate`. A third level similarly forms nested product indexes,
with reassociation needed to compare the two composition groupings.

## Labelled readings: a context-bearing Glass

A labelled pair of readings has the same two-coordinate focus, but now
reconstruction must also retain a label. A Glass separates that metadata
from the values being edited:

```scala mdoc:silent
case class Labelled[A](label: String, readings: Boolean => A)

def labelled[A, B]: Indexed.Aux[
  Labelled[A], Labelled[B], A, B, Boolean, String
] =
  Indexed.apply[Labelled[A], Labelled[B], A, B, Boolean, String](
    source => (source.label, source.readings)
  )((label, values) => Labelled[B](label, values))

val readings = labelled[Int, Int]
val sensor = Labelled[Int]("boiler", high => if high then 80 else 20)
```

Both indexed modification and broadcast replacement preserve the label:

```scala mdoc:silent
val calibrated = readings.modify((high, value) =>
  value + (if high then 2 else 1)
)(sensor)
val reset = readings.replace(0)(sensor)
require(calibrated.label == sensor.label)
require(reset.label == sensor.label)
```

```scala mdoc
(calibrated.label, calibrated.readings(false), calibrated.readings(true))
(reset.label, reset.readings(false), reset.readings(true))
```

`from` accepts **arbitrary** context/values bundles, not only ones obtained
from `to`. The carrier's named fields make the contract explicit:

```scala mdoc:silent
val supplied: GlassF[Boolean][String, Int] =
  (context = "outdoor", values = (high: Boolean) => if high then 12 else 3)
val outdoor = readings.from(supplied)
val recovered = readings.to(outdoor)
require(recovered.context == supplied.context)
require(recovered.values(false) == supplied.values(false))
require(recovered.values(true) == supplied.values(true))

val roundTrip = readings.from(readings.to(sensor))
require(roundTrip.label == sensor.label)
require(roundTrip.readings(false) == sensor.readings(false))
require(roundTrip.readings(true) == sensor.readings(true))
```

```scala mdoc
(outdoor.label, outdoor.readings(false), outdoor.readings(true))
(recovered.context, recovered.values(false), recovered.values(true))
```

Unlike the Grate above, this Glass needs a `String` context to reconstruct.
It need not receive an old source, but a values-only tabulation is not enough.
User-written `Indexed.apply` constructors must satisfy both round trips:
`from(to(source))` recovers the source, and `to(from(bundle))` recovers
arbitrary context and values pointwise. Functions are compared at their
coordinates here, not with Scala function equality. Polymorphic constructors
also need the corresponding target-side decomposition to state the latter law.

## Rebuild a grid without losing row metadata

Nest `Labelled` to model a labelled installation with independently
labelled sensor rows. Composition must retain **each row's label**, not just
the installation's label or one representative row.

```scala mdoc:silent
val installationRows = labelled[Labelled[Int], Labelled[Int]]
val installationGrid = installationRows.andThen(readings)

val installation = Labelled[Labelled[Int]](
  "plant",
  row => if row then
    Labelled[Int]("return", high => if high then 40 else 30)
  else
    Labelled[Int]("supply", high => if high then 20 else 10)
)

val original = installationGrid.to(installation)
val rebuilt = installationGrid.from((
  context = original.context,
  values = (coordinate: (Boolean, Boolean)) =>
    (if coordinate._1 then 300 else 100) + (if coordinate._2 then 2 else 1)
))

require(rebuilt.label == "plant")
require(rebuilt.readings(false).label == "supply")
require(rebuilt.readings(true).label == "return")
require(installationGrid.at((false, true))(rebuilt) == 102)
require(installationGrid.at((true, false))(rebuilt) == 301)

val blankInstallation = installationGrid.replace(0)(installation)
require(blankInstallation.label == installation.label)
require(blankInstallation.readings(false).label == installation.readings(false).label)
require(blankInstallation.readings(true).label == installation.readings(true).label)
```

```scala mdoc
(original.context._1, original.context._2(false), original.context._2(true))
(rebuilt.label, rebuilt.readings(false).label, rebuilt.readings(true).label)
(
  installationGrid.at((false, false))(rebuilt),
  installationGrid.at((false, true))(rebuilt),
  installationGrid.at((true, false))(rebuilt),
  installationGrid.at((true, true))(rebuilt)
)
(
  blankInstallation.label,
  blankInstallation.readings(false).label,
  blankInstallation.readings(true).label,
  installationGrid.at((false, true))(blankInstallation),
  installationGrid.at((true, false))(blankInstallation)
)
```

Here `X` is `(String, Boolean => String)`: the installation label and a
tabulation of row labels. `from` pairs each new row of values with its own
residual label before reconstructing the outer structure. Indexed `modify`
and `replace` follow that same reconstruction rule.

## Choosing and composing the family

- **`Index` is not `Indexed`.** `Index` supplies an `Optional` for a
  potentially missing collection entry. `Indexed` describes a total
  tabulation over a fixed index; it neither inserts nor removes coordinates.
- **Constructors:** `representable(r)` uses a cats witness; `apply` accepts
  a user-written algebra; `iso` has a single `Unit` coordinate; `unit`
  is the type-changing identity at that single coordinate. There is no
  new top-level `Grate` companion.
- **Nested grids:** use the dedicated product-index `andThen` shown above.
  No generic classical-family `Composer` bridges or
  `AssociativeFunctor[GlassF[I]]` are installed. The classical composition
  matrix is not a promise of Glass seams.
- **Write-only inner optics:** the existing writable-outer `Optic`
  extension supports `glass.andThen(modify)` with a `Modify` inner.
  This is not a Glass `Composer` bridge, and does not establish the reverse
  direction.
- **Combining sources:** `zip` and `collect` are not installed. Choosing
  residual context when combining Glass sources needs a separate design.

For variable-shape containers and aggregations, see [MultiFocus](multifocus.md).
For the surrounding optic families, see the [Optics reference](optics.md#indexed);
for task-oriented examples, see the [Cookbook](cookbook.md).
