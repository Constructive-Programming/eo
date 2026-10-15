package dev.constructive.eo.demo.grpc

import dev.constructive.eo.generics.{lens, prism}
import dev.constructive.eo.optics.{AffineFold, Optional, PickFold}

/** The same foci as [[Wire]]'s, on a decoded `Detail`: what the policy reads when the whole message
  * has been decoded, as the baseline does. Built from eo-generics' derived prisms (one per case)
  * and lenses (one per field).
  */
object Model:

  given kind: PickFold[Detail, Kind] = AffineFold(detail => Some(Kind.of(detail)))

  given severity: Focus[Detail, Severity] =
    prism[Detail, Detail.Incident].andThen(lens[Detail.Incident](_.severity))

  given tier: Focus[Detail, Tier] =
    prism[Detail, Detail.Incident]
      .andThen(lens[Detail.Incident](_.impact))
      .andThen(lens[Impact](_.service))
      .andThen(lens[Service](_.tier))

  given module: Focus[Detail, Module] =
    prism[Detail, Detail.Bug]
      .andThen(lens[Detail.Bug](_.trace))
      .andThen(lens[Trace](_.frames))
      .andThen(head[Frame])
      .andThen(lens[Frame](_.module))

  given plan: Focus[Detail, Plan] =
    prism[Detail, Detail.Request]
      .andThen(lens[Detail.Request](_.requester))
      .andThen(lens[Customer](_.contract))
      .andThen(lens[Contract](_.plan))

  given votes: Focus[Detail, Votes] =
    prism[Detail, Detail.Request].andThen(lens[Detail.Request](_.votes))

  /** A list's first element, if it has one. */
  private def head[A]: Optional[List[A], List[A], A, A] =
    Optional(
      as => as.headOption.toRight(as),
      (as, a) => a :: as.drop(1),
    )
