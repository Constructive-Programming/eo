package dev.constructive.eo
package bench

import java.util.concurrent.TimeUnit

import scala.annotation.tailrec

import io.scalaland.chimney.dsl.*
import io.scalaland.chimney.{partial, PartialTransformer, Patcher, Transformer}
import org.openjdk.jmh.annotations.*

import bench.fixture.*
import chimney.*
import generics.lens
import optics.Traversal

/** `cats-eo-chimney` vs plain chimney vs hand-written code, one row triple per scenario:
  *
  *   - `eo*` — the cats-eo-chimney bridge, optics built once in fields;
  *   - `chimney*` — the same job with chimney alone (no eo), transformers built once in fields, the
  *     DSL (`into…withFieldComputed`) where a field override is needed;
  *   - `hand*` — what you would write with neither library.
  *
  * The headline is the transform quartet — `transformFocus` / `transformFocusPartial` (retype every
  * focus of a 64-element list) and `transformImage` / `transformImagePartial` (edit a field, hand
  * the result back as another type). The bridge scenarios after it (convert-then-read, edit through
  * a `Codec`, read with errors, patch a nested part) cover the rest of the module.
  *
  * Only the success paths are measured: a failed chimney transformation builds an error chain and
  * would measure that instead of the transform.
  */
@State(Scope.Benchmark)
@BenchmarkMode(Array(Mode.AverageTime))
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@Fork(3)
@Warmup(iterations = 3, time = 1)
@Measurement(iterations = 5, time = 1)
class ChimneyBench extends JmhDefaults:

  private val raws: List[ChRawPrice] = List.tabulate(64)(i => ChRawPrice(i.toLong * 100))
  private val user = ChUser("Ada", 36, ChAddress("1 Analytical St", "London"))
  private val dto = ChAccountDto("ada", 120L)
  private val order = ChOrder(1L, ChCustomer("Ada", "ada@old", "000"))
  private val contact = ChContactPatch("ada@new", "111")

  // ---- chimney-only values, derived once ---------------------------------------------------

  // Givens, so the eo extensions below summon these very instances (the two sides share them).
  private given priceT: Transformer[ChRawPrice, ChPrice] = Transformer.derive
  private val pricesT: Transformer[List[ChRawPrice], List[ChPrice]] = Transformer.derive

  private given pricePT: PartialTransformer[ChRawPrice, ChPrice] =
    ChPrice.given_PartialTransformer_ChRawPrice_ChPrice

  // Both a total and a partial element transformer are in scope: tell chimney to use the partial.
  private val pricesPT: PartialTransformer[List[ChRawPrice], List[ChPrice]] =
    PartialTransformer
      .define[List[ChRawPrice], List[ChPrice]]
      .enableImplicitConflictResolution(PreferPartialTransformer)
      .buildTransformer

  private val viewT: Transformer[ChUser, ChUserView] = Transformer.derive
  private val accountPT: PartialTransformer[ChAccountDto, ChAccount] = PartialTransformer.derive
  private val accountT: Transformer[ChAccount, ChAccountDto] = Transformer.derive
  private val contactP: Patcher[ChCustomer, ChContactPatch] = Patcher.derive

  // ---- eo-chimney optics, built once -------------------------------------------------------

  private val prices = Traversal.pEach[List, ChRawPrice, ChPrice]
  private val eoTransformFocusFn = prices.transformFocus
  private val eoTransformFocusPartialFn = prices.transformFocusPartial
  private val nameToView = lens[ChUser](_.name).transformImage[ChUserView](using viewT)

  private val balanceToAccount =
    lens[ChAccountDto](_.balance).transformImagePartial[ChAccount](using accountPT)

  private val viewCity = Chimney
    .getter[ChUser, ChUserView](using viewT)
    .andThen(lens[ChUserView](_.address))
    .andThen(lens[ChAddressView](_.city))

  private val dtoOwner =
    io.scalaland.chimney.Codec(accountT, accountPT).toPrism.andThen(lens[ChAccount](_.owner))

  private val decodedOwner = accountPT.toResultFold().andThen(lens[ChAccount](_.owner))
  private val customer = lens[ChOrder](_.customer)

  // ==== the quartet: transformFocus ==========================================================

  @Benchmark def eoTransformFocus: List[ChPrice] = eoTransformFocusFn(raws)
  @Benchmark def chimneyTransformFocus: List[ChPrice] = pricesT.transform(raws)
  @Benchmark def handTransformFocus: List[ChPrice] = raws.map(r => ChPrice(r.cents))

  // ==== the quartet: transformFocusPartial ===================================================

  @Benchmark def eoTransformFocusPartial: partial.Result[List[ChPrice]] =
    eoTransformFocusPartialFn(raws)

  @Benchmark def chimneyTransformFocusPartial: partial.Result[List[ChPrice]] =
    pricesPT.transform(raws)

  @Benchmark def handTransformFocusPartial: Either[List[String], List[ChPrice]] =
    handPrices(raws, Nil, Nil)

  // ==== the quartet: transformImage ==========================================================

  @Benchmark def eoTransformImage: ChUserView = nameToView.modify(_.toUpperCase)(user)

  @Benchmark def chimneyTransformImage: ChUserView =
    user.into[ChUserView].withFieldComputed(_.name, _.name.toUpperCase).transform

  @Benchmark def handTransformImage: ChUserView =
    ChUserView(user.name.toUpperCase, ChAddressView(user.address.city, user.address.street))

  // ==== the quartet: transformImagePartial ===================================================

  @Benchmark def eoTransformImagePartial: partial.Result[ChAccount] =
    balanceToAccount.modify(_ + 1)(dto)

  @Benchmark def chimneyTransformImagePartial: partial.Result[ChAccount] =
    accountPT.transform(dto.copy(balance = dto.balance + 1))

  @Benchmark def handTransformImagePartial: Option[ChAccount] =
    ChCents.from(dto.balance + 1).map(ChAccount(dto.owner, _))

  // ==== convert, then read a field (Chimney.getter) ==========================================

  @Benchmark def eoGetterRead: String = viewCity.get(user)
  @Benchmark def chimneyGetterRead: String = viewT.transform(user).address.city
  @Benchmark def handGetterRead: String = user.address.city

  // ==== edit a domain field on the DTO (Codec → prism ∘ lens) ================================

  @Benchmark def eoCodecModify: ChAccountDto = dtoOwner.modify(_.toUpperCase)(dto)

  @Benchmark def chimneyCodecModify: ChAccountDto =
    accountPT.transform(dto).asOption match
      case Some(a) => accountT.transform(a.copy(owner = a.owner.toUpperCase))
      case None    => dto

  @Benchmark def handCodecModify: ChAccountDto =
    ChCents.from(dto.balance) match
      case Some(_) => dto.copy(owner = dto.owner.toUpperCase)
      case None    => dto

  // ==== decode, then read a field, keeping the errors (ResultFold) ===========================

  @Benchmark def eoResultFoldRead: Either[partial.Result.Errors, String] = decodedOwner.either(dto)

  @Benchmark def chimneyResultFoldRead: Either[partial.Result.Errors, String] =
    accountPT.transform(dto).map(_.owner).asEither

  @Benchmark def handResultFoldRead: Either[String, String] =
    ChCents.from(dto.balance).map(_ => dto.owner).toRight("negative amount")

  // ==== patch a nested part (optic.patch) ====================================================

  @Benchmark def eoPatch: ChOrder = customer.patch(contact)(using contactP)(order)

  @Benchmark def chimneyPatch: ChOrder =
    order.copy(customer = contactP.patch(order.customer, contact))

  @Benchmark def handPatch: ChOrder =
    order.copy(customer = order.customer.copy(email = contact.email, phone = contact.phone))

  // Hand-written error-accumulating traversal: every failure collected, like chimney's.
  @tailrec
  private def handPrices(
      rest: List[ChRawPrice],
      ok: List[ChPrice],
      errors: List[String],
  ): Either[List[String], List[ChPrice]] =
    rest match
      case r :: tail =>
        if r.cents >= 0 then handPrices(tail, ChPrice(r.cents) :: ok, errors)
        else handPrices(tail, ok, "negative price" :: errors)
      case Nil =>
        if errors.isEmpty then Right(ok.reverse) else Left(errors.reverse)
