package dev.constructive.eo

/** Gate typeclasses keyed by carrier: [[Accessor]] (total read), [[PartialAccessor]] (read that may
  * miss), [[ReverseAccessor]] (build back from the write side). Signatures that take an
  * `Optic[…, F]` plus one of these on `F` must list the optic '''first''' in the same using clause
  * — left-to-right resolution pins `F` before the gate is searched.
  */
package object accessor
