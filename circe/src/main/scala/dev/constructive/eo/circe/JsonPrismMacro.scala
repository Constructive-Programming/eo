package dev.constructive.eo.circe

import scala.quoted.*

import dev.constructive.eo.generics.MacroSelectors
import io.circe.{Decoder, Encoder}

/** Macros backing `JsonPrism.field(_.fieldName)`, `selectDynamic`, `at(i)`, `each`, `fields`.
  * Extracts the field name from the selector AST (same pattern as eo-generics' `lens[S](_.field)`)
  * and emits a `widenPath` call.
  *
  * {{{
  *   val streetPrism: JsonPrism[String] =
  *     codecPrism[Person].field(_.address).field(_.street)
  * }}}
  */
object JsonPrismMacro:

  /** Child `JsonPrism[B]` from `parent: JsonPrism[A]` via a `(_.field)` selector. */
  def fieldImpl[A: Type, B: Type](
      parent: Expr[JsonPrism[A]],
      selector: Expr[A => B],
      encB: Expr[Encoder[B]],
      decB: Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonPrism[B]] =
    import quotes.reflect.*

    val name: String = MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "JsonPrism.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + "Nested paths are not yet supported inside a single call;\n"
          + "chain them: `_.field(_.a).field(_.b)`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenPath[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Drives `codecPrism[Person].address`. Looks the field up on `A`'s schema, summons its codecs,
    * emits `widenPath`. Returns `Expr[Any]` so `transparent inline` refines per call site.
    */
  def selectFieldImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsonPrism selectDynamic", nameE) {
      [b] => (
          name: String,
          enc: Expr[Encoder[b]],
          dec: Expr[Decoder[b]],
      ) => '{ $parent.widenPath[b](${ Expr(name) })(using $enc, $dec) }
    }

  /** `.at(i)` — verifies `A <: Iterable`, summons element codecs, emits `widenPathIndex`. */
  def atImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("JsonPrism.at")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonPrism.at: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenPathIndex[b]($iE)(using $enc, $dec) }

  /** `.each` — emits `toTraversal[B]` over `A`'s element type. */
  def eachImpl[A: Type](
      parent: Expr[JsonPrism[A]]
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("JsonPrism.each")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonPrism.each: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.toTraversal[b](using $enc, $dec) }

  /** Traversal counterpart to [[fieldImpl]] — extends the suffix by a named field. */
  def fieldTraversalImpl[A: Type, B: Type](
      parent: Expr[JsonTraversal[A]],
      selector: Expr[A => B],
      encB: Expr[Encoder[B]],
      decB: Expr[Decoder[B]],
  )(using q: Quotes): Expr[JsonTraversal[B]] =
    import quotes.reflect.*

    val name: String = MacroSelectors.extractFieldName(selector.asTerm).getOrElse {
      report.errorAndAbort(
        "JsonTraversal.field: selector must be a single-field accessor like `_.fieldName`.\n"
          + s"Got: ${selector.asTerm.show}"
      )
    }

    '{
      $parent.widenSuffix[B](${ Expr(name) })(using $encB, $decB)
    }

  /** Traversal counterpart to [[atImpl]] — extends the suffix by an array index. */
  def atTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      iE: Expr[Int],
  )(using q: Quotes): Expr[Any] =
    val elemTpe = MacroSelectors.iterableElementType[A]("JsonTraversal.at")

    elemTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"JsonTraversal.at: no given $role[${Type
              .show[b]}] in scope for the element type of ${Type.show[A]}."
        )
        '{ $parent.widenSuffixIndex[b]($iE)(using $enc, $dec) }

  /** `.fields(_.a, _.b, ...)` — multi-field focus. Validates arity ≥ 2, duplicate-ness, known
    * fields, non-nested. Synthesises an NT in SELECTOR order, summons codecs, emits
    * `toFieldsPrism`. Error-message catalogue tested by `FieldsMacroErrorSpec`.
    */
  def fieldsImpl[A: Type](
      parent: Expr[JsonPrism[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("JsonPrism.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          enc: Expr[Encoder[nt]],
          dec: Expr[Decoder[nt]],
      ) => '{ $parent.toFieldsPrism[nt]($namesExpr)(using $enc, $dec) }
    }

  /** Traversal counterpart to [[fieldsImpl]]. */
  def fieldsTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      selectorsE: Expr[Seq[A => Any]],
  )(using q: Quotes): Expr[Any] =
    fieldsCommon[A]("JsonTraversal.fields", selectorsE) {
      [nt] => (
          namesExpr: Expr[Array[String]],
          enc: Expr[Encoder[nt]],
          dec: Expr[Decoder[nt]],
      ) => '{ $parent.toFieldsTraversal[nt]($namesExpr)(using $enc, $dec) }
    }

  /** Shared backbone for the prism / traversal `fieldsImpl` — validation + SELECTOR-order
    * NamedTuple synthesis live in the shared `MacroSelectors.fieldsSelectorNT` (eo-generics); this
    * stub owns the module-specific parts: the `Encoder` / `Decoder` pair summon (vs avro's single
    * `AvroCodec`) with the kindlings derivation hint.
    */
  private def fieldsCommon[A: Type](
      who: String,
      selectorsE: Expr[Seq[A => Any]],
  )(
      emit: [nt] => (Expr[Array[String]], Expr[Encoder[nt]], Expr[Decoder[nt]]) => Type[
        nt
      ] ?=> Expr[
        Any
      ]
  )(using q: Quotes): Expr[Any] =
    val (selectedNames, _, ntTpe) = MacroSelectors.fieldsSelectorNT[A](who, selectorsE)
    ntTpe.asType match
      case '[nt] =>
        val (enc, dec) = summonCodecs[nt](role =>
          s"$who[${Type.show[A]}]: no given $role[${Type.show[nt]}] in scope."
            + s" Derive one via `given Codec.AsObject[${Type.show[nt]}] ="
            + " KindlingsCodecAsObject.derived`"
            + " (`import hearth.kindlings.circederivation.KindlingsCodecAsObject`,"
            + " dependency `\"com.kubuszok\" %% \"kindlings-circe-derivation\" % \"0.3.0\"`),"
            + " or provide one manually."
        )
        val namesExpr: Expr[Array[String]] =
          '{ Array[String](${ Varargs(selectedNames.map(Expr(_))) }*) }
        emit[nt](namesExpr, enc, dec)

  /** Traversal counterpart to [[selectFieldImpl]] — drives Dynamic sugar by extending the suffix.
    */
  def selectFieldTraversalImpl[A: Type](
      parent: Expr[JsonTraversal[A]],
      nameE: Expr[String],
  )(using q: Quotes): Expr[Any] =
    selectDynamicCommon[A]("JsonTraversal selectDynamic", nameE) {
      [b] => (
          name: String,
          enc: Expr[Encoder[b]],
          dec: Expr[Decoder[b]],
      ) => '{ $parent.widenSuffix[b](${ Expr(name) })(using $enc, $dec) }
    }

  /** Shared backbone for `selectFieldImpl` (JsonPrism side) and `selectFieldTraversalImpl`
    * (JsonTraversal side). Validates the literal-string name, looks the field up on `A`'s
    * case-class schema, widens the field type, summons `Encoder` / `Decoder`, and routes through
    * the caller-supplied `emit` callback for the side-specific final expression (`widenPath` vs
    * `widenSuffix`).
    */
  private def selectDynamicCommon[A: Type](
      who: String,
      nameE: Expr[String],
  )(emit: [b] => (String, Expr[Encoder[b]], Expr[Decoder[b]]) => Type[b] ?=> Expr[Any])(using
      q: Quotes
  ): Expr[Any] =
    val (name, _, fieldTpe) = MacroSelectors.caseFieldType[A](who, nameE)
    fieldTpe.asType match
      case '[b] =>
        val (enc, dec) = summonCodecs[b](role =>
          s"$who: no given $role[${Type
              .show[b]}] in scope for field '$name' of ${Type.show[A]}."
        )
        emit[b](name, enc, dec)

  /** Shared helper: summon both `Encoder[B]` and `Decoder[B]` from the enclosing scope, aborting
    * with `errorMsg(role)` (role = `"Encoder"` / `"Decoder"`) for whichever is missing.
    */
  private def summonCodecs[B: Type](
      errorMsg: String => String
  )(using q: Quotes): (Expr[Encoder[B]], Expr[Decoder[B]]) =
    import quotes.reflect.*
    val enc = Expr.summon[Encoder[B]].getOrElse(report.errorAndAbort(errorMsg("Encoder")))
    val dec = Expr.summon[Decoder[B]].getOrElse(report.errorAndAbort(errorMsg("Decoder")))
    (enc, dec)
