package dev.constructive.eo.demo.grpc

import dev.constructive.eo.CanGetOption

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
      kind: CanGetOption[D, Kind],
      severity: CanGetOption[D, Severity],
      tier: CanGetOption[D, Tier],
      module: CanGetOption[D, Module],
      plan: CanGetOption[D, Plan],
      votes: CanGetOption[D, Votes],
  ): Boolean =
    kind.getOption(detail) match
      case Some(Kind.Incident) =>
        severity.getOption(detail).exists(urgent) && tier.getOption(detail).exists(tiers)
      case Some(Kind.Bug)     => module.getOption(detail).exists(modules)
      case Some(Kind.Request) =>
        plan.getOption(detail).exists(plans) && votes
          .getOption(detail)
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
