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
import scala.reflect.runtime.universe._

class COSet[E] private (val id: Id, val replica: Replica, private val clock: component.GCounter[Long], private val elements: Map[E, component.GCounter[Long]])(implicit paramType: TypeTag[E]) extends CRDT[Set[E], COSet[E]] {
	def value(): Set[E] = elements.keySet
  
  def add(ele: E): COSet[E] = {
	  if (elements.contains(ele)) this
	  else new COSet[E](id, replica, clock.increment, elements + (ele -> clock.increment))
	}
	
	def remove(ele: E): COSet[E] = {
	  if (elements.contains(ele)) new COSet[E](id, replica, clock.increment, elements - ele)
	  else this
	}
	
	def contains(ele: E): Boolean = elements.contains(ele)
	
	def isEmpty(): Boolean = elements.isEmpty
	
	private lazy val sizeCache: Int = elements.size
	
	def size(): Int = sizeCache
	
	def merge(other: COSet[E]): Option[COSet[E]] = {
	  if (id == other.id) {
		  (clock /<=\ other.clock, other.clock /<=\ clock) match {
			  case (true, true) => Some(this) // this == other
			  case (true, false) => Some(new COSet[E](id, replica, clock /+\ other.clock, other.elements)) // this < other
			  case (false, true) => Some(this) // this > other
			  case (false, false) =>
			    //1. take elements present in both sets and merge their tags
			    val sharedElements = elements.collect{ case (ele, tag) if other.elements.contains(ele) => (ele -> tag /+\ other.elements(ele)) }
			    
			    //2. take elements that present only in this and are concurrent with current clock in other
			    val newThisElements = (elements -- other.elements.keys).filter{ case (_, tag) => tag /~\ other.clock }
			    
			    //3. take elements that present only in other and are concurrent with current clock in this
			    val newOtherElements = (other.elements -- elements.keys).filter{ case (_, tag) => tag /~\ clock }
			     
			    Some(new COSet[E](id, replica, clock /+\ other.clock, sharedElements ++ newThisElements ++ newOtherElements))// concurrent
			}
		} else None
	}
	
	def leq(other: COSet[E]): Option[Boolean] = {
	  if (id == other.id) Some(clock isDominatedBy other.clock)
	  else None
	}
	
	protected def canRdtTypeEqual[OTHER: TypeTag](other: Any) = {
    typeOf[OTHER] == typeOf[E] && other.isInstanceOf[COSet[_]]
  }

  def rdtTypeEquals(other: Any) = other match {
    case that: COSet[_] => that.canRdtTypeEqual[E](this)
    case _ => false
  }
  
  def copyForReplica(newReplica: Replica): COSet[E] = new COSet[E](id, newReplica, clock.copyForReplica(newReplica), elements.mapValues(_.copyForReplica(newReplica)))
	
	override def toString(): String = s"COSet($id, $replica, $clock) with elements ${elements.keys}" 
}

object COSet {
  def apply[T](id: Id, replica: Replica)(implicit paramType: TypeTag[T]): COSet[T] = new COSet[T](id, replica, component.GCounter[Long](replica), immutable.Map.empty)
  
  def apply[T](id: Id, replica: Replica, elements: T*)(implicit paramType: TypeTag[T]): COSet[T] = {
    val elementPairs = for {
      ele <- elements
    } yield (ele, component.GCounter[Long](replica, 1))
    
    new COSet[T](id, replica,  component.GCounter[Long](replica).increment(elementPairs.size.toLong), elementPairs.toMap)
  }
}