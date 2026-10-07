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
source leftover: `Indexed.Grate[S, A, I]` specializes `X = Unit` and keeps
the source and focus types unchanged. For type-changing writes, use
`Indexed.PGrate[S, T, A, B, I]`, which also has `X = Unit`.
`Indexed.Aux[S, T, A, B, I, X0]` exposes a particular residual type.

## Environment-dependent settings

A reader over named environments is a small, useful product. Rather than
choose one environment, a Grate can rewrite the settings for both.
Each environment also records the Git commit it deploys. Cats'
`Representable` provides inverse `index` and
`tabulate` operations. Pass an explicit `Representable.Aux` witness so the
concrete index remains `Environment`, not an abstract `Representation`.

```scala mdoc:silent
import cats.Representable
import cats.instances.function.given
import dev.constructive.eo.optics.Indexed
import dev.constructive.eo.data.GlassF

sealed trait Environment:
  def gitSha: String

object Environment:
  case object Dev extends Environment:
    val gitSha: String = "0292103"
  case object Prod extends Environment:
    val gitSha: String = "8f14e45"

type ByEnvironment[A] = Environment => A

val environments: Representable.Aux[ByEnvironment, Environment] =
  summon[Representable.Aux[ByEnvironment, Environment]]

val settings: Indexed.Grate[ByEnvironment[Int], Int, Environment] =
  Indexed.representable[ByEnvironment, Int](environments)

val timeout: ByEnvironment[Int] =
  case Environment.Dev  => 5
  case Environment.Prod => 30

val path: ByEnvironment[String] = env =>
  if env == Environment.Prod then "/opt/app/" else s"/dev/app/${env.gitSha}"
require(path(Environment.Dev) == "/dev/app/0292103")
require(path(Environment.Prod) == "/opt/app/")
```

`at` reads one coordinate. Indexed `modify` supplies **both the index and
the old value**; `replace` broadcasts to the entire index space.

```scala mdoc:silent
val tuned = settings.modify((env, seconds) =>
  if env == Environment.Prod then seconds + 10 else seconds
)(timeout)
val maintenance = settings.replace(60)(timeout)
```

```scala mdoc
settings.at(Environment.Prod)(timeout)
(path(Environment.Dev), path(Environment.Prod))
(tuned(Environment.Dev), tuned(Environment.Prod))
(maintenance(Environment.Dev), maintenance(Environment.Prod))
```

The target need not have the same element type. A type-changing write can
render the whole product as user-facing settings:

```scala mdoc:silent
val renderSettings: Indexed.PGrate[
  ByEnvironment[Int], ByEnvironment[String], Int, String, Environment
] = Indexed.representableP[ByEnvironment, Int, String](environments)
val rendered = renderSettings.modify((env, seconds) =>
  s"${env}: ${seconds}s"
)(timeout)
require(rendered(Environment.Dev) == "Dev: 5s")
require(rendered(Environment.Prod) == "Prod: 30s")
val disabled = renderSettings.replace("disabled")(timeout)
```

```scala mdoc
(rendered(Environment.Dev), rendered(Environment.Prod))
(disabled(Environment.Dev), disabled(Environment.Prod))
```

With `X = Unit`, reconstruction does not need a previous source:

```scala mdoc:silent
val fresh = settings.from((context = (), values = (env: Environment) =>
  if env == Environment.Prod then 45 else 10
))
```

```scala mdoc
(fresh(Environment.Dev), fresh(Environment.Prod))
```

## Nested settings: the full grid

Now each environment contains settings for three priorities: `Low`, `Mid`,
and `High`. The environment and priority axes have different types and sizes.
Composition creates all **six** coordinates, not a shared-index diagonal.

```scala mdoc:silent
enum Priority:
  case Low, Mid, High

type ByPriority[A] = Priority => A
val priorities = summon[Representable.Aux[ByPriority, Priority]]
val prioritySettings = Indexed.representable[ByPriority, Int](priorities)

val rows = Indexed.representable[
  ByEnvironment, ByPriority[Int]
](environments)
val grid = rows.andThen(prioritySettings)

val timeouts: ByEnvironment[ByPriority[Int]] = env => priority =>
  timeout(env) + priority.ordinal * 2

def gridValues(g: ByEnvironment[ByPriority[Int]]): (Int, Int, Int, Int, Int, Int) =
  (
    g(Environment.Dev)(Priority.Low), g(Environment.Dev)(Priority.Mid),
    g(Environment.Dev)(Priority.High), g(Environment.Prod)(Priority.Low),
    g(Environment.Prod)(Priority.Mid), g(Environment.Prod)(Priority.High)
  )
```

