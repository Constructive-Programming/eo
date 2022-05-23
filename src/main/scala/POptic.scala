package eo

import cats.arrow._

trait ProYo[P[_, _], A, B](using P: Profunctor[P]) {
  def to[X, Y](build: (X => A, B => Y)): P[X, Y]
}

trait POptic[P[_, _]](using P: Profunctor[P]) {
  def optic[S, T, A, B]: P[A, B] => P[S, T]
}

object POptic {
  def lens[P[_, _]](using P: Strong[P]): POptic[P] = new POptic[P] {
    def optic[S, T, A, B]: P[A, B] => P[S, T] = ???
  }

}
