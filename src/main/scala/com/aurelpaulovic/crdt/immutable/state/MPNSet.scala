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
import scala.reflect.runtime.universe._

class MPNSet[E] private (val id: Id, val replica: Replica, private val clock: component.GCounter[Long], private val elements: immutable.Map[E, component.PNCounter[Int]])(implicit paramType: TypeTag[E]) extends CRDT[Set[E], MPNSet[E]] {
	def value(): Set[E] = (elements.collect{case (ele, counter) if elementExists(counter) => ele}).toSet
  
  def add(ele: E): MPNSet[E] = elements.get(ele) match {
	  case None => new MPNSet(id, replica, clock.increment, elements + newElement(ele))
	  case Some(counter) if elementExists(counter) => this
	  case Some(counter) => new MPNSet(id, replica, clock.increment, elements.updated(ele, counter.setToOne))
	}
	
	private def newElement(ele: E): (E, component.PNCounter[Int]) = (ele, component.PNCounter(replica, 1))
	
	def remove(ele: E): MPNSet[E] = elements.get(ele) match {
	  case None => this
	  case Some(counter) if !elementExists(counter) => this
	  case Some(counter) => new MPNSet(id, replica, clock.increment, elements.updated(ele, counter.setToZero))
	}
	
	def contains(ele: E): Boolean = elements.get(ele) match {
		case Some(counter) => elementExists(counter)
	  case None => false
	}
	
	def isEmpty(): Boolean = elements.forall{ case (ele, counter) => counter.value <= 0 }
	
	def size(): Int = elements.foldLeft(0){ 
	  case (sum, (_, counter)) if counter.value > 0 => sum + 1
	  case (sum, what) => sum
	}
	
	private def elementExists(counter: component.PNCounter[Int]): Boolean = counter.value > counter.zero
	
	def merge(other: MPNSet[E]): Option[MPNSet[E]] = other.id match {
	  case `id` => 
	    val mergedElements = elements ++ ( 
	    		for {
	    		  (otherEle, otherCounter) <- other.elements 
	    		} yield {
	    		  elements.get(otherEle) match {
	    		    case None => (otherEle, otherCounter)
	    		    case Some(thisCounter) => (otherEle, thisCounter /+\ otherCounter)
	    		  }
	    		}
	    )
	    Some(new MPNSet(id, replica, clock /+\ other.clock, mergedElements))
	  case _ => None
	}

	def leq(other: MPNSet[E]): Option[Boolean] = other.id match {
	  case `id` =>
	    Some(clock isDominatedBy other.clock)
	  case _ => None
	}
	
	protected def canRdtTypeEqual[OTHER: TypeTag](other: Any) = {
    typeOf[OTHER] == typeOf[E] && other.isInstanceOf[MPNSet[_]]
  }

  def rdtTypeEquals(other: Any) = other match {
    case that: MPNSet[E] => that.canRdtTypeEqual[E](this)
    case _ => false
  }
  
  def copyForReplica(newReplica: Replica): MPNSet[E] = new MPNSet[E](id, newReplica, clock.copyForReplica(newReplica), elements.mapValues(_.copyForReplica(newReplica)))
	
	override def toString(): String = s"MPNSet($id, $replica, $clock, $elements)"
}

object MPNSet {
  def apply[T](id: Id, replica: Replica)(implicit paramType: TypeTag[T]): MPNSet[T] = new MPNSet[T](id, replica, component.GCounter[Long](replica), immutable.Map.empty)
  
  def apply[T](id: Id, replica: Replica, elements: T*)(implicit paramType: TypeTag[T]) = {
    val elementPairs = for {
      ele <- elements
    } yield (ele, component.PNCounter(replica, 1))
    
    new MPNSet[T](id, replica,  component.GCounter[Long](replica).increment(elementPairs.size.toLong), elementPairs.toMap)
  }
}