package eo
package data

import scala.annotation.tailrec
import scala.compiletime.ops.int.*

type FixedTraversal_[C, A, B] = C match
  case 0    => A *: EmptyTuple
  case S[n] => B *: FixedTraversal_[n, A, B]

type FixedTraversal[C] = [A, B] =>> FixedTraversal_[C, A, B]
