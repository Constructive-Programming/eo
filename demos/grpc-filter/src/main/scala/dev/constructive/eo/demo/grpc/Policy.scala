package dev.constructive.eo.demo.grpc

import dev.constructive.eo.{getOption, CanGetOption}

/** Which issues to keep: urgent incidents on the services that matter, bugs whose top stack frame
  * is in a watched module, and popular requests from customers on the plans that matter. Anything
  * else, including a kind this policy doesn't know, is dropped.
  *
  * [[keep]] is written once, against capabilities: it asks for each leaf it reads and never says
  * where the leaf comes from. The same code runs on a message's bytes (with [[Wire]]'s optics) and
  * on a decoded `Detail` (with [[Model]]'s), which is how the tests check one against the other.
  */
final case class Policy(
    urgent: Set[Severity],
    tiers: Set[Tier],
    modules: Set[Module],
    plans: Set[Plan],
    minVotes: Votes,
):

  def keep[D](detail: D)(using
      CanGetOption[D, Kind],
      CanGetOption[D, Severity],
      CanGetOption[D, Tier],
      CanGetOption[D, Module],
      CanGetOption[D, Plan],
      CanGetOption[D, Votes],
  ): Boolean =
    detail.getOption[Kind] match
      case Some(Kind.Incident) =>
        detail.getOption[Severity].exists(urgent) && detail.getOption[Tier].exists(tiers)
      case Some(Kind.Bug)     => detail.getOption[Module].exists(modules)
      case Some(Kind.Request) =>
        detail.getOption[Plan].exists(plans) && detail
          .getOption[Votes]
          .exists(_.count >= minVotes.count)
      case None => false

object Policy:

  val default: Policy = Policy(
    urgent = Set(Severity.Sev1, Severity.Sev2),
    tiers = Set(Tier.Critical),
    modules = Set(Module("payments"), Module("ledger")),
    plans = Set(Plan.Enterprise),
    minVotes = Votes(100),
  )
