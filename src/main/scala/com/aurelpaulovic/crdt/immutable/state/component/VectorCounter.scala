/*
 * Copyright 2014 Aurel Paulovic (aurel.paulovic@gmail.com) (aurelpaulovic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 			http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.aurelpaulovic.crdt.immutable.state.component

import scala.collection.immutable
import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.replica.NamedReplica
import com.aurelpaulovic.crdt.immutable.state.lattice.Lattice

sealed trait VectorCounter[T] extends Lattice[VectorCounter[T]] {
  protected implicit val num: Numeric[T]
  import num._

  def value(): T

  def isZero: Boolean

  def isEmpty: Boolean

  def payload: immutable.Map[Replica, T]

  def increment(replica: Replica): VectorCounter[T] = increment(replica, num.one)

  def increment(replica: Replica, by: T): VectorCounter[T]
}

object VectorCounter {
  def apply[T: Numeric](): VectorCounter[T] = empty[T]

  def empty[T: Numeric]: VectorCounter[T] = EmptyVectorCounter[T]()
}

final case class EmptyVectorCounter[T]()(implicit protected val num: Numeric[T]) extends VectorCounter[T] {
  import num._

  val value: T = num.zero

  val isZero: Boolean = true

  val isEmpty: Boolean = true

  val payload: immutable.Map[Replica, T] = immutable.Map.empty

  def increment(replica: Replica, by: T): VectorCounter[T] = {
    if      (by == num.zero) this
    else if (by > num.zero)  NonEmptyVectorCounter(immutable.Map(replica -> by))
    else throw new IllegalArgumentException(s"incremented value must not be negative, $by given")
  }

  def isDominatedBy(other: VectorCounter[T]): Boolean = true

  def dominates(other: VectorCounter[T]): Boolean = other.isEmpty

  def join(other: VectorCounter[T]): VectorCounter[T] = other

  def isSameAs(other: VectorCounter[T]): Boolean = other.isEmpty

  def meet(other: VectorCounter[T]): VectorCounter[T] = this
}

case class NonEmptyVectorCounter[T](val payload: immutable.Map[Replica, T])(implicit protected val num: Numeric[T]) extends VectorCounter[T] {
  import num._

  lazy val value = payload.foldLeft(num.zero)(_ + _._2)

  val isZero: Boolean = false

  val isEmpty: Boolean = false

  def increment(replica: Replica, by: T): VectorCounter[T] = {
    if      (by == num.zero) this
    else if (by > num.zero)  NonEmptyVectorCounter(payload.updated(replica, payload.getOrElse(replica, num.zero) + by))
    else throw new IllegalArgumentException(s"incremented value must not be negative, $by given")
  }

  def isDominatedBy(other: VectorCounter[T]): Boolean = payload forall {
    case (rep, count) => other.payload.get(rep) match {
      case Some(otherCount) if count <= otherCount => true
      case _ => false
    }
  }

  def dominates(other: VectorCounter[T]): Boolean = other.payload forall {
    case (rep, otherCount) => payload.get(rep) match {
      case Some(count) if count >= otherCount => true
      case _ => false
    }
  }

  def isSameAs(other: VectorCounter[T]): Boolean = payload.equals(other.payload) // structural compare

  def join(other: VectorCounter[T]): VectorCounter[T] = {
    val mergedPayload = payload ++ (
      for {
        pair @ (rep, count) <- other.payload
        if payload.getOrElse(rep, num.negate(num.one)) < count
      } yield pair)

    NonEmptyVectorCounter(mergedPayload)
  }

  def meet(other: VectorCounter[T]): VectorCounter[T] = {
    val intersectPayload = payload collect { case (rep, count) if other.payload.getOrElse(rep, num.zero) > num.zero => rep -> num.min(count, other.payload(rep)) }

    if (intersectPayload.isEmpty) VectorCounter.empty
    else NonEmptyVectorCounter(intersectPayload)
  }
}