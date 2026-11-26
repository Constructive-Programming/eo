package dev.constructive.eo.jsoniter

import scala.annotation.publicInBinary

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec

/** The string-path constructors of eo-jsoniter's optics, written once for [[JsoniterPrism]] and
  * [[JsoniterTraversal]]: `fromPath` for a path known only at runtime, `path` for a literal one.
  * Path syntax: `$`, dotted field names (`$.foo.bar`), array indices (`$[0]`) and — in a traversal
  * only — the `[*]` wildcard. See [[PathParser]] for the full grammar.
  */
abstract class JsoniterPathConstructors[O[_]] private[jsoniter] (
    build: [A] => (List[PathStep], JsonValueCodec[A]) => O[A],
    accepts: (String, List[PathStep]) => Either[String, List[PathStep]],
):

  /** The path's steps, or why it is not a path for this optic. `fromPath` runs it at runtime and
    * `path` at compile time, so both report the same message.
    */
  private[jsoniter] def steps(path: String): Either[String, List[PathStep]] =
    PathParser.parse(path).left.map(e => s"invalid JSONPath '$path': $e").flatMap(accepts(path, _))

  @publicInBinary private[jsoniter] def fromSteps[A](steps: List[PathStep])(using
      codec: JsonValueCodec[A]
  ): O[A] =
    build[A](steps, codec)

  /** Build the optic at a JSONPath known only at runtime, keeping the failure in the type: a path
    * that does not parse, or a `[*]` wildcard given to a prism, comes back as `Left(message)`. A
    * path is DATA (a config value, a CLI argument, a registry lookup), so there is no throwing twin
    * to reach for.
    *
    * {{{
    * given JsoniterCodec[Long] = JsoniterCodec.derived
    *
    * val idP: Either[String, JsoniterPrism[Long]] = JsoniterPrism.fromPath[Long](rawPath)
    * idP.map(_.foldMap(identity)(bytes))   // Either[String, Long], no AST allocation
    * }}}
    *
    * @group Constructors
    */
  def fromPath[A](path: String)(using JsonValueCodec[A]): Either[String, O[A]] =
    steps(path).map(fromSteps[A])

  /** [[fromPath]] for a LITERAL path, checked at compile time: every path `fromPath` would reject
    * is a compile error carrying the same message, and the parsed steps are emitted as a literal,
    * so nothing is parsed, or can fail, at runtime. A non-literal argument is a compile error that
    * points to `fromPath`.
    *
    * {{{
    * val idP: JsoniterPrism[Long] = JsoniterPrism.path[Long]("$.payload.user.id")
    * val itemsT: JsoniterTraversal[Long] = JsoniterTraversal.path[Long]("$.cart.items[*]")
    * }}}
    *
    * @group Constructors
    */
  inline def path[A](inline path: String)(using codec: JsonValueCodec[A]): O[A] =
    ${ JsoniterPrismMacro.path[A, O]('this, 'path, 'codec) }
