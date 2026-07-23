package dev.constructive.eo
package bench.fixture

/** Fixture services for the DI-integration benches (ZioDiBench / KyoDiBench): one focused service,
  * one sibling that must ride along untouched in the environment map.
  */
case class DiDb(url: String, pool: Int)
case class DiMetrics(prefix: String)
