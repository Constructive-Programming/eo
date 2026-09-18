package dev.constructive.eo.avro

import scala.annotation.tailrec

import org.apache.avro.Schema

/** A VERBATIM copy of the scan-shaped nominal rung as PR #98 shipped it, frozen here as a
  * differential oracle for issue #103.
  *
  * Issue #103 replaces the per-case-field linear scan with a per-schema normalised-name index. The
  * doctrine that rung encodes — total and injective or abstain entirely, exact before normalised,
  * two normalised hits is no hit — is NOT up for renegotiation, so the pre-index implementation is
  * kept as executable ground truth and [[NominalResolutionParitySpec]] diffs every verdict of the
  * live [[AvroWalk.totalNominalIndex]] against it.
  *
  * Do not "fix" this object. If it disagrees with `AvroWalk`, the live code changed the doctrine.
  */
private[avro] object NominalResolutionOracle:

  /** Frozen `AvroWalk.totalNominalIndex`. */
  def totalNominalIndex(record: Schema, caseNames: List[String], declIdx: Int): Int =
    val arity = caseNames.size
    if arity == 0 || declIdx < 0 || declIdx >= arity || arity > record.getFields.size then -1
    else
      val out = new Array[Int](arity)
      @tailrec def seen(j: Int, idx: Int): Boolean =
        j < 0 || (out(j) != idx && seen(j - 1, idx))
      @tailrec def loop(i: Int, rest: List[String]): Boolean =
        rest match
          case Nil    => true
          case n :: t =>
            val idx = nominalIndex(record, n)
            if idx < 0 || !seen(i - 1, idx) then false
            else
              out(i) = idx
              loop(i + 1, t)
      if loop(0, caseNames) then out(declIdx) else -1

  /** Frozen `AvroWalk.nominalIndex` — exact hash hit, else a full linear scan with no early exit
    * (the scan is what issue #103 is about: it is `arity x fields.size x nameLength` per hop).
    */
  private def nominalIndex(record: Schema, scalaName: String): Int =
    val exact = record.getField(scalaName)
    if exact != null then exact.pos
    else
      val fields = record.getFields
      @tailrec def loop(i: Int, found: Int): Int =
        if i >= fields.size then found
        else if sameFieldName(fields.get(i).name, scalaName) then
          if found >= 0 then -1 else loop(i + 1, i)
        else loop(i + 1, found)
      loop(0, -1)

  /** Frozen `AvroWalk.sameFieldName`. */
  private def sameFieldName(a: String, b: String): Boolean =
    @tailrec def skip(s: String, i: Int): Int =
      if i >= s.length then i
      else
        val c = s.charAt(i)
        if c == '_' || c == '-' || c == '.' then skip(s, i + 1) else i
    @tailrec def loop(i: Int, j: Int): Boolean =
      val x = skip(a, i)
      val y = skip(b, j)
      if x >= a.length || y >= b.length then x >= a.length && y >= b.length
      else if Character.toLowerCase(a.charAt(x)) != Character.toLowerCase(b.charAt(y)) then false
      else loop(x + 1, y + 1)
    loop(0, 0)

end NominalResolutionOracle
