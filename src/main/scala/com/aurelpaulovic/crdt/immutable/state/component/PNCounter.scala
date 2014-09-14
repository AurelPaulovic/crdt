/*
 * Copyright 2014 Aurel Paulovic (aurel.paulovic@gmail.com) (aurelpaulovic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.aurelpaulovic.crdt.immutable.state.component

import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.immutable.state.lattice.JoinSemilattice

sealed trait PNCounter[T] extends JoinSemilattice[PNCounter[T]] {
  protected implicit val num: Numeric[T]
  import num._
  
  val zero: T = num.zero

  val replica: Replica

  def isEmpty(): Boolean

  def value(): T
  
  def isZero(): Boolean
  
  def isOne(): Boolean
  
  def isNegative(): Boolean

  def increment(by: T): PNCounter[T]

  def increment(): PNCounter[T] = increment(num.one)

  def decrement(by: T): PNCounter[T]

  def decrement(): PNCounter[T] = decrement(num.one)
  
  def setToOne(): PNCounter[T] = setTo(num.one)

  def setToZero(): PNCounter[T] = setTo(num.zero)
  
  def setTo(newValue: T): PNCounter[T]

  protected[component] def payload: PNCounter.Payload[T]
}

object PNCounter {
  def apply[T: Numeric](replica: Replica): PNCounter[T] = EmptyPNCounter(replica)

  def apply[T](replica: Replica, value: T)(implicit num: Numeric[T]): PNCounter[T] = NonEmptyPNCounter(replica, value, num.zero, Payload.empty[T])

  protected[component] case class Payload[T](p: VectorCounter[T], n: VectorCounter[T]) extends JoinSemilattice[Payload[T]] {
    def dominates(other: Payload[T]): Boolean = (p dominates other.p) && (n dominates other.n)

    def isDominatedBy(other: Payload[T]): Boolean = (p isDominatedBy other.p) && (n isDominatedBy other.n)

    def isSameAs(other: Payload[T]): Boolean = (p isSameAs other.p) && (n isSameAs other.n)

    def join(other: Payload[T]): Payload[T] = Payload(p join other.p, n join other.n)
  }

  protected[component] object Payload {
    def empty[T: Numeric](): Payload[T] = Payload[T](VectorCounter.empty[T], VectorCounter.empty[T])
  }
}

final case class EmptyPNCounter[T](val replica: Replica)(implicit protected val num: Numeric[T]) extends PNCounter[T] {
  val value: T = num.zero
  
  val isZero: Boolean = true
  
  val isNegative: Boolean = false
  
  val isOne: Boolean = false
  
  def setTo(newValue: T): PNCounter[T] = {
    if      (newValue == num.zero)        this
    else if (num.gt(newValue, num.zero))  new NonEmptyPNCounter(replica, newValue, num.zero, PNCounter.Payload.empty[T])
    else                                  new NonEmptyPNCounter(replica, num.zero, newValue, PNCounter.Payload.empty[T])
  }
  
  def increment(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, by, num.zero, PNCounter.Payload.empty[T])

  def decrement(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, num.zero, by, PNCounter.Payload.empty[T])

  protected[component] lazy val payload: PNCounter.Payload[T] = PNCounter.Payload.empty[T]

  val isEmpty: Boolean = true

  def dominates(other: PNCounter[T]): Boolean = other.isEmpty

  def isDominatedBy(other: PNCounter[T]): Boolean = true

  def isSameAs(other: PNCounter[T]): Boolean = other.isEmpty

  def join(other: PNCounter[T]): PNCounter[T] = {
    if (other.isEmpty) this
    else new NonEmptyPNCounter(replica, num.zero, num.zero, other.payload)
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
  
  def isZero(): Boolean = value == num.zero
  
  def isNegative(): Boolean = value < num.zero
  
  def isOne(): Boolean = value == num.one
  
  def setTo(newValue: T): PNCounter[T] = {
    if      (newValue == value)       this
    else if (num.gt(newValue, value)) increment(newValue - value)
    else                              decrement(value - newValue)
  }

  def increment(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, localP + by, localN, outdatedPayload)

  def decrement(by: T): PNCounter[T] = new NonEmptyPNCounter(replica, localP, localN + by, outdatedPayload)

  protected[component] lazy val payload: PNCounter.Payload[T] = {
    if (localP == num.zero && localN == num.zero) outdatedPayload
    else PNCounter.Payload(outdatedPayload.p.increment(replica, localP), outdatedPayload.n.increment(replica, localN))
  }

  def dominates(other: PNCounter[T]): Boolean = payload dominates other.payload

  def isDominatedBy(other: PNCounter[T]): Boolean = payload isDominatedBy other.payload

  def isSameAs(other: PNCounter[T]): Boolean = payload isSameAs other.payload

  def join(other: PNCounter[T]): PNCounter[T] = new NonEmptyPNCounter(replica, num.zero, num.zero, payload /+\ other.payload)
}