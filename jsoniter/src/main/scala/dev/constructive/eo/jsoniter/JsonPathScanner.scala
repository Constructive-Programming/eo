package dev.constructive.eo.jsoniter

import scala.annotation.tailrec

// This scanner is the project's one sanctioned `return` exception (see `.scalafix.conf`
// `noReturns`). Each method is a hot bytecode-level scan whose guard checks read far better as
// flat early `return`s than as the deep if/else pyramids a return-free rewrite forces. The loops
// are `@tailrec` (the project bans `while`); the early `return`s are the non-recursive miss / hit
// exits, and the sole recursive self-call in each stays in tail position.
// scalafix:off DisableSyntax.return

/** Hand-rolled JSON byte scanner. Resolves a [[PathStep]] list against an `Array[Byte]` JSON
  * document and returns the byte span `[start, end)` of the resolved value, or `-1` for the start
  * if the path doesn't match.
  *
  * Why hand-rolled rather than reusing `jsoniter-scala-core`'s `JsonReader`: the scanner only needs
  * to skip-with-backtracking, not decode. Embedding `JsonReader` would force us to thread its mark
  * / rollback state through every call site, and its API doesn't expose byte offsets directly.
  * 100-ish LoC of dispatch is cheaper than the impedance mismatch.
  *
  * The scanner is permissive about whitespace (skips it everywhere) and strict about structure
  * (unbalanced quotes / brackets short-circuit to "miss"). It does NOT validate the document; an
  * invalid JSON whose path step happens to resolve will return a span — decoding is the layer that
  * surfaces validity failures.
  *
  * @group Scanner
  */
