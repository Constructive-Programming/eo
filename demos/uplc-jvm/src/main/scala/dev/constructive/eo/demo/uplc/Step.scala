package dev.constructive.eo.demo.uplc

import cats.Eval

/** One step of a compiler walk — decoding, typing, lowering: each recurses as deep as the program
  * nests. A step is [[Step.Done]], or [[Step.Later]], on the heap.
  *
  * A walk makes plain calls while it is shallow: a done step's `flatMap` calls straight on. Once it
  * is [[Walk.Deep]] calls deep it defers ([[Walk.nested]]), and the rest runs from `cats.Eval`'s
  * loop, at the bottom of the JVM stack. So no program overflows the stack, and a shallow one pays
  * next to nothing for that.
  *
  * Every step runs once, and only after the steps before it — so a step may do its work as it is
  * made: the lowering writes instructions that way.
  */
enum Step[+A]:
  case Done(a: A)
  case Later(steps: Eval[A])

  def flatMap[B](f: A => Step[B]): Step[B] = this match
    case Done(a)  => f(a)
    case Later(e) => Later(e.flatMap(f(_).eval))

  def map[B](f: A => B): Step[B] = this match
    case Done(a)  => Done(f(a))
    case Later(e) => Later(e.map(f))

  def >>[B](next: => Step[B]): Step[B] = this match
    case Done(_)  => next
    case Later(e) => Later(e.flatMap(_ => next.eval))

  def eval: Eval[A] = this match
    case Done(a)  => Eval.now(a)
    case Later(e) => e

  def value: A = eval.value

object Step:

  val unit: Step[Unit] = Done(())

  def each[A](as: Iterable[A])(step: A => Step[Unit]): Step[Unit] =
    as.foldLeft(unit)((acc, a) => acc >> step(a))

  def all[A, B](as: List[A])(step: A => Step[B]): Step[List[B]] =
    as.foldLeft(Done(List.empty[B]): Step[List[B]])((acc, a) =>
      acc.flatMap(bs => step(a).map(_ :: bs))
    ).map(_.reverse)

/** How deep a walk is on the JVM stack: every recursive call of a walk goes through [[nested]]. */
final class Walk:

  private var depth = 0

  /** `step`, made now while the walk is shallow; once it is [[Walk.Deep]] calls deep, made later,
    * from the bottom of the stack.
    */
  def nested[A](step: => Step[A]): Step[A] =
    if depth >= Walk.Deep then Step.Later(Eval.defer(step.eval))
    else
      depth += 1
      val made = step
      depth -= 1
      made

object Walk:

  /** Nested calls a walk makes on the JVM stack before it defers: a few frames each, far from the
    * smallest stack a thread gets.
    */
  val Deep = 128
