package dev.constructive.eo.avro

import scala.annotation.tailrec

import java.lang.ref.WeakReference
import org.apache.avro.Schema
import org.specs2.mutable.Specification

/** The cache contract behind the nominal rung's normalised-name index (issue #103).
  *
  * The index itself only makes the rung linear in the field count; it is this cache that makes a
  * k-hop chain into one record pay for it ONCE instead of k times, and a `def`-shaped optic — which
  * re-resolves on every operation — pay for it once instead of once per operation.
  *
  * Three properties, all of them consequences of the two design choices (identity keys, weak keys),
  * and all pinned here because a future "simplification" to a plain `HashMap[Schema, _]` would
  * silently reintroduce both a deep-structural compare per lookup and an unbounded leak.
  */
class NominalNameIndexCacheSpec extends Specification:

  import NominalCostFixtures.*

  "the index is built once per schema and handed back by reference" >> {
    val sch = schema(24, snake = true)
    AvroWalk.normalisedNameIndex(sch) must beTheSameAs(AvroWalk.normalisedNameIndex(sch))
  }

  "repeated resolution against one schema never rebuilds the index" >> {
    // The k-hop claim: drilling k times into the same record consults one index instance.
    val sch = schema(24, snake = true)
    val names = caseNames(24)
    val first = AvroWalk.normalisedNameIndex(sch)
    (1 to 64).foreach(_ => sink.addAndGet(AvroWalk.totalNominalIndex(sch, names, 0).toLong))
    AvroWalk.normalisedNameIndex(sch) must beTheSameAs(first)
  }

  "the cache keys on REFERENCE identity, not on structural equality" >> {
    // `Schema.equals` is a deep, recursive compare and `Schema` is mutable (`addProp` resets its
    // cached hash), so a structural key would pay a deep compare per lookup and could lose an entry
    // under annotation. Two structurally EQUAL schemas therefore get one entry each.
    val a = named("TwinRec", 8, snake = true)
    val b = named("TwinRec", 8, snake = true)
    (a === b)
      .and(a must not(beTheSameAs(b)))
      .and(AvroWalk.normalisedNameIndex(a) must not(beTheSameAs(AvroWalk.normalisedNameIndex(b))))
  }

  "the cache does not keep a schema alive — the keys are weak" >> {
    // The leak story, as a fact rather than a claim: resolve against a schema, drop every strong
    // reference to it, and it must still become collectable. A strong-keyed side table would pin
    // every schema ever resolved through, for the life of the process.
    def resolveAndForget(): WeakReference[Schema] =
      val sch = named("Ephemeral", 8, snake = true)
      sink.addAndGet(AvroWalk.totalNominalIndex(sch, caseNames(8), 0).toLong)
      new WeakReference(sch)

    val ref = resolveAndForget()
    @tailrec def awaitCollection(tries: Int): Boolean =
      if ref.get() == null then true
      else if tries <= 0 then false
      else
        System.gc()
        Thread.sleep(20L)
        awaitCollection(tries - 1)
    awaitCollection(50) must beTrue
  }

  "two schema fields normalising alike are recorded as ambiguous, not as first-wins" >> {
    // The collision signal the linear scan produced on its SECOND hit, moved to index-build time.
    val fields = new java.util.ArrayList[Schema.Field]()
    fields.add(new Schema.Field("user_id", Schema.create(Schema.Type.STRING), null, null))
    fields.add(new Schema.Field("userId", Schema.create(Schema.Type.STRING), null, null))
    val sch = Schema.createRecord("Collide", null, "eo.perf", false, fields)
    AvroWalk.normalisedNameIndex(sch).get("userid").intValue === -1
  }

end NominalNameIndexCacheSpec
