package dev.constructive.eo

/** Recursion schemes as composable optics: `Schemes.cata` (fold), `Schemes.ana` (unfold) and
  * `Schemes.hylo` (refold) over any type with a `Plated` instance — stack-safe, expressed as eo
  * optics so they cross-compose with the rest of the library (`ana(…).cross(cata(…))` '''is'''
  * `hylo`).
  */
package object schemes
