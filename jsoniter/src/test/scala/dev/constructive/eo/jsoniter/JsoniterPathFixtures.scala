package dev.constructive.eo.jsoniter

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec

/** Test-only unwrappers for the jsoniter path constructors.
  *
  * `JsoniterPrism.fromPath` / `JsoniterTraversal.fromPath` return `Either[String, …]` — a path is
  * DATA, and application callers handle the message (that is the whole point of the signature).
  * The specs here build optics from LITERAL paths, so a `Left` can only be a typo in the test
  * itself: unwrap once, failing with the parser's own diagnostic.
  */
private[jsoniter] object JsoniterPathFixtures:

  def prism[A](path: String)(using JsonValueCodec[A]): JsoniterPrism[A] =
    JsoniterPrism
      .fromPath[A](path)
      .fold(msg => throw new AssertionError(s"test path '$path': $msg"), identity)

  def traversal[A](path: String)(using JsonValueCodec[A]): JsoniterTraversal[A] =
    JsoniterTraversal
      .fromPath[A](path)
      .fold(msg => throw new AssertionError(s"test path '$path': $msg"), identity)
