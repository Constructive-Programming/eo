package dev.constructive.eo.jsoniter

import scala.language.dynamics

import com.github.plokhotnyuk.jsoniter_scala.core.JsonValueCodec

/** The path builder every eo-jsoniter optic drills with: `.field(_.x)`, `.at(i)` and the Dynamic
  * sugar `.x`, each compile-time checked against `A`'s schema and appending one [[PathStep]].
  * Shared by the whole-array optics ([[JsoniterPrism]], [[JsoniterTraversal]]) and their in-place
  * faces ([[JsoniterInPlacePrism]], [[JsoniterInPlaceTraversal]]), so one set of macros builds all
  * four and `.inPlace` can be taken anywhere in a chain: `JsoniterPrism.inPlace[Person].address`
  * and `JsoniterPrism[Person].address.inPlace` are the same optic.
  *
  * @tparam A
  *   the focus type the next step is checked against
  * @tparam P
  *   the optic one step further on: the same kind of optic over a new focus
  *
  * @group Optics
  */
trait JsoniterPath[A, P[_]] extends Dynamic:

  /** The path so far. */
  private[jsoniter] def steps: List[PathStep]

  /** The same kind of optic on another path, focusing a `B`. */
  private[jsoniter] def withSteps[B](steps: List[PathStep])(using JsonValueCodec[B]): P[B]

  /** Extend the path by a field step. Used by `.field` / `selectDynamic`. */
  final private[jsoniter] def widenField[B](name: String)(using JsonValueCodec[B]): P[B] =
    withSteps[B](steps :+ PathStep.Field(name))

  /** Extend the path by an array-index step. Used by `.at`. */
  final private[jsoniter] def widenIndex[B](i: Int)(using JsonValueCodec[B]): P[B] =
    withSteps[B](steps :+ PathStep.Index(i))

  /** Dynamic field sugar — `JsoniterPrism[Person].name` lowers to
    * `JsoniterPrism[Person].field(_.name)`. Compile-time checked against `A`'s case fields; the
    * field's `JsonValueCodec` is summoned at the call site.
    */
  transparent inline def selectDynamic(inline name: String): Any =
    ${ JsoniterPrismMacro.selectFieldImpl[A, P]('{ this }, 'name) }

object JsoniterPath:

  /** `.field(_.x)` — drill via selector lambda, compile-time checked against `A`'s case fields. */
  extension [A, P[_]](o: JsoniterPath[A, P])

    transparent inline def field[B](
        inline selector: A => B
    )(using codecB: JsonValueCodec[B]): P[B] =
      ${ JsoniterPrismMacro.fieldImpl[A, B, P]('o, 'selector, 'codecB) }

  /** `.at(i)` — drill into the i-th array element. */
  extension [A, P[_]](o: JsoniterPath[A, P])

    transparent inline def at(i: Int): Any =
      ${ JsoniterPrismMacro.atImpl[A, P]('o, 'i) }

/** A prism's path builder: [[JsoniterPath]]'s steps, plus `.each`, which splits the prism into a
  * traversal `T` over the iterated array.
  *
  * @tparam T
  *   the traversal `.each` hands off to
  *
  * @group Optics
  */
trait JsoniterPrismPath[A, P[_], T[_]] extends JsoniterPath[A, P]:

  /** The traversal over the elements of the array here, focusing a `B`. Used by `.each`. */
  private[jsoniter] def toTraversal[B](using JsonValueCodec[B]): T[B]

object JsoniterPrismPath:

  /** `.each` — split into a traversal over the iterated array. */
  extension [A, P[_], T[_]](o: JsoniterPrismPath[A, P, T])

    transparent inline def each: Any =
      ${ JsoniterPrismMacro.eachImpl[A, T]('o) }
