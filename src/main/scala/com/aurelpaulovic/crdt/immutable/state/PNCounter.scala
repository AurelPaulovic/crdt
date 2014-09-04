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

class PNCounter[T] private (val id: Id, private[this] val replica: Replica, private[this] val pcount: GCounter[T], private[this] val ncount: GCounter[T])(implicit num: Numeric[T]) {
  import num._ 
  
  lazy val state: PNCounter.PNCounterState[T] = new PNCounter.PNCounterState(pcount, ncount)
  
  def setToOne(): PNCounter[T] = setTo(num.one)
  
  def setToZero(): PNCounter[T] = setTo(num.zero)
  
  private def setTo(newVal: T): PNCounter[T] = {
    if (newVal < value) new PNCounter(id, replica, pcount, ncount.incrementBy(value - newVal))
    else if (newVal == value) this
    else new PNCounter(id, replica, pcount.incrementBy(newVal - value) , ncount)
  }
  
  def increment(): PNCounter[T] = new PNCounter(id, replica, pcount.increment, ncount)
  
  def decrement(): PNCounter[T] = new PNCounter(id, replica, pcount, ncount.increment)
  
  def value(): T = pcount.value - ncount.value
  
  def isZero(): Boolean = ( value == num.zero )
  
  def isNegative(): Boolean = ( value < num.zero )
  
  def leq(other: PNCounter[T]): Option[Boolean] = other.id match {
    case `id` => Some(leq(other.state))
    case _ => None
  }
  
  def leq(other: PNCounter.PNCounterState[T]): Boolean = pcount.leq(other.pcount).get && ncount.leq(other.ncount).get
  
  def merge(other: PNCounter[T]): Option[PNCounter[T]] = other.id match {
    case `id` => Some(merge(other.state))
    case _ => None
  }
  
  def merge(other: PNCounter.PNCounterState[T]): PNCounter[T] = new PNCounter(id, replica, pcount.merge(other.pcount).get, ncount.merge(other.ncount).get)
  
  override def toString(): String = s"PNCounter($id, $replica) with value $value"
}

object PNCounter {
  def apply[T](id: Id, replica: Replica)(implicit num: Numeric[T]): PNCounter[T] = new PNCounter[T](id, replica, GCounter(id, replica), GCounter(id, replica, num.zero))
	
  def apply[T](id: Id, replica: Replica, value: T)(implicit num: Numeric[T]): PNCounter[T] = new PNCounter[T](id, replica, GCounter(id, replica, value), GCounter(id, replica, num.zero))
  
  def apply[T](id: Id, replica: Replica, initState: PNCounterState[T])(implicit num: Numeric[T]): PNCounter[T] = new PNCounter(id, replica, initState.pcount, initState.ncount)
  
  protected[PNCounter] case class PNCounterState[T: Numeric](protected[PNCounter] val pcount: GCounter[T], protected[PNCounter] val ncount: GCounter[T])
}
