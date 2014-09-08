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

package com.aurelpaulovic.crdt.immutable.state

import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.Id

class PNCounter[T] private (val id: Id, val replica: Replica, private val counter: components.PNCounter[T])(implicit num: Numeric[T]) extends CRDT[T, PNCounter[T]] {
  import num._

  def this(id: Id, replica: Replica)(implicit num: Numeric[T]) = this(id, replica, components.PNCounter[T](replica))

  def setToOne(): PNCounter[T] = setTo(num.one)

  def setToZero(): PNCounter[T] = setTo(num.zero)

  private def setTo(newValue: T): PNCounter[T] = {
    if (newValue < value) new PNCounter[T](id, replica, counter.decrement(value - newValue))
    else if (newValue == value) this
    else new PNCounter[T](id, replica, counter.increment(newValue - value))
  }

  lazy val value: T = counter.value

  def increment(): PNCounter[T] = increment(num.one)

  def decrement(): PNCounter[T] = decrement(num.one)

  def increment(by: T): PNCounter[T] = new PNCounter[T](id, replica, counter.increment(by))

  def decrement(by: T): PNCounter[T] = new PNCounter[T](id, replica, counter.decrement(by))

  def isZero(): Boolean = (value == num.zero)

  def isNegative(): Boolean = (value < num.zero)

  def leq(other: PNCounter[T]): Option[Boolean] = {
    if (other.id == id) Some(counter /<=\ other.counter)
    else None
  }

  def merge(other: PNCounter[T]): Option[PNCounter[T]] = {
    if (other.id == id) Some(new PNCounter[T](id, replica, counter /+\ other.counter))
    else None
  }

  override def toString(): String = s"PNCounter($id, $replica, $counter) with value $value"
}

object PNCounter {
  def apply[T](id: Id, replica: Replica)(implicit num: Numeric[T]): PNCounter[T] = new PNCounter[T](id, replica)

  def apply[T](id: Id, replica: Replica, value: T)(implicit num: Numeric[T]): PNCounter[T] = new PNCounter[T](id, replica).increment(value)
}
