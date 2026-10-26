package dev.constructive.eo
package spark

import scala.quoted.*
import scala.quoted.Expr as QuotedExpr // hearth's `Expr` shadows it inside the macro class
import scala.reflect.ClassTag

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.encoders.{AgnosticEncoder, Codec}
import org.apache.spark.sql.catalyst.expressions.GenericRow

/** Quoted-macro implementation behind [[SparkEncoders.derived]]. */
object SparkEncodersMacro:

  def derivedImpl[T: Type](using q: Quotes): Expr[AgnosticEncoder[T]] =
    new HearthEncoderMacro(q).derive[T]

/** Hearth-backed encoder derivation: `CaseClass.parse` lists the fields, `caseFieldValuesAt` reads
  * them for `encode`, and `construct[Id]` calls the primary constructor for `decode` — `new T(...)`
  * straight from the row, with no `Mirror` and no intermediate `Product`. A NULL struct travels
  * through the codec as `null` both ways (Spark's NULL, not a missed optic focus).
  */
final private class HearthEncoderMacro(q: Quotes) extends _root_.hearth.MacroCommonsScala3(using q):

  import quotes.reflect.*
  import _root_.hearth.fp.Id
  import _root_.hearth.fp.instances.*

  def derive[T](using Type[T]): Expr[AgnosticEncoder[T]] =
    val cc = CaseClass.parse[T].toEither match
      case Right(cc)    => cc
      case Left(reason) => fail[T](s"$reason — expected a case class")
    val tTpe = TypeRepr.of[T]
    // One struct field per constructor parameter, named by the parameter.
    val fields: List[Expr[(String, AgnosticEncoder[?])]] = tTpe.typeSymbol.caseFields.map { f =>
      tTpe.memberType(f).asType match
        case '[a] =>
          val e = QuotedExpr.summon[AgnosticEncoder[a]].getOrElse {
            fail[T](s"no AgnosticEncoder[${Type.prettyPrint[a]}] for field '${f.name}'")
          }
          '{ (${ QuotedExpr(f.name) }, $e) }
    }

    val classTag = QuotedExpr.summon[ClassTag[T]].getOrElse(fail[T]("no ClassTag"))

    // The codec is ONE class of its own (an anonymous class at the call site), not two lambdas:
    // the eager bridge ships encoders to the server, and a lambda is deserialized through its
    // enclosing class, which would drag everything that class references along with it.
    '{
      SparkEncoders.product[T](
        $classTag,
        ${ QuotedExpr.ofList(fields) },
        new Codec[T, Row]:
          def encode(in: T): Row =
            if (in: T | Null) == null then null.asInstanceOf[Row]
            else new GenericRow(Array[Any](${ reads(cc, '{ in }) }*))
          def decode(out: Row): T =
            if (out: Row | Null) == null then null.asInstanceOf[T] else ${ construct(cc, '{ out }) }
      )
    }

  /** The field values of `in`, declaration order, as literal varargs: `Array[Any](a, b, …)` then
    * compiles to one array store per field. (`Row.fromSeq` would copy a `Seq` through an
    * `ArrayBuilder` on every row — half the server's CPU in the eager bridge benchmark.)
    */
  private def reads[T: Type](cc: CaseClass[T], in: Expr[T]): Expr[Seq[Any]] =
    Varargs(cc.caseFieldValuesAt(in).values.toList.map { v =>
      import v.{Underlying as F, value as fe}
      '{ $fe: Any }
    })

  /** `new T(...)` from the field values in `row`, by declaration position. */
  private def construct[T: Type](cc: CaseClass[T], row: Expr[Row]): Expr[T] =
    val names: List[String] = cc.caseFields.map(_.name)
    val built: Id[Option[Expr[T]]] = cc.construct[Id] { (param: Parameter) =>
      import param.tpe.Underlying as P
      val i = QuotedExpr(names.indexOf(param.name))
      Existential[Expr, P]('{ $row.get($i).asInstanceOf[P] }): Expr_??
    }
    built.getOrElse(fail[T]("cannot call the primary constructor"))

  private def fail[T: Type](reason: String): Nothing =
    report.errorAndAbort(s"SparkEncoders.derived[${Type.prettyPrint[T]}]: $reason")
