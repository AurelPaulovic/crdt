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
import com.aurelpaulovic.crdt.util.TotalOrdering
import com.aurelpaulovic.crdt.util.TotalTimeClock

class TotallyOrderedRegister[T, R <: Replica] private (val id: Id, private[this] val replica: R with Ordered[R], val value: T, protected val clock: TotalTimeClock[R]) {
	def assign(value: T): TotallyOrderedRegister[T, R] = new TotallyOrderedRegister(id, replica, value, TotalTimeClock.makeGreaterThan(replica, clock))
	
	def merge(other: TotallyOrderedRegister[T, R]): Option[TotallyOrderedRegister[T, R]] = other.id match {
	  case `id` => 
	  	if (clock >= other.clock) Some(this)
	  	else Some(new TotallyOrderedRegister(id, replica, other.value, other.clock))
	  case _ => None
	}
	
	def leq(other: TotallyOrderedRegister[T, R]): Option[Boolean] = other.id match {
	  case `id` => Some(clock <= other.clock)
	  case _ => None
	}
	
	override def toString = s"TotallyOrderedRegister($value)"
}

object TotallyOrderedRegister {
  def apply[R <: Replica, T](id: Id, replica: R with Ordered[R], value: T): TotallyOrderedRegister[T, R] = new TotallyOrderedRegister(id, replica, value, TotalTimeClock(replica))
}