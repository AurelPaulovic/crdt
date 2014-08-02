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

import com.aurelpaulovic.crdt.Id
import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.util.VectorClock
import com.aurelpaulovic.crdt.util.Mergeable

class MergeableRegister[T: Mergeable] private (val id: Id, private[this] val replica: Replica, val value: T, protected val clock: VectorClock) {
  import Mergeable._
  
	def assign(value: T): MergeableRegister[T] = new MergeableRegister(id, replica, value, clock.increment)
  
	def merge(other: MergeableRegister[T]): Option[MergeableRegister[T]] = other.id match {
	  case `id` => 
	    if 			(other.clock > clock)		Some(other) // this register has older value, the other register has seen our value
		  else if (other.clock <= clock)	Some(this)	// this register is newer and has seen the other register's value or the registers are equal
		  else {
		    // the registers are concurrent -> merge them
		  	Some(new MergeableRegister(id, replica, value merge other.value, clock + other.clock)) 
		  }
	  case _ => None
	}
  
	def leq(other: MergeableRegister[T]): Option[Boolean] = other.id match {
	  case `id` => Some(clock <= other.clock)
	  case _ => None
	}
	
	override def toString(): String = s"MergeableRegister($value)"
}

object MergeableRegister {
  def apply[T: Mergeable](id: Id, replica: Replica, value: T): MergeableRegister[T] = new MergeableRegister(id, replica, value, VectorClock(replica))
}