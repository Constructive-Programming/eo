package dev.constructive.eo.jsoniter

import scala.language.implicitConversions

import cats.Functor
import com.github.plokhotnyuk.jsoniter_scala.core.{writeToArray, JsonValueCodec, WriterConfig}
import com.github.plokhotnyuk.jsoniter_scala.macros.JsonCodecMaker
import dev.constructive.eo.data.Affine
import dev.constructive.eo.laws.discipline.{OptionalTests, TraversalTests}
import dev.constructive.eo.laws.{OptionalLaws, TraversalLaws}
import dev.constructive.eo.optics.Optic
import dev.constructive.eo.optics.Optic.*
import java.nio.ByteBuffer
import org.scalacheck.{Arbitrary, Cogen, Gen, Prop}
import org.specs2.ScalaCheck
import org.specs2.mutable.Specification
import org.typelevel.discipline.specs2.mutable.Discipline

// Fixture ADTs at top level (mirrors the eo-generics convention for macro-facing test types).
final case class SliceInner(flag: Boolean, score: Int)
final case class SliceRec(id: Long, name: String, tags: List[Long], inner: SliceInner)
final case class SliceBasket(items: List[SliceInner])

/** The in-place optics, `JsoniterPrism.inPlace` / `JsoniterTraversal.inPlace`, read a JSON document
  * in place from a range of a larger array. The contract is DIFFERENTIAL: a document embedded
  * between junk bytes on both sides reads and writes exactly like the same bytes copied into an
  * array of their own, read by the whole-array optic — for every path shape (root, field, nested,
  * index, wildcard, raw, missing, mistyped). The junk is adversarial: JSON fragments that would
  * extend the document if a scan read past the slice (`42` followed by `999`, `{…}` followed by
  * `,"id":7}`).
  *
  * Plus: the optic laws on embedded slices (`OptionalTests` / `TraversalTests`, slices compared by
  * content); the write rule (a splice returns a fresh array holding only the slice's bytes and
  * never writes the source; a miss hands back the input slice itself); the scanner's ranged entry
  * points (absolute spans, shifted by the offset); and `JsonSlice`'s fail-fast bounds check.
  *
  * And the in-place optics' own surface: a path built in either order (drill, then `.inPlace`; or
  * `.inPlace`, then drill) is the same optic; `.raw` in place focuses a VIEW of the source array,
  * reads and writes as the copying `.raw`, composes with ordinary `andThen`, and is lawful; and the
  * `ByteBuffer` / `jsonSlice` constructors.
  */