The dedicated `andThen` takes the **product index** `(Environment, Priority)`.
An update can therefore target Dev's high-priority channel independently:

```scala mdoc:silent
val priorityDev = grid.modify((coordinate, seconds) =>
  if coordinate == (Environment.Dev, Priority.High) then seconds + 100 else seconds
)(timeouts)
val allMaintenance = grid.replace(60)(timeouts)
require(gridValues(priorityDev) == (5, 7, 109, 30, 32, 34))
require(gridValues(allMaintenance) == (60, 60, 60, 60, 60, 60))
```

```scala mdoc
grid.at((Environment.Dev, Priority.High))(timeouts)
gridValues(priorityDev)
gridValues(allMaintenance)
```

The equality test is this application's choice for environment/priority coordinates,
not a requirement of `Indexed`. `at` is a read, **not a one-coordinate
setter**: arbitrary index types need not supply coordinate equality.

Generic composition retains residual context
`(outer.X, I => inner.X)`. Even though both components here are Grates,
the composite's `X` is `(Unit, Environment => Unit)`, not literally `Unit`.
Unit-context normalization is still open; do not annotate this result
as `Indexed.Grate`. A third level similarly forms nested product indexes,
with reassociation needed to compare the two composition groupings.

## Device readings: a context-bearing Glass

A device has a reading for every named sensor and a label that must
survive updates. Its total `Sensor => A` tabulation supports both
`Device[Int]` measurements and type-changing writes to `Device[String]`:

```scala mdoc:silent
enum Sensor:
  case Humidity, Temperature, Preasure

case class Device[A](label: String, readings: Sensor => A)
```

Each `Sensor` coordinate has an `A`, so the focus needs neither `Option`
nor a missing-reading policy. The label is the residual `X`; the values
being rewritten are the sensor readings:

```scala mdoc:silent
def device[A, B]: Indexed.Aux[
  Device[A], Device[B], A, B, Sensor, String
] =
  Indexed.apply[Device[A], Device[B], A, B, Sensor, String](
    source => (source.label, source.readings)
  )((label, values) => Device[B](label, values))

val readings = device[Int, Int]
val boiler = Device[Int](
  "boiler",
  sensor => sensor match
    case Sensor.Humidity    => 40
    case Sensor.Temperature => 20
    case Sensor.Preasure    => 1010
)
```

Calibration sees the sensor name as well as its reading.
Broadcast replacement zeroes **all three** sensor readings.
Both preserve the label:

```scala mdoc:silent
val calibrated = readings.modify((sensor, value) =>
  value + (if sensor == Sensor.Temperature then 2 else 1)
)(boiler)
val reset = readings.replace(0)(boiler)
require(calibrated.label == boiler.label)
require(reset.label == boiler.label)
require(readings.at(Sensor.Humidity)(calibrated) == 41)
require(readings.at(Sensor.Temperature)(calibrated) == 22)
require(Sensor.values.forall(sensor => readings.at(sensor)(reset) == 0))
```

```scala mdoc
(
  calibrated.label,
  readings.at(Sensor.Humidity)(calibrated),
  readings.at(Sensor.Temperature)(calibrated),
  readings.at(Sensor.Preasure)(calibrated)
)
(reset.label, reset.readings(Sensor.Temperature), reset.readings(Sensor.Preasure))
```

`from` accepts **arbitrary** context/values bundles, not only ones obtained
from `to`. The carrier's named fields make the contract explicit:

```scala mdoc:silent
val supplied: GlassF[Sensor][String, Int] =
  (
    context = "outdoor",
    values = (sensor: Sensor) => sensor match
      case Sensor.Humidity    => 75
      case Sensor.Temperature => 12
      case Sensor.Preasure    => 1012
  )
val outdoor = readings.from(supplied)
val recovered = readings.to(outdoor)
require(recovered.context == supplied.context)
require(Sensor.values.forall(sensor => recovered.values(sensor) == supplied.values(sensor)))
val roundTrip = readings.from(readings.to(boiler))
require(roundTrip.label == boiler.label)
require(Sensor.values.forall(sensor => roundTrip.readings(sensor) == boiler.readings(sensor)))

val reports = device[Int, String].modify((sensor, value) =>
  s"$sensor: $value"
)(boiler)
require(reports.label == boiler.label)
require(reports.readings(Sensor.Temperature) == "Temperature: 20")
```

```scala mdoc
(
  outdoor.label,
  recovered.values(Sensor.Humidity),
  recovered.values(Sensor.Temperature),
  recovered.values(Sensor.Preasure)
)
(reports.label, reports.readings(Sensor.Temperature))
```

