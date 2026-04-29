package dev.constructive.eo.jsoniter

/** Parser for the JSONPath subset we accept in [[JsoniterPrism]] / [[JsoniterTraversal]]. Grammar:
  *
  * {{{
  *   path  := '$' (step)*
  *   step  := '.' ident | '[' int ']'
  *   ident := [A-Za-z_][A-Za-z_0-9]*
  *   int   := [0-9]+
  * }}}
  *
  * Wildcards / filters / recursive descent are not in scope; reject them at parse time so the
  * scanner doesn't have to.
  *
  * @group Parser
  */
object PathParser:

  /** Parse a path string into the step list, or return a human-readable error.
    *
    * @group Parser
    */
  def parse(input: String): Either[String, List[PathStep]] =
    if input.isEmpty then Left("empty path")
    else if input.charAt(0) != '$' then Left(s"path must start with '$$' (got '${input.charAt(0)}')")
    else parseSteps(input, 1, Nil)

  private def parseSteps(
      s: String,
      pos: Int,
      acc: List[PathStep],
  ): Either[String, List[PathStep]] =
    if pos >= s.length then Right(acc.reverse)
    else
      s.charAt(pos) match
        case '.' => parseField(s, pos + 1, acc)
        case '[' => parseIndex(s, pos + 1, acc)
        case c   => Left(s"unexpected '$c' at position $pos (expected '.' or '[')")

  private def parseField(
      s: String,
      pos: Int,
      acc: List[PathStep],
  ): Either[String, List[PathStep]] =
    if pos >= s.length || !isIdentStart(s.charAt(pos)) then
      Left(s"expected identifier at position $pos")
    else
      var end = pos + 1
      while end < s.length && isIdentPart(s.charAt(end)) do end += 1
      val name = s.substring(pos, end)
      parseSteps(s, end, PathStep.Field(name) :: acc)

  private def parseIndex(
      s: String,
      pos: Int,
      acc: List[PathStep],
  ): Either[String, List[PathStep]] =
    var end = pos
    while end < s.length && s.charAt(end).isDigit do end += 1
    if end == pos then Left(s"expected integer at position $pos")
    else if end >= s.length || s.charAt(end) != ']' then
      Left(s"expected ']' at position $end")
    else
      val i = s.substring(pos, end).toInt
      parseSteps(s, end + 1, PathStep.Index(i) :: acc)

  private inline def isIdentStart(c: Char): Boolean =
    (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_'

  private inline def isIdentPart(c: Char): Boolean =
    isIdentStart(c) || (c >= '0' && c <= '9')
