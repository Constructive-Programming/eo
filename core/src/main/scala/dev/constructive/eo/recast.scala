package dev.constructive.eo

/** Zero-alloc phantom recasts for `Either`, the analogue of `data.Affine.Miss.widenB`: a `Left`
  * never stores its right type parameter and a `Right` never stores its left, so re-typing the
  * phantom side is a cast, not a rebuild. Composition kernels and carrier bridges use these to pass
  * miss / hit wrappers through unchanged instead of re-allocating `Left(t)` / `Right(c)` at every
  * hop. (The stdlib's `withRight` / `withLeft` are upcasts only — they can't re-type the phantom
  * side to an unrelated type.)
  */
extension [L, R](l: Left[L, R])
  private[eo] inline def widenRight[R2]: Either[L, R2] = l.asInstanceOf[Either[L, R2]]

extension [L, R](r: Right[L, R])
  private[eo] inline def widenLeft[L2]: Either[L2, R] = r.asInstanceOf[Either[L2, R]]
