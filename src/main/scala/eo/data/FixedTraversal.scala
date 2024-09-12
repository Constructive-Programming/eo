package eo
package data

import scala.annotation.tailrec
import scala.compiletime.ops.int.*

type FixedTraversal_[N, A] = N match
  case 0    => EmptyTuple
  case S[n] => A *: FixedTraversal_[n, A]

type FixedTraversal[N] = [A, B] =>> FixedTraversal_[N, B]
