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

class MPNSet[T] private (val id: Id, val replica: Replica, private val clock: component.GCounter[Long], private val elements: immutable.Map[T, component.PNCounter[Int]])(implicit paramType: TypeTag[T]) extends CRDT[Set[T], MPNSet[T]] {
	def value(): Set[T] = (elements.collect{case (ele, counter) if elementExists(counter) => ele}).toSet
  
  def add(ele: T): MPNSet[T] = elements.get(ele) match {
	  case None => new MPNSet(id, replica, clock.increment, elements + newElement(ele))
	  case Some(counter) if elementExists(counter) => this
	  case Some(counter) => new MPNSet(id, replica, clock.increment, elements.updated(ele, counter.setToOne))
	}
	
	private def newElement(ele: T): (T, component.PNCounter[Int]) = (ele, component.PNCounter(replica, 1))
	
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
	
	private def elementExists(counter: component.PNCounter[Int]): Boolean = counter.value > counter.zero
	
	def merge(other: MPNSet[T]): Option[MPNSet[T]] = other.id match {
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

	def leq(other: MPNSet[T]): Option[Boolean] = other.id match {
	  case `id` =>
	    Some(clock isDominatedBy other.clock)
	  case _ => None
	}
	
	protected def canRdtTypeEqual[X: TypeTag](other: Any) = {
    typeOf[X] == typeOf[T] && other.isInstanceOf[com.aurelpaulovic.crdt.immutable.state.MPNSet[_]]
  }

  override def rdtTypeEquals(other: Any) = other match {
    case that: com.aurelpaulovic.crdt.immutable.state.MPNSet[T] => that canRdtTypeEqual this
    case _ => false
  }
	
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