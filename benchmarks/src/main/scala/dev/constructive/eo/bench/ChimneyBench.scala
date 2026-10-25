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
  * The headline is the transform quartet — `transformFocus` / `transformFocusPartial` (optics that
  * write a different focus type: reprice and retype every focus of a 64-element list in one write,
  * and write a view through a derived lens) and `transformImage` / `transformImagePartial` (edit a
  * field, hand the result back as another type). The bridge scenarios after it (convert-then-read,
  * edit through a `Codec`, read with errors, patch a nested part) cover the rest of the module.
  *
  * The `…Failing` / `…Miss` rows run the same operations on inputs that fail (half the list
  * elements invalid; a DTO whose balance doesn't decode), so the cost of collecting chimney's
  * errors — or of passing a miss through — shows next to the success path.
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
  private val badDto = ChAccountDto("bob", -5L)

  private val rawsHalfBad: List[ChRawPrice] =
    List.tabulate(64)(i => ChRawPrice(if i % 2 == 0 then i.toLong * 100 else -1L))

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
  private given viewToAddressT: Transformer[ChAddressView, ChAddress] = Transformer.derive
  private val accountPT: PartialTransformer[ChAccountDto, ChAccount] = PartialTransformer.derive
  private val accountT: Transformer[ChAccount, ChAccountDto] = Transformer.derive
  private val contactP: Patcher[ChCustomer, ChContactPatch] = Patcher.derive

  // ---- eo-chimney optics, built once -------------------------------------------------------

  private val prices = Traversal.pEach[List, ChRawPrice, ChPrice]
  // transformFocus[ChRawPrice]: the traversal reads ChRawPrice and is written with ChRawPrice too,
  // which the transformer turns into the ChPrice the traversal stores — reprice and retype in one
  // write.
  private val repricePrices = prices.transformFocus[ChRawPrice]
  private val repricePricesPartial = prices.transformFocusPartial[ChRawPrice]
  private val reprice: ChRawPrice => ChRawPrice = r => ChRawPrice(r.cents * 2)
  private val addressAsView = lens[ChUser](_.address).transformFocus[ChAddressView]
  private val view = ChAddressView("Paris", "2 Rue")
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

  @Benchmark def eoTransformFocus: List[ChPrice] = repricePrices.modify(reprice)(raws)
  @Benchmark def chimneyTransformFocus: List[ChPrice] = pricesT.transform(raws.map(reprice))
  @Benchmark def handTransformFocus: List[ChPrice] = raws.map(r => ChPrice(r.cents * 2))

  // ==== transformFocus as a write: put a view where a derived lens expects the domain type ====

  @Benchmark def eoTransformFocusWrite: ChUser = addressAsView.replace(view)(user)

  @Benchmark def chimneyTransformFocusWrite: ChUser =
    user.copy(address = viewToAddressT.transform(view))

  @Benchmark def handTransformFocusWrite: ChUser =
    user.copy(address = ChAddress(view.street, view.city))

  // ==== the quartet: transformFocusPartial ===================================================

  @Benchmark def eoTransformFocusPartial: partial.Result[List[ChPrice]] =
    repricePricesPartial.modify(reprice)(raws)

  @Benchmark def chimneyTransformFocusPartial: partial.Result[List[ChPrice]] =
    pricesPT.transform(raws.map(reprice))

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

  // Hand-written conversion too: build the view, then read it — the same work as the other two.
  @Benchmark def handGetterRead: String =
    ChUserView(user.name, ChAddressView(user.address.city, user.address.street)).address.city

  // ==== edit a domain field on the DTO (Codec → prism ∘ lens) ================================

  @Benchmark def eoCodecModify: ChAccountDto = dtoOwner.modify(_.toUpperCase)(dto)

  @Benchmark def chimneyCodecModify: ChAccountDto =
    accountPT.transform(dto).asOption match
      case Some(a) => accountT.transform(a.copy(owner = a.owner.toUpperCase))
      case None    => dto

  // Decode to the domain, edit there, encode back — what the codec round trip does.
  @Benchmark def handCodecModify: ChAccountDto = handEditOwner(dto)

  // ==== decode, then read a field, keeping the errors (ResultFold) ===========================

  @Benchmark def eoResultFoldRead: Either[partial.Result.Errors, String] = decodedOwner.either(dto)

  @Benchmark def chimneyResultFoldRead: Either[partial.Result.Errors, String] =
    accountPT.transform(dto).map(_.owner).asEither

  @Benchmark def handResultFoldRead: Either[String, String] = handDecode(dto).map(_.owner)

  // ==== patch a nested part (optic.patch) ====================================================

  @Benchmark def eoPatch: ChOrder = customer.patch(contact)(using contactP)(order)

  @Benchmark def chimneyPatch: ChOrder =
    order.copy(customer = contactP.patch(order.customer, contact))

  @Benchmark def handPatch: ChOrder =
    order.copy(customer = order.customer.copy(email = contact.email, phone = contact.phone))

  // ==== failure paths ========================================================================

  @Benchmark def eoTransformFocusPartialFailing: partial.Result[List[ChPrice]] =
    repricePricesPartial.modify(reprice)(rawsHalfBad)

  @Benchmark def chimneyTransformFocusPartialFailing: partial.Result[List[ChPrice]] =
    pricesPT.transform(rawsHalfBad.map(reprice))

  @Benchmark def handTransformFocusPartialFailing: Either[List[String], List[ChPrice]] =
    handPrices(rawsHalfBad, Nil, Nil)

  @Benchmark def eoTransformImagePartialFailing: partial.Result[ChAccount] =
    balanceToAccount.modify(_ - 200)(dto)

  @Benchmark def chimneyTransformImagePartialFailing: partial.Result[ChAccount] =
    accountPT.transform(dto.copy(balance = dto.balance - 200))

  @Benchmark def handTransformImagePartialFailing: Option[ChAccount] =
    ChCents.from(dto.balance - 200).map(ChAccount(dto.owner, _))

  @Benchmark def eoCodecModifyMiss: ChAccountDto = dtoOwner.modify(_.toUpperCase)(badDto)

  @Benchmark def chimneyCodecModifyMiss: ChAccountDto =
    accountPT.transform(badDto).asOption match
      case Some(a) => accountT.transform(a.copy(owner = a.owner.toUpperCase))
      case None    => badDto

  @Benchmark def handCodecModifyMiss: ChAccountDto = handEditOwner(badDto)

  @Benchmark def eoResultFoldReadMiss: Either[partial.Result.Errors, String] =
    decodedOwner.either(badDto)

  @Benchmark def chimneyResultFoldReadMiss: Either[partial.Result.Errors, String] =
    accountPT.transform(badDto).map(_.owner).asEither

  @Benchmark def handResultFoldReadMiss: Either[String, String] = handDecode(badDto).map(_.owner)

  // Hand-written codec: decode the DTO into the domain (failing on a negative balance), encode it
  // back. The hand rows run the same conversions as the chimney and eo rows.
  private def handDecode(d: ChAccountDto): Either[String, ChAccount] =
    ChCents.from(d.balance).map(ChAccount(d.owner, _)).toRight("negative amount")

  private def handEditOwner(d: ChAccountDto): ChAccountDto =
    handDecode(d) match
      case Right(a) => ChAccountDto(a.owner.toUpperCase, a.balance.value)
      case Left(_)  => d

  // Hand-written error-accumulating traversal (repricing ×2 as it goes): every failure collected,
  // like chimney's.
  @tailrec
  private def handPrices(
      rest: List[ChRawPrice],
      ok: List[ChPrice],
      errors: List[String],
  ): Either[List[String], List[ChPrice]] =
    rest match
      case r :: tail =>
        if r.cents >= 0 then handPrices(tail, ChPrice(r.cents * 2) :: ok, errors)
        else handPrices(tail, ok, "negative price" :: errors)
      case Nil =>
        if errors.isEmpty then Right(ok.reverse) else Left(errors.reverse)
