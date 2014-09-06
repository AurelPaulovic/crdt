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

import scala.collection.immutable
import com.aurelpaulovic.crdt.replica.Replica
import com.aurelpaulovic.crdt.Id

class MPNSet[T] private (val id: Id, private[this] val replica: Replica, private val clock: GCounter[Long], private val elements: immutable.Map[T, PNCounter[Int]]) {
	def add(ele: T): MPNSet[T] = elements.get(ele) match {
	  case None => new MPNSet(id, replica, clock.increment, elements + newElement(ele))
	  case Some(counter) if elementExists(counter) => this
	  case Some(counter) => new MPNSet(id, replica, clock.increment, elements.updated(ele, counter.setToOne))
	}
	
	private def newElement(ele: T): (T, PNCounter[Int]) = (ele, PNCounter(id, replica, 1))
	
	def remove(ele: T): MPNSet[T] = elements.get(ele) match {
	  case None => this
	  case Some(counter) if !elementExists(counter) => this
	  case Some(counter) => new MPNSet(id, replica, clock.increment, elements.updated(ele, counter.setToZero))
	}
	
	def contains(ele: T): Boolean = elements.get(ele) match {
		case Some(counter) => elementExists(counter)
	  case None => false
	}
	
	def isEmpty(): Boolean = elements.forall{ case (ele, counter) => counter.value <= 0 }
	
	def size(): Int = elements.foldLeft(0){ 
	  case (sum, (_, counter)) if counter.value > 0 => sum + 1
	  case (sum, what) => sum
	}
	
	private def elementExists(counter: PNCounter[Int]): Boolean = !(counter.isNegative ||  counter.isZero)
	
	def merge(other: MPNSet[T]): Option[MPNSet[T]] = other.id match {
	  case `id` => 
	    val mergedElements = elements ++ ( 
	    		for {
	    		  (otherEle, otherCounter) <- other.elements 
	    		} yield {
	    		  elements.get(otherEle) match {
	    		    case None => (otherEle, otherCounter)
	    		    case Some(thisCounter) => (otherEle, thisCounter.merge(otherCounter).get)
	    		  }
	    		}
	    )
	    Some(new MPNSet(id, replica, (clock.merge(other.clock)).get, mergedElements))
	  case _ => None
	}

	def leq(other: MPNSet[T]): Option[Boolean] = other.id match {
	  case `id` =>
	    Some((clock leq other.clock).get)
	  case _ => None
	}
	
	override def toString(): String = s"MPNSet($id, $replica, $clock, $elements)"
}

object MPNSet {
  def apply[T](id: Id, replica: Replica): MPNSet[T] = new MPNSet[T](id, replica, GCounter[Long](id, replica), immutable.Map.empty)
  
  def apply[T](id: Id, replica: Replica, elements: T*) = {
    val elementPairs = for {
      ele <- elements
    } yield (ele, PNCounter(id, replica, 1))
    
    new MPNSet[T](id, replica,  GCounter[Long](id, replica).increment(elementPairs.size.toLong), elementPairs.toMap)
  }
}