object JsonPathScanner:

  /** Result of a scan: `start >= 0` means hit, `start == -1` means miss. Avoids `Option` allocation
    * on the hot path. End is `bytes.length` on miss.
    *
    * @group Scanner
    */
  final case class Span(start: Int, end: Int):
    inline def isHit: Boolean = start >= 0

  /** The single sentinel "miss" Span. */
  val Miss: Span = Span(-1, -1)

  /** Resolve `path` against `bytes`. Returns the byte span of the focused value (start, end), or
    * [[Miss]] if any step fails to match. Rejects [[PathStep.Wildcard]] — wildcards produce
    * 0-or-many results and don't fit a single-Span return; route them through [[findAll]].
    *
    * @group Scanner
    */
  def find(bytes: Array[Byte], path: List[PathStep]): Span =
    val start0 = skipWs(bytes, 0)
    if start0 >= bytes.length then Miss else walk(bytes, start0, path)

  /** Resolve `path` against `bytes`, returning every span that matches. Wildcard steps fan out
    * across array elements; a path with no wildcards returns 0 or 1 spans. Spans are returned in
    * document order. On any structural mismatch (wrong context for a step, malformed JSON), the
    * sub-tree is silently skipped — the caller sees fewer spans, not an error.
    *
    * @group Scanner
    */
  def findAll(bytes: Array[Byte], path: List[PathStep]): List[Span] =
    val start0 = skipWs(bytes, 0)
    if start0 >= bytes.length then Nil
    else
      val buf = scala.collection.mutable.ListBuffer.empty[Span]
      walkAll(bytes, start0, path, buf)
      buf.toList

  // ----- recursive walker ----------------------------------------------

  private def walk(bytes: Array[Byte], pos: Int, path: List[PathStep]): Span =
    path match
      case Nil =>
        // No steps left — current position is the focus. Skip the value to find its end.
        val end = skipValue(bytes, pos)
        if end < 0 then Miss else Span(pos, end)

      case PathStep.Field(name) :: rest =>
        if peek(bytes, pos) != '{' then Miss
        else
          findFieldValue(bytes, pos + 1, name) match
            case -1 => Miss
            case vp => walk(bytes, vp, rest)

      case PathStep.Index(i) :: rest =>
        if peek(bytes, pos) != '[' then Miss
        else
          findArrayElement(bytes, pos + 1, i) match
            case -1 => Miss
            case vp => walk(bytes, vp, rest)

      case PathStep.Wildcard :: _ =>
        // Wildcard is multi-focus — single-Span [[find]] can't represent it. Route via [[findAll]].
        Miss

  private def walkAll(
      bytes: Array[Byte],
      pos: Int,
      path: List[PathStep],
      buf: scala.collection.mutable.ListBuffer[Span],
  ): Unit =
    path match
      case Nil =>
        val end = skipValue(bytes, pos)
        if end >= 0 then buf += Span(pos, end)
        ()

      case PathStep.Field(name) :: rest =>
        if peek(bytes, pos) == '{' then
          val vp = findFieldValue(bytes, pos + 1, name)
          if vp >= 0 then walkAll(bytes, vp, rest, buf)

      case PathStep.Index(i) :: rest =>
        if peek(bytes, pos) == '[' then
          val vp = findArrayElement(bytes, pos + 1, i)
          if vp >= 0 then walkAll(bytes, vp, rest, buf)

      case PathStep.Wildcard :: rest =>
        if peek(bytes, pos) == '[' then walkAllArrayElements(bytes, pos + 1, rest, buf)

  /** From a position just past `[`, walk every element and apply `rest` to each, accumulating into
    * `buf`. On an empty array (`[]`), produces no spans.
    */
  private def walkAllArrayElements(
      bytes: Array[Byte],
      pos0: Int,
      rest: List[PathStep],
      buf: scala.collection.mutable.ListBuffer[Span],
  ): Unit =
    val start = skipWs(bytes, pos0)
    if start < bytes.length && bytes(start) == ']' then ()
    else walkAllArrayElementsLoop(bytes, start, rest, buf)

  /** Loop body of [[walkAllArrayElements]]: apply `rest` to the element at `pos`, then advance past
    * the following comma to the next element (stopping at `]` or end-of-input).
    */
  @tailrec private def walkAllArrayElementsLoop(
      bytes: Array[Byte],
      pos: Int,
      rest: List[PathStep],
      buf: scala.collection.mutable.ListBuffer[Span],
  ): Unit =
    if pos >= bytes.length then return
    walkAll(bytes, pos, rest, buf)
    val p1 = skipValue(bytes, pos)
    if p1 < 0 then return
    val p2 = skipWs(bytes, p1)
    if p2 >= bytes.length then return
    bytes(p2) match
      case ',' => walkAllArrayElementsLoop(bytes, skipWs(bytes, p2 + 1), rest, buf)
      case _   => () // ']' or malformed

  // ----- object field lookup -------------------------------------------

  /** From a position just past `{`, scan key/value pairs looking for `target`. Returns the position
    * of the value, or -1 if not found / malformed.
    */
  private def findFieldValue(bytes: Array[Byte], pos0: Int, target: String): Int =
    val start = skipWs(bytes, pos0)
    if start < bytes.length && bytes(start) == '}' then -1
    else findFieldValueLoop(bytes, start, target)

  /** Loop body of [[findFieldValue]]: at each key/value pair, return the value position on a key
    * match, else skip the value and advance past the following comma (stopping at `}` or end).
    */
  @tailrec private def findFieldValueLoop(bytes: Array[Byte], pos: Int, target: String): Int =
    if pos >= bytes.length then return -1
    val kpos = skipWs(bytes, pos) // key
    if kpos >= bytes.length || bytes(kpos) != '"' then return -1
    val keyEnd = skipString(bytes, kpos)
    if keyEnd < 0 then return -1
    val matches = stringEqualsAscii(bytes, kpos + 1, keyEnd - 1, target)
    val cpos = skipWs(bytes, keyEnd) // ':'
    if cpos >= bytes.length || bytes(cpos) != ':' then return -1
    val vpos = skipWs(bytes, cpos + 1)
    if matches then return vpos
    val ap = skipValue(bytes, vpos) // skip value, then comma / close
    if ap < 0 then return -1
    val bp = skipWs(bytes, ap)
    if bp >= bytes.length then return -1
    bytes(bp) match
      case ',' => findFieldValueLoop(bytes, skipWs(bytes, bp + 1), target)
      case _   => -1 // '}' or malformed

  /** Compare a JSON-encoded string slice (without surrounding quotes — `[from, until)` covers the
    * inner bytes) against an ASCII target. Returns true on byte-for-byte match.
    *
    * Permissive: doesn't decode JSON-string escapes, only matches when the encoded form is
    * literally the target. Field names with escapes will not match by ASCII compare — acceptable
    * for v1 since real-world JSON keys are overwhelmingly plain ASCII.
    */
  private def stringEqualsAscii(
      bytes: Array[Byte],
      from: Int,
      until: Int,
      target: String,
  ): Boolean =
    val len = until - from
    if len != target.length then false
    else
      @tailrec def loop(i: Int): Boolean =
        if i < len then
          if bytes(from + i) != target.charAt(i).toByte then false
          else loop(i + 1)
        else true
      loop(0)

  // ----- array element lookup ------------------------------------------

  /** From a position just past `[`, skip `targetIdx` elements and return the position of the
    * target. Returns -1 on miss / malformed.
    */
  private def findArrayElement(bytes: Array[Byte], pos0: Int, targetIdx: Int): Int =
    val start = skipWs(bytes, pos0)
    if start < bytes.length && bytes(start) == ']' then -1
    else findArrayElementLoop(bytes, start, 0, targetIdx)

  /** Loop body of [[findArrayElement]]: return `pos` when `idx` reaches `targetIdx`, else skip the
    * element and advance past the following comma (stopping at `]` or end-of-input).
    */
  @tailrec private def findArrayElementLoop(
      bytes: Array[Byte],
      pos: Int,
      idx: Int,
      targetIdx: Int,
  ): Int =
    if pos >= bytes.length then return -1
    if idx == targetIdx then return pos
    val p1 = skipValue(bytes, pos)
    if p1 < 0 then return -1
    val p2 = skipWs(bytes, p1)
    if p2 >= bytes.length then return -1
    bytes(p2) match
      case ',' => findArrayElementLoop(bytes, skipWs(bytes, p2 + 1), idx + 1, targetIdx)
      case _   => -1 // ']' or malformed

  // ----- value-skip dispatch -------------------------------------------

  /** Skip the JSON value at `pos`; return the position just past it, or -1 on malformed input. */
  private def skipValue(bytes: Array[Byte], pos: Int): Int =
    val p = skipWs(bytes, pos)
    if p >= bytes.length then -1
    else
      bytes(p) match
        case '{'                                     => skipObject(bytes, p + 1)
        case '['                                     => skipArray(bytes, p + 1)
        case '"'                                     => skipString(bytes, p)
        case 't' | 'f' | 'n'                         => skipLiteral(bytes, p)
        case c if c == '-' || (c >= '0' && c <= '9') => skipNumber(bytes, p)
        case _                                       => -1

  private def skipObject(bytes: Array[Byte], pos0: Int): Int =
    val start = skipWs(bytes, pos0)
    if start < bytes.length && bytes(start) == '}' then start + 1
    else skipObjectLoop(bytes, start)

  /** Loop body of [[skipObject]]: skip each `"key": value` pair, advancing past commas; return the
    * position just past the closing `}`, or -1 on malformed input.
    */
  @tailrec private def skipObjectLoop(bytes: Array[Byte], pos: Int): Int =
    if pos >= bytes.length then return -1
    val kpos = skipWs(bytes, pos)
    if kpos >= bytes.length || bytes(kpos) != '"' then return -1
    val keyEnd = skipString(bytes, kpos)
    if keyEnd < 0 then return -1
    val cpos = skipWs(bytes, keyEnd)
    if cpos >= bytes.length || bytes(cpos) != ':' then return -1
    val vpos = skipValue(bytes, cpos + 1)
    if vpos < 0 then return -1
    val np = skipWs(bytes, vpos)
    if np >= bytes.length then return -1
    bytes(np) match
      case ',' => skipObjectLoop(bytes, np + 1)
      case '}' => np + 1
      case _   => -1

  private def skipArray(bytes: Array[Byte], pos0: Int): Int =
    @tailrec def loop(pos: Int): Int =
      if pos < bytes.length then
        val p1 = skipValue(bytes, pos)
        if p1 < 0 then -1
        else
          val p2 = skipWs(bytes, p1)
          if p2 >= bytes.length then -1
          else
            bytes(p2) match
              case ',' => loop(p2 + 1)
              case ']' => p2 + 1
              case _   => -1
      else -1
    val start = skipWs(bytes, pos0)
    if start < bytes.length && bytes(start) == ']' then start + 1
    else loop(start)

  /** Skip a JSON string starting at `pos` (pointing at the opening `"`). Returns the position just
    * past the closing quote. Handles `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, `\uXXXX`.
    * Returns -1 on unterminated string.
    */
  private def skipString(bytes: Array[Byte], pos: Int): Int =
    @tailrec def loop(i: Int): Int =
      if i < bytes.length then
        bytes(i) match
          case '"'  => i + 1
          case '\\' =>
            loop(
              i + 2
            ) // any escape is one extra byte; \uXXXX consumes 5 but the trailing 4 are scanned through naturally
          case _ => loop(i + 1)
      else -1
    loop(pos + 1)

  /** Skip `true` / `false` / `null`. Returns -1 if the literal isn't a recognised one. */
  private def skipLiteral(bytes: Array[Byte], pos: Int): Int =
    bytes(pos) match
      case 't' if matches(bytes, pos, "true")  => pos + 4
      case 'f' if matches(bytes, pos, "false") => pos + 5
      case 'n' if matches(bytes, pos, "null")  => pos + 4
      case _                                   => -1

  /** Skip a number — sign, digits, fraction, exponent. Returns the position just past the last
    * numeric char.
    */
  private def skipNumber(bytes: Array[Byte], pos: Int): Int =
    val afterSign = if bytes(pos) == '-' then pos + 1 else pos
    val afterInt = skipDigits(bytes, afterSign)
    val afterFrac =
      if afterInt < bytes.length && bytes(afterInt) == '.' then skipDigits(bytes, afterInt + 1)
      else afterInt
    if afterFrac < bytes.length && (bytes(afterFrac) == 'e' || bytes(afterFrac) == 'E') then
      val afterE = afterFrac + 1
      val afterExpSign =
        if afterE < bytes.length && (bytes(afterE) == '+' || bytes(afterE) == '-') then afterE + 1
        else afterE
      skipDigits(bytes, afterExpSign)
    else afterFrac

  /** Skip a run of ASCII digits from `i`; return the first non-digit position (or `i` if none). */
  @tailrec private def skipDigits(bytes: Array[Byte], i: Int): Int =
    if i < bytes.length && bytes(i) >= '0' && bytes(i) <= '9' then skipDigits(bytes, i + 1)
    else i

  // ----- byte-level helpers --------------------------------------------

  private def skipWs(bytes: Array[Byte], pos: Int): Int =
    @tailrec def loop(i: Int): Int =
      if i < bytes.length && (bytes(i) match
          case ' ' | '\t' | '\n' | '\r' => true
          case _                        => false)
      then loop(i + 1)
      else i
    loop(pos)

  private def peek(bytes: Array[Byte], pos: Int): Byte =
    if pos >= bytes.length then 0 else bytes(pos)

  private def matches(bytes: Array[Byte], pos: Int, target: String): Boolean =
    if pos + target.length > bytes.length then false
    else
      @tailrec def loop(i: Int): Boolean =
        if i < target.length then
          if bytes(pos + i) != target.charAt(i).toByte then false
          else loop(i + 1)
        else true
      loop(0)
