package com.aurelpaulovic.crdt.immutable.state.component

import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.immutable.state.JoinSemilattice

sealed trait GCounter[T] extends JoinSemilattice[GCounter[T]] {
  protected implicit val num: Numeric[T]
  import num._

  val replica: Replica

  def isEmpty(): Boolean

  def value(): T

  def increment(by: T): GCounter[T]

  def increment(): GCounter[T] = increment(num.one)

  protected[component] def payload: VectorCounter[T]
}

object GCounter {
  def apply[T](replica: Replica)(implicit num: Numeric[T]): GCounter[T] = EmptyGCounter(replica)

  def apply[T: Numeric](replica: Replica, value: T): GCounter[T] = NonEmptyGCounter(replica, value, VectorCounter.empty)
}

final case class EmptyGCounter[T](val replica: Replica)(implicit protected val num: Numeric[T]) extends GCounter[T] {
  val value: T = num.zero

  def increment(by: T): GCounter[T] = NonEmptyGCounter(replica, by, VectorCounter.empty)

  protected[component] lazy val payload: VectorCounter[T] = VectorCounter.empty[T]

  val isEmpty: Boolean = true

  def dominates(other: GCounter[T]): Boolean = other.isEmpty

  def isDominatedBy(other: GCounter[T]): Boolean = true

  def isSameAs(other: GCounter[T]): Boolean = other.isEmpty

  def join(other: GCounter[T]): GCounter[T] = {
    if (other.isEmpty) this
    else NonEmptyGCounter(replica, num.zero, other.payload)
  }
}

case class NonEmptyGCounter[T](val replica: Replica,
                               val localValue: T,
                               val outdatedPayload: VectorCounter[T])(implicit protected val num: Numeric[T]) extends GCounter[T] {
  import num._

  protected[component] lazy val payload: VectorCounter[T] = {
    if (localValue == num.zero) outdatedPayload
    else outdatedPayload.increment(replica, localValue)
  }

  val isEmpty: Boolean = false

  lazy val value: T = outdatedPayload.value + localValue

  def increment(by: T): GCounter[T] = new NonEmptyGCounter(replica, localValue + by, outdatedPayload)

  def isDominatedBy(other: GCounter[T]): Boolean = payload isDominatedBy other.payload

  def dominates(other: GCounter[T]): Boolean = payload dominates other.payload

  def isSameAs(other: GCounter[T]): Boolean = payload isSameAs other.payload

  def join(other: GCounter[T]): GCounter[T] = new NonEmptyGCounter(replica, num.zero, payload /+\ other.payload)

  override def toString(): String = s"components.NonEmptyGCounter($replica, $payload) with value $value"
}