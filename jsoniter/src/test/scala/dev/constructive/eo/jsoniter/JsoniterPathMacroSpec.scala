package dev.constructive.eo
package jsoniter

import scala.compiletime.testing.typeCheckErrors

/** `JsoniterPrism.path` / `JsoniterTraversal.path`: a literal path checked at compile time builds
  * the same optic `fromPath` does, and every path `fromPath` would reject is a compile error with
  * the same message.
  */
class JsoniterPathMacroSpec extends Specification:

  import SpecCodecs.given
  import optics.Optic.*

  private val doc = """{"a":{"b":7},"xs":[1,2,3]}""".getBytes("UTF-8")

  private def errors(code: List[scala.compiletime.testing.Error]): List[String] =
    code.map(_.message)

  "path builds the optic fromPath builds" >> {
    val viaMacro = JsoniterPrism.path[Long]("$.a.b")
    val viaParse = JsoniterPrism.fromPath[Long]("$.a.b").toOption.get
    val each = JsoniterTraversal.path[Long]("$.xs[*]")
    (viaMacro.foldMap(identity)(doc) === viaParse.foldMap(identity)(doc))
      .and(viaMacro.foldMap(identity)(doc) === 7L)
      .and(each.foldMap(identity[Long])(doc) === 6L)
  }

  "a malformed literal is a compile error with fromPath's message" >> {
    val expected = JsoniterPrism.fromPath[Long]("$.a.").left.toOption.get
    errors(typeCheckErrors("""JsoniterPrism.path[Long]("$.a.")""")) === List(expected)
  }

  "a wildcard in a prism path is a compile error naming JsoniterTraversal" >> {
    errors(typeCheckErrors("""JsoniterPrism.path[Long]("$.xs[*]")""")).mkString must contain(
      "use JsoniterTraversal"
    )
  }

  "a non-literal path is refused, pointing at fromPath" >> {
    errors(
      typeCheckErrors("""val p = "$.a.b"; JsoniterTraversal.path[Long](p)""")
    ).mkString must contain("use fromPath")
  }
