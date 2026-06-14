package dev.constructive.eo
package schemes

import accessor.{Accessor, ReverseAccessor}

/** The recursion-scheme carrier. At the value level `Scheme[X, A] = A` (the focus); `X` is a
  * phantom at runtime but a *load-bearing type-level index* at the optic level — it is the thesis
  * of this module made into a type.
  *
  * ==Why a distinct carrier (not `Direct`)==
  *
  * The plain `Direct`-backed [[dev.constructive.eo.optics.Getter]] /
  * [[dev.constructive.eo.optics.Review]] collapse a scheme to an opaque closure (`S => A` /
  * `Seed => S`), throwing the algebra away. Once the `coalg`/`alg` are gone the
  * `project ∘ embed = id` cancellation that deforestation rides on is *invisible*, so
  * `ana.cross(cata)` over `Direct` can only materialise the whole `S` and then fold it. The scheme
  * citizens ([[Cata]], [[Ana]]) instead **carry their (co)algebra**, so the fused [[Ana.cross]] can
  * rebuild the one-pass machine — `hylo` with no intermediate `S`.
  *
  * ==The existential `X` is the index==
  *
  * `X` records what structure a scheme *retains* — the soundness condition for fusion:
  *
  *   - [[Cata]] : `X = Nothing` — a node-blind fold (`alg: F[A] => A`). It retains nothing of the
  *     source tree, so `ana.cross(cata)` is sound to **fuse** (deforest).
  *   - [[Ana]] : `X = S` — the unfold threads the built structure.
  *   - [[Histo]] : `X = Attr[F, A]` (cofree comonad), [[Futu]] : `X = Coattr[F, A]` (free monad) —
  *     the universal indices, refining the fold/unfold towers.
  *   - [[Meta]] : `X = A` — the metamorphism's neck; non-trivial precisely because a fold→unfold
  *     over *different* functors cannot deforest, the honest mirror of [[Hylo]]'s `X = Nothing`.
  *
  * Refining `X` upward trades allocation for capability; the `(co)free (co)monads` are the
  * universal such indices.
  *
  * Value-identical to `Direct`; kept separate so the scheme optics are honest citizens and so the
  * fused [[Ana.cross]] / [[Futu.cross]] members are reachable in overload resolution.
  */
opaque type Scheme[X, A] = A

object Scheme:

  inline def apply[X, A](a: A): Scheme[X, A] = a

  extension [X, A](s: Scheme[X, A]) inline def value: A = s

  /** Reads the focus out — powers `.get` on [[Cata]] / [[Hylo]]. */
  given Accessor[Scheme] with
    def get[X, A](fa: Scheme[X, A]): A = fa

  /** Wraps a focus in — powers `.reverseGet` on [[Ana]]. */
  given ReverseAccessor[Scheme] with
    def reverseGet[X, A](a: A): Scheme[X, A] = a
