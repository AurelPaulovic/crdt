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
import scala.collection.immutable
import com.aurelpaulovic.crdt.Id

class GCounter[T] private (val id: Id, private[this] val replica: Replica, private[this] val localValue: T, private[this] val payloadWithOutdatedLocal: immutable.Map[Replica, T])(implicit num: Numeric[T]) {
  import num._
  
  /*
   * The counter efficiently does not update the payload (payloadWithOutdatedLocal) map when incremented or merged and makes
   * use of the localValue. The outdated payload is, however, never leaded.
   * 
   * The only way to get the inner state of the counter is using the state or value fields, which 
   * both fix the local replica value in the state to have value equal to localValue.
   */
  
  lazy val state: GCounter.GCounterState[T] = new GCounter.GCounterState(payloadWithOutdatedLocal + (replica -> localValue))

  def increment(): GCounter[T] = new GCounter(id, replica, localValue + num.one, payloadWithOutdatedLocal)
    
  lazy val value: T = state.payload.foldLeft(num.zero)(_ + _._2)
  
  def leq(other: GCounter[T]): Option[Boolean] = other.id match {
    case `id` => Some(leq(other.state))
    case _ => None
  }

  def leq(other: GCounter.GCounterState[T]): Boolean = state.payload.forall {
    case (k, v) => other.payload.get(k) match {
      case Some(v2) => v <= v2
      case _ => false
    }
  }
  
  def merge(other: GCounter[T]): Option[GCounter[T]] = other.id match {
    case `id` => Some(merge(other.state))
    case _ => None 
  }
  
  def merge(other: GCounter.GCounterState[T]): GCounter[T] = {
    val mergedPayload = payloadWithOutdatedLocal ++ (
        for {
			    pair @ (k, v) <- other.payload
			    if payloadWithOutdatedLocal.getOrElse(k, num.zero) <= v
			  } yield pair
	  )
	  
	  new GCounter(id, replica, num.max(localValue, other.payload.getOrElse(replica, num.zero)), mergedPayload)
  }
  
  override def toString(): String = s"GCounter($id, $replica) with value $value"
}

object GCounter {
	def apply[T](id: Id, replica: Replica)(implicit num: Numeric[T]): GCounter[T] = new GCounter[T](id, replica, num.zero, immutable.Map.empty)
	
  def apply[T](id: Id, replica: Replica, value: T)(implicit num: Numeric[T]): GCounter[T] = new GCounter[T](id, replica, value, immutable.Map.empty)
  
  def apply[T](id: Id, replica: Replica, initState: GCounterState[T])(implicit num: Numeric[T]): GCounter[T] = new GCounter(id, replica, initState.payload.getOrElse(replica, num.zero), initState.payload)
  
  protected[GCounter] case class GCounterState[T: Numeric](protected[GCounter] val payload: immutable.Map[Replica, T])
}