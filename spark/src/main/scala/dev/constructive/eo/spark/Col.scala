package dev.constructive.eo
package spark

import org.apache.spark.sql.Column

/** A Spark [[org.apache.spark.sql.Column]] expression tagged with the Scala shape `A` of the values
  * it evaluates to. Erased at runtime: a `Col[A]` IS a `Column`.
  *
  * A column is a plain Scala value — an unevaluated expression — so an optic whose source and focus
  * are `Col`s is an ordinary eo optic: `get` / `replace` build expressions, and Spark evaluates the
  * composed expression per row. `A` is a phantom: nothing checks it against the column's runtime
  * `DataType`, which is why constructors stay in this module and take the type on trust from a
  * schema or a selector.
  */
opaque type Col[A] = Column

object Col:

  /** Tag an untyped column. The caller vouches that `column` evaluates to values of shape `A`. */
  def apply[A](column: Column): Col[A] = column

  extension [A](c: Col[A])
    /** The underlying untyped Spark column. */
    def column: Column = c

    /** View a possibly-NULL column as `Option`-valued: NULL reads back as `None`. This is how a
      * per-row miss of a partial focus (`getOption` on a plan-time hit) surfaces in Scala.
      */
    def asOption: Col[Option[A]] = c
