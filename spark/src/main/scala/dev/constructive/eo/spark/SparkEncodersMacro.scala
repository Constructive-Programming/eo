package dev.constructive.eo
package spark

import scala.quoted.*
import scala.quoted.Expr as QuotedExpr // hearth's `Expr` shadows it inside the macro class
import scala.reflect.ClassTag

import org.apache.spark.sql.Row
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoder
import org.apache.spark.sql.catalyst.encoders.AgnosticEncoders.EncoderField
import org.apache.spark.sql.types.Metadata

/** Quoted-macro implementation behind [[SparkEncoders.derived]]. */
object SparkEncodersMacro:

  def derivedImpl[T: Type](using q: Quotes): Expr[AgnosticEncoder[T]] =
    new HearthEncoderMacro(q).derive[T]

/** Hearth-backed encoder derivation: `CaseClass.parse` lists the fields, `caseFieldValuesAt` reads
  * them for `encode`, and `construct[Id]` calls the primary constructor for `decode` — `new T(...)`
  * straight from the row, with no `Mirror` and no intermediate `Product`.
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
    val names: List[String] = cc.caseFields.map(_.name)

    // One struct field per constructor parameter, named by the parameter, nullable exactly when
    // the field's own encoder is.
    val fields: List[Expr[EncoderField]] = tTpe.typeSymbol.caseFields.map { f =>
      tTpe.memberType(f).asType match
        case '[a] =>
          val e = QuotedExpr.summon[AgnosticEncoder[a]].getOrElse {
            fail[T](s"no AgnosticEncoder[${Type.prettyPrint[a]}] for field '${f.name}'")
          }
          val name = scala.quoted.Expr(f.name)
          '{ EncoderField($name, $e, $e.nullable, Metadata.empty, None, None) }
    }

    val classTag = QuotedExpr.summon[ClassTag[T]].getOrElse(fail[T]("no ClassTag"))

    val encode: Expr[T => Row] =
      '{ (in: T) =>
        Row.fromSeq(${
          QuotedExpr.ofList(cc.caseFieldValuesAt('{ in }).values.toList.map { v =>
            import v.{Underlying as F, value as fe}
            '{ $fe: Any }
          })
        })
      }

    val decode: Expr[Row => T] =
      '{ (row: Row) =>
        ${
          val built: Id[Option[Expr[T]]] = cc.construct[Id] { (param: Parameter) =>
            import param.tpe.Underlying as P
            val i = scala.quoted.Expr(names.indexOf(param.name))
            Existential[Expr, P]('{ row.get($i).asInstanceOf[P] }): Expr_??
          }
          built.getOrElse(fail[T]("cannot call the primary constructor"))
        }
      }

    '{ SparkEncoders.product[T]($classTag, ${ QuotedExpr.ofList(fields) }, $encode, $decode) }

  private def fail[T: Type](reason: String): Nothing =
    report.errorAndAbort(s"SparkEncoders.derived[${Type.prettyPrint[T]}]: $reason")
