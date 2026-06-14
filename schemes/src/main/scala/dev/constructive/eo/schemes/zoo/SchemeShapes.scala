package dev.constructive.eo
package schemes
package zoo

import data.Direct
import optics.Optic

/** The two carrier-wearing shapes every scheme citizen takes, factored so the
  * [[dev.constructive.eo.data.Direct]] wrapping lives in **one** place instead of being respelled in
  * every citizen — a carrier change touches these two classes, not all fourteen (the cost the
  * `Scheme`→`Direct` migration paid by hand).
  *
  * The `read`/`write` member is virtual (one dispatch per fold), which is immaterial here: a scheme's
  * `.get`/`.reverseGet` is called once per O(n) fold, so the indirection core `Getter`/`Review`
  * avoid for *hot composed* reads (their ~1.8× megamorphic-dispatch finding) does not apply. Each
  * subclass supplies the function and pins its existential `type X` (the recursion index).
  */

/** Read-direction scheme — a `Getter`-shaped optic over `Direct` reading `S => A` (`.get`). */
abstract class ReadScheme[S, A] extends Optic[S, Unit, A, Unit, Direct]:
  protected def read(s: S): A
  final def to(s: S): Direct[X, A] = Direct[X, A](read(s))
  final def from(b: Direct[X, Unit]): Unit = ()

/** Build-direction scheme — a `Review`-shaped optic over `Direct` building `B => T` (`.reverseGet`). */
abstract class BuildScheme[T, B] extends Optic[Unit, T, Unit, B, Direct]:
  protected def write(b: B): T
  final def to(u: Unit): Direct[X, Unit] = Direct[X, Unit](())
  final def from(d: Direct[X, B]): T = write(Direct.value(d))
