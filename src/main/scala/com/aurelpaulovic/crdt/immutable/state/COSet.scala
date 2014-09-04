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
import scala.collection.immutable

class COSet [T] private (val id: Id, val replica: Replica, private val clock: GCounter[Long], private val elements: Map[T, GCounter[Long]]) {
	def add(ele: T): COSet[T] = {
	  if (elements.contains(ele)) this
	  else new COSet[T](id, replica, clock.increment, elements + (ele -> clock.increment))
	}
	
	def remove(ele: T): COSet[T] = {
	  if (elements.contains(ele)) new COSet[T](id, replica, clock.increment, elements - ele) 
	  else this
	}
	
	def contains(ele: T): Boolean = elements.contains(ele)
	
	def isEmpty(): Boolean = elements.isEmpty
	
	private lazy val sizeCache: Int = elements.size
	
	def size(): Int = sizeCache
	
	def merge(other: COSet[T]): Option[COSet[T]] = (clock leq other.clock, other.clock leq clock) match {
	  case (Some(true), Some(true)) => Some(this) // this == other
	  case (Some(true), Some(false)) => Some(new COSet[T](id, replica, (clock merge other.clock).get, other.elements)) // this < other
	  case (Some(false), Some(true)) => Some(this) // this > other
	  case (Some(false), Some(false)) =>
	    //1. take elements present in both sets and merge their tags
	    val sharedElements = elements.collect{ case (ele, tag) if other.elements.contains(ele) => (ele -> (tag merge other.elements(ele)).get) }
	    
	    //2. take elements that present only in this and are concurrent with current clock in other
	    val newThisElements = (elements -- other.elements.keys).filter{ case (_, tag) => concurrentClocks(tag, other.clock) }
	    
	    //3. take elements that present only in other and are concurrent with current clock in this
	    val newOtherElements = (other.elements -- elements.keys).filter{ case (_, tag) => concurrentClocks(tag, clock) }
	     
	    Some(new COSet[T](id, replica, (clock merge other.clock).get, sharedElements ++ newThisElements ++ newOtherElements))// concurrent
	  case (_, _) => None
	}
	
	private def concurrentClocks(c1: GCounter[Long], c2: GCounter[Long]): Boolean = !((c1 leq c2).get || (c2 leq c1).get)
	
	def leq(other: COSet[T]): Option[Boolean] = clock leq other.clock
	
	override def toString(): String = s"COSet($id, $replica) with elements ${elements.keys}" 
}

object COSet {
  def apply[T](id: Id, replica: Replica): COSet[T] = new COSet[T](id, replica, GCounter[Long](id, replica), immutable.Map.empty)
  
  def apply[T](id: Id, replica: Replica, elements: T*): COSet[T] = {
    val elementPairs = for {
      ele <- elements
    } yield (ele, GCounter[Long](id, replica, 1))
    
    new COSet[T](id, replica,  GCounter[Long](id, replica).increment(elementPairs.size.toLong), elementPairs.toMap)
  }
}