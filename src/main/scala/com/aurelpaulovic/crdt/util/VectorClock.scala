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

package com.aurelpaulovic.crdt.util

import com.aurelpaulovic.crdt.replica.Replica
import scala.collection.immutable

class VectorClock private (private[this] val replica: Replica, private[this] val localValue: Int, private[this] val stateWithOutdatedLocal: immutable.Map[Replica, Int]) extends PartiallyOrdered[VectorClock] with Serializable {
  def this(replica: Replica) = this(replica, 0, immutable.Map.empty)
  
  protected[VectorClock] lazy val state: immutable.Map[Replica, Int] = stateWithOutdatedLocal + (replica -> localValue)
  
  override def equals(other: Any): Boolean = other match {
    case (that: VectorClock) => that.isInstanceOf[VectorClock] && state == that.state
    case _ => false
  }
  
  def tryCompareTo[B >: VectorClock <% PartiallyOrdered[B]](that: B): Option[Int] = that match {
    case (that: VectorClock) =>
      if 			(this < that) 	Some(-1)
      else if (this == that)	Some(0)
      else 										Some(1)
    case _ => None
  }
  
  def lteq(other: VectorClock): Boolean = this <= other
  
  def <=(other: VectorClock): Boolean = this < other || this == other
  
	def <(other: VectorClock): Boolean = state.forall { case (rep, value) => other.state.get(rep) match {
	  	case Some(otherValue) if value < otherValue => true
	  	case _ => false
		}
	}
	
	def >(other: VectorClock): Boolean = other.state.forall { case (rep, otherValue) => state.get(rep) match {
	  	case Some(value) if value > otherValue => true
	  	case _ => false
		}
	}
	
	def increment(): VectorClock = new VectorClock(replica, localValue + 1, stateWithOutdatedLocal)
	
	def +(other: VectorClock): VectorClock = { 
	  val mergedState = stateWithOutdatedLocal ++ (
        for {
			    pair @ (k, v) <- other.state
			    if stateWithOutdatedLocal.getOrElse(k, 0) <= v
			  } yield pair
	  )
	  
	  new VectorClock(replica, math.max(localValue, other.state.getOrElse(replica, 0)) + 1, mergedState)
	}
	
	override def toString(): String = s"VectorClock($state)"
}

object VectorClock {
  def apply(replica: Replica): VectorClock = new VectorClock(replica, 0, immutable.Map.empty)
}