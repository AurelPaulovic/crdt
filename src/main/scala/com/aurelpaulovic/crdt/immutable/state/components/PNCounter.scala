package com.aurelpaulovic.crdt.immutable.state.components

import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.immutable.state.JoinSemilattice

sealed trait PNCounter[T] extends JoinSemilattice[PNCounter[T]] {
  protected implicit val num: Numeric[T]
  import num._

  val replica: Replica

  def isEmpty(): Boolean

  def value(): T

  def increment(by: T): PNCounter[T]

  def increment(): PNCounter[T] = increment(num.one)

  def decrement(by: T): PNCounter[T]

  def decrement(): PNCounter[T] = decrement(num.one)

  protected[components] def payload: PNCounter.Payload[T]
}

object PNCounter {
  def apply[T: Numeric](replica: Replica): PNCounter[T] = EmptyPNCounter(replica)

  def apply[T](replica: Replica, value: T)(implicit num: Numeric[T]): PNCounter[T] = NonEmptyPNCounter(replica, value, num.zero, Payload.empty[T])

  protected[components] case class Payload[T](p: VectorCounter[T], n: VectorCounter[T]) extends JoinSemilattice[Payload[T]] {
    def dominates(other: Payload[T]): Boolean = (p dominates other.p) && (n dominates other.n)

    def isDominatedBy(other: Payload[T]): Boolean = (p isDominatedBy other.p) && (n isDominatedBy other.n)

    def isSameAs(other: Payload[T]): Boolean = (p isSameAs other.p) && (n isSameAs other.n)

    def join(other: Payload[T]): Payload[T] = Payload(p join other.p, n join other.n)
  }

  protected[components] object Payload {
    def empty[T: Numeric](): Payload[T] = Payload[T](VectorCounter.empty[T], VectorCounter.empty[T])
  }
}

final case class EmptyPNCounter[T](val replica: Replica)(implicit protected val num: Numeric[T]) extends PNCounter[T] {
  val value: T = num.zero

  def increment(by: T): PNCounter[T] = NonEmptyPNCounter(replica, by, num.zero, PNCounter.Payload.empty[T])

  def decrement(by: T): PNCounter[T] = NonEmptyPNCounter(replica, num.zero, by, PNCounter.Payload.empty[T])

  protected[components] lazy val payload: PNCounter.Payload[T] = PNCounter.Payload.empty[T]

  val isEmpty: Boolean = true

  def dominates(other: PNCounter[T]): Boolean = other.isEmpty

  def isDominatedBy(other: PNCounter[T]): Boolean = true

  def isSameAs(other: PNCounter[T]): Boolean = other.isEmpty

  def join(other: PNCounter[T]): PNCounter[T] = {
    if (other.isEmpty) this
    else NonEmptyPNCounter(replica, num.zero, num.zero, other.payload)
  }
}

case class NonEmptyPNCounter[T](val replica: Replica,
                                val localP: T,
                                val localN: T,
                                val outdatedPayload: PNCounter.Payload[T]
                               )(implicit protected val num: Numeric[T]) extends PNCounter[T] {
  import num._

  def value(): T = (outdatedPayload.p.value + localP) - (outdatedPayload.n.value + localN)

  val isEmpty: Boolean = false

  def increment(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, localP + by, localN, outdatedPayload)

  def decrement(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, localP, localN + by, outdatedPayload)

  protected[components] lazy val payload: PNCounter.Payload[T] = {
    if (localP == num.zero && localN == num.zero) outdatedPayload
    else PNCounter.Payload(outdatedPayload.p.increment(replica, localP), outdatedPayload.n.increment(replica, localN))
  }

  def dominates(other: PNCounter[T]): Boolean = payload dominates other.payload

  def isDominatedBy(other: PNCounter[T]): Boolean = payload isDominatedBy other.payload

  def isSameAs(other: PNCounter[T]): Boolean = payload isSameAs other.payload

  def join(other: PNCounter[T]): PNCounter[T] = new NonEmptyPNCounter(replica, num.zero, num.zero, payload /+\ other.payload)
}