Unlike the Grate above, this Glass needs a `String` context to reconstruct.
It need not receive an old source, but a values-only tabulation is not enough.
User-written `Indexed.apply` constructors must satisfy both round trips:
`from(to(source))` recovers the source, and `to(from(bundle))` recovers
arbitrary context and values pointwise. Functions are compared at their
coordinates here, not with Scala function equality. Polymorphic constructors
also need the corresponding target-side decomposition to state the latter law.

## Rebuild a grid without losing device metadata

An installation contains a device in each environment. Compose its environment
axis with the sensor axis to retain **each device's label**, not just the
installation's label or one representative device.

```scala mdoc:silent
case class Installation[A](label: String, devices: ByEnvironment[Device[A]])

def installationOf[A, B]: Indexed.Aux[
  Installation[A], Installation[B], Device[A], Device[B], Environment, String
] =
  Indexed.apply[
    Installation[A], Installation[B], Device[A], Device[B], Environment, String
  ](source => (source.label, source.devices))((label, values) => Installation[B](label, values))

val installationRows = installationOf[Int, Int]
val installationGrid = installationRows.andThen(readings)

val installation = Installation[Int](
  "plant",
  env => env match
    case Environment.Dev =>
      Device[Int]("dev-boiler", sensor => sensor match
        case Sensor.Humidity    => 20
        case Sensor.Temperature => 10
        case Sensor.Preasure    => 1000
      )
    case Environment.Prod =>
      Device[Int](
        "prod-boiler",
        sensor => sensor match
          case Sensor.Humidity    => 50
          case Sensor.Temperature => 30
          case Sensor.Preasure    => 1010
      )
)

val original = installationGrid.to(installation)
val rebuilt = installationGrid.from((
  context = original.context,
  values = (coordinate: (Environment, Sensor)) =>
    (if coordinate._1 == Environment.Prod then 300 else 100) + coordinate._2.ordinal
))

require(rebuilt.label == "plant")
require(rebuilt.devices(Environment.Dev).label == "dev-boiler")
require(rebuilt.devices(Environment.Prod).label == "prod-boiler")
require(installationGrid.at((Environment.Dev, Sensor.Temperature))(rebuilt) == 101)
require(installationGrid.at((Environment.Prod, Sensor.Humidity))(rebuilt) == 300)
require(installationGrid.at((Environment.Dev, Sensor.Preasure))(rebuilt) == 102)

val blankInstallation = installationGrid.replace(0)(installation)
require(blankInstallation.label == installation.label)
require(blankInstallation.devices(Environment.Dev).label == installation.devices(Environment.Dev).label)
require(blankInstallation.devices(Environment.Prod).label == installation.devices(Environment.Prod).label)
require(List(Environment.Dev, Environment.Prod).forall(env =>
  Sensor.values.forall(sensor => installationGrid.at((env, sensor))(blankInstallation) == 0)
))
```

```scala mdoc
(
  original.context._1,
  original.context._2(Environment.Dev),
  original.context._2(Environment.Prod)
)
(rebuilt.label, rebuilt.devices(Environment.Dev).label, rebuilt.devices(Environment.Prod).label)
(
  installationGrid.at((Environment.Dev, Sensor.Humidity))(rebuilt),
  installationGrid.at((Environment.Dev, Sensor.Temperature))(rebuilt),
  installationGrid.at((Environment.Dev, Sensor.Preasure))(rebuilt),
  installationGrid.at((Environment.Prod, Sensor.Humidity))(rebuilt),
  installationGrid.at((Environment.Prod, Sensor.Temperature))(rebuilt),
  installationGrid.at((Environment.Prod, Sensor.Preasure))(rebuilt)
)
(
  blankInstallation.label,
  blankInstallation.devices(Environment.Dev).label,
  blankInstallation.devices(Environment.Prod).label,
  installationGrid.at((Environment.Dev, Sensor.Humidity))(blankInstallation),
  installationGrid.at((Environment.Prod, Sensor.Preasure))(blankInstallation)
)
```

Here `X` is `(String, Environment => String)`: the installation label and
a tabulation of device labels. `from` pairs each new sensor row with its
own residual label before reconstructing the outer structure. Indexed
`modify` and `replace` follow that same reconstruction rule.

## Choosing and composing the family

- **`Index` is not `Indexed`.** `Index` supplies an `Optional` for a
  potentially missing collection entry. `Indexed` describes a total
  tabulation over a fixed index; it neither inserts nor removes coordinates.
- **Constructors:** `representable(r)` builds a monomorphic `Grate` from
  a cats witness; `representableP(r)` builds a type-changing `PGrate`.
  `apply` accepts
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