class JsonSliceSpec extends Specification with ScalaCheck with Discipline:

  given JsonValueCodec[Long] = JsonCodecMaker.make
  given JsonValueCodec[Int] = JsonCodecMaker.make
  given JsonValueCodec[String] = JsonCodecMaker.make
  given JsonValueCodec[List[Long]] = JsonCodecMaker.make
  given JsonValueCodec[SliceInner] = JsonCodecMaker.make
  given JsonValueCodec[SliceRec] = JsonCodecMaker.make
  given innersCodec: JsonValueCodec[List[SliceInner]] = JsonCodecMaker.make
  given JsonValueCodec[SliceBasket] = JsonCodecMaker.make

  // ----- generators -----------------------------------------------------

  private val genRec: Gen[SliceRec] =
    for
      id <- Gen.choose(-1000L, 1000L)
      name <- Gen.oneOf("", "a", "Ω", "quote\"d", "back\\slash", "line\nbreak")
      tags <- Gen.choose(0, 4).flatMap(Gen.listOfN(_, Gen.choose(-50L, 50L)))
      inner <- genInner
    yield SliceRec(id, name, tags, inner)

  private lazy val genInner: Gen[SliceInner] =
    for
      flag <- Gen.oneOf(true, false)
      score <- Gen.choose(-9, 9)
    yield SliceInner(flag, score)

  /** Bytes around a slice. JSON fragments that would continue — or complete — the document if a
    * scan crossed the slice's edge, a stray quote or backslash, and arbitrary bytes.
    */
  private val genJunk: Gen[Array[Byte]] = Gen.oneOf(
    Gen.const(Array.emptyByteArray),
    Gen
      .oneOf(
        // continue a complete document: a number, an object, an array
        "9",
        "999",
        ".5e3",
        "}",
        "]",
        ",\"id\":7}",
        "{\"id\":1,",
        "[1,2",
        "null",
        // complete a truncated one: `{` + `"id":7}`, `[1,` + `1]`, `nul` + `l`, `"open` + `"`
        "\"id\":7}",
        "1]",
        "l",
        "\"",
        "\\",
        "  ",
      )
      .map(_.getBytes("UTF-8")),
    Gen.listOf(Arbitrary.arbitrary[Byte]).map(_.toArray),
  )

  /** A document in the middle of junk, with the document's own bytes. */
  private def embed(doc: Array[Byte]): Gen[(JsonSlice, Array[Byte])] =
    for
      prefix <- genJunk
      suffix <- genJunk
    yield (JsonSlice(prefix ++ doc ++ suffix, prefix.length, doc.length), doc)

  private val compact: Gen[Array[Byte]] = genRec.map(writeToArray(_))

  private val anyLayout: Gen[Array[Byte]] = Gen.oneOf(
    compact,
    genRec.map(writeToArray(_, WriterConfig.withIndentionStep(2))),
    genRec.map(r => s"  ${new String(writeToArray(r), "UTF-8")}\n".getBytes("UTF-8")),
  )

  /** Documents of every shape a path can meet at its root: objects, arrays, bare scalars. */
  private val genDoc: Gen[Array[Byte]] = Gen.oneOf(
    anyLayout,
    Gen.listOf(Gen.choose(-99L, 99L)).map(writeToArray(_)),
    Gen.choose(-99999L, 99999L).map(writeToArray(_)),
    Gen.oneOf("", " ", "{", "[1,", "nul", "\"open").map(_.getBytes("UTF-8")),
  )

  // ----- the optics under test ------------------------------------------

  private val root = JsoniterPrism[SliceRec]
  private val id = JsoniterPrism[SliceRec].field(_.id)
  private val name = JsoniterPrism[SliceRec].field(_.name)
  private val score = JsoniterPrism[SliceRec].field(_.inner).field(_.score)
  private val firstTag: JsoniterPrism[Long] = JsoniterPrism[SliceRec].field(_.tags).at(0)
  private val inner = JsoniterPrism[SliceRec].field(_.inner).raw
  private val bareLong = JsoniterPrism[Long]
  private val r0 = SliceRec(0, "z", Nil, SliceInner(false, 0))
  private val headOfList = JsoniterPathFixtures.prism[Long]("$[0]")
  private val missing = JsoniterPathFixtures.prism[Long]("$.nope")
  private val mistyped = JsoniterPathFixtures.prism[Long]("$.name")
  private val tags: JsoniterTraversal[Long] = JsoniterPrism[SliceRec].field(_.tags).each
  private val listItems = JsoniterPathFixtures.traversal[Long]("$[*]")

  // ----- differential: in place == copied out -----------------------------

  /** The in-place face reads, modifies and replaces like the whole-array prism on the copied bytes,
    * and leaves the source array alone.
    */
  private def samePrism[A](p: JsoniterPrism[A], f: A => A, a: A)(
      slice: JsonSlice,
      doc: Array[Byte],
  ): Prop =
    val before = slice.array.clone
    val s = p.inPlace
    def same(out: JsonSlice, expected: Array[Byte]): Boolean = out == JsonSlice(expected)
    val read = s.getOption(slice).map(normal) == p.getOption(doc).map(normal)
    val modified = same(s.modify(f)(slice), p.modify(f)(doc))
    val replaced = same(s.replace(a)(slice), p.replace(a)(doc))
    val untouched = slice.array.sameElements(before)
    (read :| "getOption") && (modified :| "modify") && (replaced :| "replace") &&
    (untouched :| "source array untouched")

  /** Arrays compare by reference; read their contents instead. */
  private def normal(a: Any): Any = a match
    case bytes: Array[Byte] => bytes.toSeq
    case other              => other

  private def sameTraversal(t: JsoniterTraversal[Long])(slice: JsonSlice, doc: Array[Byte]): Prop =
    val before = slice.array.clone
    val s = t.inPlace
    val read = s.foldMap(List(_))(slice) == t.foldMap(List(_))(doc)
    val modified = s.modify(_ * 3 - 1)(slice) == JsonSlice(t.modify(_ * 3 - 1)(doc))
    val replaced = s.replace(7L)(slice) == JsonSlice(t.replace(7L)(doc))
    (read :| "foldMap") && (modified :| "modify") && (replaced :| "replace") &&
    (slice.array.sameElements(before) :| "source array untouched")

  private def embedded(prop: (JsonSlice, Array[Byte]) => Prop): Prop =
    Prop.forAll(genDoc.flatMap(embed))((slice, doc) => prop(slice, doc))

  "in place == copied out: the root prism" >>
    embedded(
      samePrism(root, r => r.copy(id = r.id + 1), SliceRec(0, "z", Nil, SliceInner(false, 0)))
    )
  "in place == copied out: a field" >> embedded(samePrism(id, _ * 2, 5L))
  "in place == copied out: a string field" >> embedded(samePrism(name, _ + "!", "q\"x"))
  "in place == copied out: a nested field" >> embedded(samePrism(score, _ - 1, 3))
  "in place == copied out: an array index" >> embedded(samePrism(firstTag, _ + 10, -4L))
  "in place == copied out: a raw slice" >> embedded(
    samePrism(inner, identity, "{\"flag\":true,\"score\":1}".getBytes)
  )
  "in place == copied out: a bare scalar document" >> embedded(samePrism(bareLong, _ + 1, 123456L))
  "in place == copied out: an index at the root" >> embedded(samePrism(headOfList, _ - 1, 0L))
  "in place == copied out: a missing path" >> embedded(samePrism(missing, _ + 1, 1L))
  "in place == copied out: a mistyped path" >> embedded(samePrism(mistyped, _ + 1, 1L))
  "in place == copied out: a traversal" >> embedded(sameTraversal(tags))
  "in place == copied out: a traversal at the root" >> embedded(sameTraversal(listItems))

  // ----- the write rule --------------------------------------------------

  "a write that splices returns a fresh array of the slice's bytes; a miss returns the slice" >> {
    val doc = writeToArray(SliceRec(1, "n", List(1, 2), SliceInner(true, 2)))
    val buffer = "xx".getBytes ++ doc ++ "yy".getBytes
    val slice = JsonSlice(buffer, 2, doc.length)
    val hit = id.inPlace.replace(42L)(slice)
    val miss = missing.inPlace.replace(42L)(slice)
    val noFoci = listItems.inPlace.modify(_ + 1)(slice) // a record has no `$[*]`
    val written = tags.inPlace.modify(_ + 1)(slice)
    (hit.offset === 0)
      .and(hit.length === hit.array.length)
      .and(
        hit.toString === """JsonSlice({"id":42,"name":"n","tags":[1,2],"inner":{"flag":true,"score":2}})"""
      )
      .and(written.offset === 0)
      .and(written.array.length === written.length)
      .and(id.inPlace.getOption(written) === Some(1L))
      .and(tags.inPlace.foldMap(List(_))(written) === List(2L, 3L))
      .and(miss must beTheSameAs(slice))
      .and(noFoci must beTheSameAs(slice))
      .and(new String(buffer, "UTF-8") === "xx" + new String(doc, "UTF-8") + "yy")
  }

  "reverseGet on the in-place face encodes a slice of its own" >> {
    val r = SliceRec(3, "r", Nil, SliceInner(false, 1))
    val out = root.inPlace.reverseGet(r)
    (out === JsonSlice(writeToArray(r))).and(out.offset === 0).and(out.length === out.array.length)
  }

  // ----- laws, on embedded slices -----------------------------------------

  private given Arbitrary[JsonSlice] = Arbitrary(compact.flatMap(embed).map(_._1))

  checkAll(
    "JsoniterPrism[SliceRec].field(_.id).inPlace",
    new OptionalTests[JsonSlice, Long]:
      val laws = new OptionalLaws[JsonSlice, Long]:
        val optional = id.inPlace
    .optional,
  )

  checkAll(
    "JsoniterPrism[SliceRec].field(_.inner).field(_.score).inPlace",
    new OptionalTests[JsonSlice, Int]:
      val laws = new OptionalLaws[JsonSlice, Int]:
        val optional = score.inPlace
    .optional,
  )

  /** A slice is one document, whatever its foci: the constant functor the traversal laws need. */
  private type Doc[X] = JsonSlice

  private given Functor[Doc] with
    def map[A, B](fa: JsonSlice)(f: A => B): JsonSlice = fa

  checkAll(
    "JsoniterPrism[SliceRec].field(_.tags).each.inPlace",
    new TraversalTests[Doc, Long]:
      val laws = new TraversalLaws[Doc, Long]:
        val traversal = tags.inPlace
    .traversal,
  )

  // ----- drill, then `.inPlace` == `.inPlace`, then drill --------------------

  /** Documents for the order property: every shape [[genDoc]] has, plus baskets (`.each.field`). */
  private val orderDocs: Gen[Array[Byte]] = Gen.oneOf(
    genDoc,
    Gen.choose(0, 4).flatMap(Gen.listOfN(_, genInner)).map(is => writeToArray(SliceBasket(is))),
  )

  private def ordered(prop: JsonSlice => Prop): Prop =
    Prop.forAll(orderDocs.flatMap(embed))((slice, _) => prop(slice))

  /** Two in-place prisms built in different orders: the same path, read and written alike. */
  private def sameWayRound[A](
      drilledFirst: JsoniterInPlacePrism[A],
      inPlaceFirst: JsoniterInPlacePrism[A],
      f: A => A,
      a: A,
  )(slice: JsonSlice): Prop =
    (drilledFirst.steps == inPlaceFirst.steps) :| "same path" &&
      (drilledFirst.getOption(slice) == inPlaceFirst.getOption(slice)) :| "getOption" &&
      (drilledFirst.modify(f)(slice) == inPlaceFirst.modify(f)(slice)) :| "modify" &&
      (drilledFirst.replace(a)(slice) == inPlaceFirst.replace(a)(slice)) :| "replace"

  private def sameWayRoundT[A](
      drilledFirst: JsoniterInPlaceTraversal[A],
      inPlaceFirst: JsoniterInPlaceTraversal[A],
      f: A => A,
  )(slice: JsonSlice): Prop =
    (drilledFirst.steps == inPlaceFirst.steps) :| "same path" &&
      (drilledFirst.foldMap(List(_))(slice) == inPlaceFirst.foldMap(List(_))(slice)) :| "foldMap" &&
      (drilledFirst.modify(f)(slice) == inPlaceFirst.modify(f)(slice)) :| "modify"

  "either order: a field" >> ordered(
    sameWayRound(id.inPlace, JsoniterPrism.inPlace[SliceRec].field(_.id), _ * 2, 5L)
  )
  "either order: a nested field, with the Dynamic sugar on both sides" >> {
    val drilledFirst: JsoniterInPlacePrism[Int] = JsoniterPrism[SliceRec].inner.score.inPlace
    val inPlaceFirst: JsoniterInPlacePrism[Int] = JsoniterPrism.inPlace[SliceRec].inner.score
    val halfway = JsoniterPrism[SliceRec].field(_.inner).inPlace.field(_.score)
    ordered(sameWayRound(drilledFirst, inPlaceFirst, _ - 1, 3)) &&
    ordered(sameWayRound(drilledFirst, halfway, _ + 1, -3))
  }
  "either order: an array index" >> ordered(
    sameWayRound(firstTag.inPlace, JsoniterPrism.inPlace[SliceRec].field(_.tags).at(0), _ + 1, 9L)
  )
  "either order: the root" >> ordered(
    sameWayRound(root.inPlace, JsoniterPrism.inPlace[SliceRec], r => r.copy(id = 0), r0)
  )
  "either order: a raw span" >> ordered(
    sameWayRound(
      JsoniterPrism[SliceRec].field(_.inner).inPlace.raw,
      JsoniterPrism.inPlace[SliceRec].field(_.inner).raw,
      identity,
      JsonSlice("{\"flag\":true,\"score\":1}".getBytes),
    )
  )
  "either order: a traversal" >> {
    val inPlaceFirst: JsoniterInPlaceTraversal[Long] =
      JsoniterPrism.inPlace[SliceRec].field(_.tags).each
    val halfway: JsoniterInPlaceTraversal[Long] = JsoniterPrism[SliceRec].tags.inPlace.each
    ordered(sameWayRoundT(tags.inPlace, inPlaceFirst, _ * 3)) &&
    ordered(sameWayRoundT(tags.inPlace, halfway, _ - 2))
  }
  "either order: a field past `.each`" >> {
    val drilledFirst = JsoniterPrism[SliceBasket].field(_.items).each.field(_.score).inPlace
    val inPlaceFirst: JsoniterInPlaceTraversal[Int] =
      JsoniterPrism.inPlace[SliceBasket].items.each.field(_.score)
    val halfway: JsoniterInPlaceTraversal[Int] =
      JsoniterPrism[SliceBasket].items.each.inPlace.score
    ordered(sameWayRoundT(drilledFirst, inPlaceFirst, _ + 4)) &&
    ordered(sameWayRoundT(drilledFirst, halfway, _ * -1))
  }

  // ----- `.raw` in place: a view of the source array -------------------------

  private val innerIn: JsoniterInPlacePrism[JsonSlice] =
    JsoniterPrism.inPlace[SliceRec].field(_.inner).raw

  "`.raw` in place: a view of the source array, reading and writing as the copying `.raw`" >>
    Prop.forAll(genDoc.flatMap(embed), genDoc.flatMap(embed)) {
      case ((slice, doc), (newFocus, _)) =>
        val before = slice.array.clone
        val view = innerIn.getOption(slice)
        val read = view.map(_.toArray.toSeq) == inner.getOption(doc).map(_.toSeq)
        val shared = view.forall(_.array eq slice.array)
        // Written verbatim, as the copying `.raw` writes: whatever the new focus's bytes are.
        val replaced = innerIn.replace(newFocus)(slice) == JsonSlice(
          inner.replace(newFocus.toArray)(doc)
        )
        val modified = innerIn.modify(identity)(slice) == JsonSlice(inner.modify(identity)(doc))
        (read :| "getOption") && (shared :| "a view, not a copy") && (replaced :| "replace") &&
        (modified :| "modify") && (slice.array.sameElements(before) :| "source array untouched")
    }

  "`.raw` in place checks a span is one JSON value exactly as the copying `.raw` does" >>
    // Spans the scanner finds (it checks structure, not every token) and spans it doesn't.
    forall(List("[tru]", "[\"\\q\",1.]", "1.", "-", "{\"a\":1,}", "[1 2]", "\"\\u12\"", "{}")) {
      v =>
        val doc = s"{\"id\":1,\"inner\":$v}".getBytes
        innerIn.getOption(JsonSlice(doc)).map(_.toArray.toSeq) === inner.getOption(doc).map(_.toSeq)
    }

  "`.raw` in place: the root prism round-trips a whole document as a fresh copy" >> {
    val doc = "{\"id\":1}".getBytes
    val rootRaw = JsoniterPrism.inPlace[SliceRec].raw
    val focus = rootRaw.getOption(JsonSlice(doc))
    val built = rootRaw.reverseGet(JsonSlice(doc))
    (focus must beSome(JsonSlice(doc)))
      .and(focus.map(_.array eq doc) must beSome(true))
      .and(built === JsonSlice(doc))
      .and((built.array eq doc) must beFalse)
  }

  private val scoreOfInner: JsoniterInPlacePrism[Int] = JsoniterPrism.inPlace[SliceInner].score

  /** Ordinary `andThen`: the raw span's prism, then a prism on the span's own document. */
  private val composed: Optic[JsonSlice, JsonSlice, Int, Int, Affine] =
    innerIn.andThen(scoreOfInner)

  "`.raw` in place, `andThen` a prism on the focus: the same optic as drilling straight through" >>
    embedded { (slice, _) =>
      val before = slice.array.clone
      val direct = score.inPlace
      (composed.getOption(slice) == direct.getOption(slice)) :| "getOption" &&
      (composed.modify(_ * 2 + 1)(slice) == direct.modify(_ * 2 + 1)(slice)) :| "modify" &&
      (composed.replace(-7)(slice) == direct.replace(-7)(slice)) :| "replace" &&
      slice.array.sameElements(before) :| "source array untouched"
    }

  /** A slice compared, and so co-generated, by its content. */
  private given Cogen[JsonSlice] = Cogen[List[Byte]].contramap(_.toArray.toList)

  // The raw focus is any JSON value: here the arbitrary slices' own documents, embedded in junk.
  checkAll(
    "JsoniterPrism.inPlace[SliceRec].field(_.inner).raw",
    new OptionalTests[JsonSlice, JsonSlice]:
      val laws = new OptionalLaws[JsonSlice, JsonSlice]:
        val optional = innerIn
    .optional,
  )

  checkAll(
    "JsoniterPrism.inPlace[SliceRec].field(_.inner).raw.andThen(JsoniterPrism.inPlace[SliceInner].score)",
    new OptionalTests[JsonSlice, Int]:
      val laws = new OptionalLaws[JsonSlice, Int]:
        val optional = composed
    .optional,
  )

  // ----- the ByteBuffer and `jsonSlice` constructors -------------------------

  private val docBytes = "{\"id\":1}".getBytes

  "JsonSlice(ByteBuffer): a heap buffer is a view, at arrayOffset + position, and keeps its position" >> {
    val backing = "..xx{\"id\":1}yy".getBytes
    // `slice()` gives a buffer whose array starts 2 bytes into `backing`: arrayOffset 2.
    val buf = ByteBuffer.wrap(backing, 2, 12).slice()
    buf.position(2).limit(10)
    val slice = JsonSlice(buf)
    (buf.arrayOffset === 2)
      .and(slice === JsonSlice(docBytes))
      .and(slice.array must beTheSameAs(backing))
      .and(slice.offset === 4)
      .and(slice.length === 8)
      .and(buf.position === 2)
      .and(buf.limit === 10)
      .and(id.inPlace.getOption(slice) === Some(1L))
  }

  "JsonSlice(ByteBuffer): a direct buffer is copied once, and keeps its position" >> {
    val buf = ByteBuffer.allocateDirect(12)
    buf.put("xx".getBytes).put(docBytes).put("yy".getBytes).flip()
    buf.position(2).limit(10)
    val slice = JsonSlice(buf)
    (buf.hasArray must beFalse)
      .and(slice === JsonSlice(docBytes))
      .and(slice.offset === 0)
      .and(slice.array.length === 8)
      .and(buf.position === 2)
      .and(buf.limit === 10)
  }

  "JsonSlice(ByteBuffer): a read-only buffer is copied once, and keeps its position" >> {
    val backing = "xx".getBytes ++ docBytes
    val buf = ByteBuffer.wrap(backing).asReadOnlyBuffer()
    buf.position(2)
    val slice = JsonSlice(buf)
    (buf.hasArray must beFalse)
      .and(slice === JsonSlice(docBytes))
      .and((slice.array eq backing) must beFalse)
      .and(slice.offset === 0)
      .and(buf.position === 2)
  }

  "JsonSlice(ByteBuffer): an empty buffer is an empty slice that misses everywhere" >> {
    def empty(buf: ByteBuffer) =
      val slice = JsonSlice(buf)
      (slice.length === 0)
        .and(slice === JsonSlice(Array.emptyByteArray))
        .and(id.inPlace.getOption(slice) === None)
    val atLimit = ByteBuffer.wrap(docBytes)
    atLimit.position(docBytes.length)
    empty(ByteBuffer.allocate(0))
      .and(empty(ByteBuffer.allocateDirect(0)))
      .and(empty(atLimit))
      .and(atLimit.position === docBytes.length)
  }

  "bytes.jsonSlice(offset, length) is JsonSlice(bytes, offset, length), bounds check included" >>
    Prop.forAll(
      Gen.choose(0, 8),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
    ) { (n, offset, length) =>
      val bytes = Array.tabulate[Byte](n)(_.toByte)
      def attempt(make: => JsonSlice) =
        try Right(make)
        catch case e: IndexOutOfBoundsException => Left(e.getClass)
      val viaExtension = attempt(bytes.jsonSlice(offset, length))
      viaExtension == attempt(JsonSlice(bytes, offset, length)) &&
      viaExtension.forall(s => (s.array eq bytes) && s.offset == offset && s.length == length)
    }

  // ----- the scanner's ranged entry points ----------------------------------

  private val paths: List[List[PathStep]] = List(
    Nil,
    List(PathStep.Field("id")),
    List(PathStep.Field("inner"), PathStep.Field("score")),
    List(PathStep.Field("tags"), PathStep.Index(1)),
    List(PathStep.Index(0)),
    List(PathStep.Field("tags"), PathStep.Wildcard),
    List(PathStep.Wildcard),
  )

  "JsonPathScanner over a range: the whole-array spans, shifted by the offset" >>
    Prop.forAll(genDoc.flatMap(embed), Gen.oneOf(paths)) {
      case ((slice, doc), path) =>
        def shift(span: JsonPathScanner.Span) =
          if span.isHit then
            JsonPathScanner.Span(span.start + slice.offset, span.end + slice.offset)
          else span
        val one = JsonPathScanner.find(slice.array, slice.offset, slice.until, path)
        val all = JsonPathScanner.findAll(slice.array, slice.offset, slice.until, path)
        (one == shift(JsonPathScanner.find(doc, path))) :| "find" &&
        (all == JsonPathScanner.findAll(doc, path).map(shift)) :| "findAll"
    }

  // ----- JsonSlice -----------------------------------------------------------

  "JsonSlice: a range outside the array fails fast with IndexOutOfBoundsException" >>
    Prop.forAll(
      Gen.choose(0, 8),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
      Gen.oneOf(Gen.choose(-3, 12), Gen.oneOf(Int.MinValue, Int.MaxValue)),
    ) { (n, offset, length) =>
      val inside = offset >= 0 && length >= 0 && offset.toLong + length <= n
      val built =
        try Right(JsonSlice(new Array[Byte](n), offset, length))
        catch case e: IndexOutOfBoundsException => Left(e)
      built.isRight == inside
    }

  "JsonSlice: an empty slice is a document that misses everywhere" >> {
    val slice = JsonSlice("{\"id\":1}".getBytes, 8, 0)
    (id.inPlace.getOption(slice) === None)
      .and(tags.inPlace.foldMap(List(_))(slice) === Nil)
      .and(id.inPlace.replace(2L)(slice) must beTheSameAs(slice))
  }

  "JsonSlice: equal by content, wherever the bytes sit" >> {
    val a = JsonSlice("..{\"id\":1}".getBytes, 2, 8)
    val b = JsonSlice("{\"id\":1}".getBytes)
    val c = JsonSlice("{\"id\":2}".getBytes)
    (a === b)
      .and(a.hashCode === b.hashCode)
      .and((a == c) === false)
      .and(a.toArray.toSeq === b.array.toSeq)
      .and((a: Any) == "{\"id\":1}" === false)
  }
