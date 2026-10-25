package dev.constructive.eo
package spark

import scala.annotation.publicInBinary
import scala.deriving.Mirror

import org.apache.spark.sql.Column
import org.apache.spark.sql.functions.lit

import optics.{GetReplaceLens, Lens, Optional}

/** One selector, two interpretations: [[ColumnOptics.path]] turns `_.a.b.c` into the SYMBOLIC optic
  * over `Col[S]` (what Spark runs) and its EAGER twin over `S` (what Scala runs) — addressing the
  * same field by construction, so the differential specs only have to check they BEHAVE alike.
  *
  *   - [[ColumnPath.Total]] — no `Option` on the path: both halves are `Lens`es (the symbolic one
  *     lawful on non-NULL structs, like [[ColumnOptics.field]]).
  *   - [[ColumnPath.Partial]] — the path crosses `Option` fields, marked `.get` in the selector
  *     (`_.work.get.zip`): both halves are `Optional`s. The symbolic one is a guarded
  *     [[ColumnOptics.guarded]] optic whose presence test is "every `Option` prefix is non-NULL".
  *
  * Symbolic writes are ONE `withField` with the dotted path; eager writes rebuild each level
  * through its `Mirror` (no reflection, works for every case class).
  */
sealed trait ColumnPath[S, A]:
  /** Field names along the path (the `.get` markers dropped). */
  def names: List[String]

object ColumnPath:

  /** A total path: `get` / `replace` on both sides. */
  final class Total[S, A] @publicInBinary private[spark] (
      val names: List[String],
      steps: List[Step]
  ) extends ColumnPath[S, A]:

    val symbolic: GetReplaceLens[Col[S], Col[S], Col[A], Col[A]] =
      Lens[Col[S], Col[A]](
        s => Col(read(s.column, names)),
        (s, a) => Col(write(s.column, names, a.column))
      )

    val eager: GetReplaceLens[S, S, A, A] =
      Lens[S, A](
        s => Step.get(s, steps).get.asInstanceOf[A],
        (s, a) => Step.set(s, steps, a).asInstanceOf[S]
      )

  /** A path through `Option` fields: `getOption` / `modify` on both sides, misses pass through. */
  final class Partial[S, A] @publicInBinary private[spark] (
      val names: List[String],
      steps: List[Step]
  ) extends ColumnPath[S, A]:

    /** Dotted-name prefixes that must be non-NULL: one per `.get` on the path. */
    private val nullablePrefixes: List[List[String]] =
      steps
        .foldLeft((List.empty[String], List.empty[List[String]])) {
          case ((seen, acc), Step.Field(name, _, _)) => (seen :+ name, acc)
          case ((seen, acc), Step.Some)              => (seen, acc :+ seen)
        }
        ._2

    val symbolic: Optional[Col[S], Col[S], Col[A], Col[A]] =
      ColumnOptics.guarded[S, A](
        s =>
          Col(
            nullablePrefixes
              .map(p => read(s.column, p).isNotNull)
              .reduceOption(_ && _)
              .getOrElse(lit(true))
          ),
        s => Col(read(s.column, names)),
        (s, a) => Col(write(s.column, names, a.column)),
      )

    val eager: Optional[S, S, A, A] =
      Optional[S, S, A, A](
        s => Step.get(s, steps).map(_.asInstanceOf[A]).toRight(s),
        (s, a) => Step.set(s, steps, a).asInstanceOf[S],
      )

  private def read(c: Column, names: List[String]): Column = names.foldLeft(c)(_.getField(_))

  /** One `withField` with the whole dotted path (each name back-quoted, so dots in names are safe).
    */
  private def write(c: Column, names: List[String], v: Column): Column =
    c.withField(names.map(n => s"`${n.replace("`", "``")}`").mkString("."), v)

  /** A step of an eager path. Built by the macro; untyped at runtime (the macro checked the types).
    */
  enum Step:
    /** Case field `name` at constructor position `index`, rebuilt through `mirror`. */
    case Field(name: String, index: Int, mirror: Mirror.Product)

    /** Into an `Option` (`.get` in the selector): `None` is a miss. */
    case Some

  object Step:

    def get(s: Any, steps: List[Step]): Option[Any] =
      steps.foldLeft(Option(s)) {
        case (scala.Some(v), Field(_, i, _)) =>
          scala.Some(v.asInstanceOf[Product].productElement(i))
        case (scala.Some(v), Some) => v.asInstanceOf[Option[Any]]
        case (scala.None, _)       => scala.None
      }

    /** Rebuild along `steps` with `a` at the end; a `None` on the way leaves the value unchanged.
      */
    def set(s: Any, steps: List[Step], a: Any): Any = steps match
      case Nil                    => a
      case Field(_, i, m) :: rest =>
        val p = s.asInstanceOf[Product]
        m.fromProduct(Tuple.fromArray(Array.tabulate[Object](p.productArity) { j =>
          (if j == i then set(p.productElement(j), rest, a) else p.productElement(j))
            .asInstanceOf[Object]
        }))
      case Some :: rest =>
        s.asInstanceOf[Option[Any]] match
          case scala.Some(v) => scala.Some(set(v, rest, a))
          case scala.None    => scala.None
