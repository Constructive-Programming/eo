package dev.constructive.eo.jsoniter

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
    * [[Miss]] if any step fails to match.
    *
    * @group Scanner
    */
  def find(bytes: Array[Byte], path: List[PathStep]): Span =
    val start0 = skipWs(bytes, 0)
    if start0 >= bytes.length then Miss else walk(bytes, start0, path)

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

  // ----- object field lookup -------------------------------------------

  /** From a position just past `{`, scan key/value pairs looking for `target`. Returns the position
    * of the value, or -1 if not found / malformed.
    */
  private def findFieldValue(bytes: Array[Byte], pos0: Int, target: String): Int =
    var pos = skipWs(bytes, pos0)
    if pos < bytes.length && bytes(pos) == '}' then return -1
    while pos < bytes.length do
      // Read key
      pos = skipWs(bytes, pos)
      if pos >= bytes.length || bytes(pos) != '"' then return -1
      val keyEnd = skipString(bytes, pos)
      if keyEnd < 0 then return -1
      val matches = stringEqualsAscii(bytes, pos + 1, keyEnd - 1, target)

      // Skip to ':'
      pos = skipWs(bytes, keyEnd)
      if pos >= bytes.length || bytes(pos) != ':' then return -1
      pos = skipWs(bytes, pos + 1)

      if matches then return pos

      // Skip value, then comma / close
      pos = skipValue(bytes, pos)
      if pos < 0 then return -1
      pos = skipWs(bytes, pos)
      if pos >= bytes.length then return -1
      bytes(pos) match
        case ',' => pos = skipWs(bytes, pos + 1)
        case '}' => return -1
        case _   => return -1
    -1

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
      var i = 0
      while i < len do
        if bytes(from + i) != target.charAt(i).toByte then return false
        i += 1
      true

  // ----- array element lookup ------------------------------------------

  /** From a position just past `[`, skip `targetIdx` elements and return the position of the
    * target. Returns -1 on miss / malformed.
    */
  private def findArrayElement(bytes: Array[Byte], pos0: Int, targetIdx: Int): Int =
    var pos = skipWs(bytes, pos0)
    if pos < bytes.length && bytes(pos) == ']' then return -1
    var idx = 0
    while pos < bytes.length do
      if idx == targetIdx then return pos
      pos = skipValue(bytes, pos)
      if pos < 0 then return -1
      pos = skipWs(bytes, pos)
      if pos >= bytes.length then return -1
      bytes(pos) match
        case ',' => pos = skipWs(bytes, pos + 1); idx += 1
        case ']' => return -1
        case _   => return -1
    -1

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
    var pos = skipWs(bytes, pos0)
    if pos < bytes.length && bytes(pos) == '}' then return pos + 1
    while pos < bytes.length do
      pos = skipWs(bytes, pos)
      if pos >= bytes.length || bytes(pos) != '"' then return -1
      pos = skipString(bytes, pos)
      if pos < 0 then return -1
      pos = skipWs(bytes, pos)
      if pos >= bytes.length || bytes(pos) != ':' then return -1
      pos = skipValue(bytes, pos + 1)
      if pos < 0 then return -1
      pos = skipWs(bytes, pos)
      if pos >= bytes.length then return -1
      bytes(pos) match
        case ',' => pos += 1
        case '}' => return pos + 1
        case _   => return -1
    -1

  private def skipArray(bytes: Array[Byte], pos0: Int): Int =
    var pos = skipWs(bytes, pos0)
    if pos < bytes.length && bytes(pos) == ']' then return pos + 1
    while pos < bytes.length do
      pos = skipValue(bytes, pos)
      if pos < 0 then return -1
      pos = skipWs(bytes, pos)
      if pos >= bytes.length then return -1
      bytes(pos) match
        case ',' => pos += 1
        case ']' => return pos + 1
        case _   => return -1
    -1

  /** Skip a JSON string starting at `pos` (pointing at the opening `"`). Returns the position just
    * past the closing quote. Handles `\"`, `\\`, `\/`, `\b`, `\f`, `\n`, `\r`, `\t`, `\uXXXX`.
    * Returns -1 on unterminated string.
    */
  private def skipString(bytes: Array[Byte], pos: Int): Int =
    var i = pos + 1
    while i < bytes.length do
      bytes(i) match
        case '"'  => return i + 1
        case '\\' =>
          i += 2 // any escape is one extra byte; \uXXXX consumes 5 but the trailing 4 are scanned through naturally
        case _ => i += 1
    -1

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
    var i = pos
    if bytes(i) == '-' then i += 1
    while i < bytes.length && bytes(i) >= '0' && bytes(i) <= '9' do i += 1
    if i < bytes.length && bytes(i) == '.' then
      i += 1
      while i < bytes.length && bytes(i) >= '0' && bytes(i) <= '9' do i += 1
    if i < bytes.length && (bytes(i) == 'e' || bytes(i) == 'E') then
      i += 1
      if i < bytes.length && (bytes(i) == '+' || bytes(i) == '-') then i += 1
      while i < bytes.length && bytes(i) >= '0' && bytes(i) <= '9' do i += 1
    i

  // ----- byte-level helpers --------------------------------------------

  private def skipWs(bytes: Array[Byte], pos: Int): Int =
    var i = pos
    while i < bytes.length && (bytes(i) match
        case ' ' | '\t' | '\n' | '\r' => true
        case _                        => false)
    do i += 1
    i

  private def peek(bytes: Array[Byte], pos: Int): Byte =
    if pos >= bytes.length then 0 else bytes(pos)

  private def matches(bytes: Array[Byte], pos: Int, target: String): Boolean =
    if pos + target.length > bytes.length then false
    else
      var i = 0
      while i < target.length do
        if bytes(pos + i) != target.charAt(i).toByte then return false
        i += 1
      true
