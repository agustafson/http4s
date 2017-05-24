package org.http4s

import cats._
import cats.arrow.Choice
import cats.data._
import cats.implicits._
import fs2._
import fs2.util.Suspendable

object Service {

  /**
    * Lifts a total function to a `Service`. The function is expected to handle
    * all requests it is given.  If `f` is a `PartialFunction`, use `apply`
    * instead.
    */
  def lift[F[_], A, B](f: A => F[B]): Service[F, A, B] =
    Kleisli(f)

  /** Lifts a partial function to an `Service`.  Responds with the
    * zero of [B] for any request where `pf` is not defined.
    */
  def apply[F[_], A, B: Monoid](pf: PartialFunction[A, F[B]])(implicit F: Applicative[F]): Service[F, A, B] =
    lift(req => pf.applyOrElse(req, Function.const(F.pure(Monoid[B].empty))))

  /**
    * Lifts a F into a [[Service]].
    *
    */
  def const[F[_], A, B](b: F[B]): Service[F, A, B] =
    lift(_ => b)

  /**
    *  Lifts a value into a [[Service]].
    *
    */
  def constVal[F[_], A, B](b: => B)(implicit F: Suspendable[F]): Service[F, A, B] =
    lift(_ => F.delay(b))

  /** Allows Service chainig through a `scalaz.Monoid` instance. */
  def withFallback[F[_], A, B](fallback: Service[F, A, B])(service: Service[F, A, B])(implicit M: Monoid[F[B]]): Service[F, A, B] =
    service |+| fallback

  /** A service that always returns the zero of B. */
  def empty[F[_]: Suspendable, A, B: Monoid]: Service[F, A, B] =
    constVal(Monoid[B].empty)
}
