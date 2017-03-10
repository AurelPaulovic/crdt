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
import com.aurelpaulovic.crdt.util.TotalTimeClock

import scala.reflect.runtime.universe._

class TotallyOrderedRegister[E, R <: Replica] private (val id: Id, private[this] val pReplica: R with Ordered[R], val value: E, protected val clock: TotalTimeClock[R])(implicit paramType: TypeTag[E]) extends CRDT[E, TotallyOrderedRegister[E,R]] {
  val replica: Replica = pReplica
  
  def assign(value: E): TotallyOrderedRegister[E, R] = new TotallyOrderedRegister(id, pReplica, value, TotalTimeClock.makeGreaterThan(pReplica, clock))
	
	def merge(other: TotallyOrderedRegister[E, R]): Option[TotallyOrderedRegister[E, R]] = other.id match {
	  case `id` => 
	  	if (clock >= other.clock) Some(this)
	  	else Some(new TotallyOrderedRegister(id, pReplica, other.value, other.clock))
	  case _ => None
	}
	
	def leq(other: TotallyOrderedRegister[E, R]): Option[Boolean] = other.id match {
	  case `id` => Some(clock <= other.clock)
	  case _ => None
	}
	
	protected def canRdtTypeEqual[OTHER: TypeTag](other: Any) = {
    typeOf[OTHER] == typeOf[E] && other.isInstanceOf[TotallyOrderedRegister[_,_]]
  }

  def rdtTypeEquals(other: Any) = other match {
    case that: TotallyOrderedRegister[E, R] => that.canRdtTypeEqual[E](this)
    case _ => false
  }
  
  def copyForReplica(newReplica: Replica): TotallyOrderedRegister[E, R] = // can throw cast exception
    new TotallyOrderedRegister(id, newReplica.asInstanceOf[R with Ordered[R]], value, clock.copyForReplica(newReplica.asInstanceOf[R with Ordered[R]]))
	
	override def toString = s"TotallyOrderedRegister($value)"
}

object TotallyOrderedRegister {
  def apply[R <: Replica, T](id: Id, replica: R with Ordered[R], value: T)(implicit paramType: TypeTag[T]): TotallyOrderedRegister[T, R] = new TotallyOrderedRegister(id, replica, value, TotalTimeClock(replica))
}
