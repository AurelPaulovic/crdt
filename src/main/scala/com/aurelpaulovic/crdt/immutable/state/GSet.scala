/*
 * Copyright 2014 Aurel Paulovic (aurel.paulovic@gmail.com) (aurelpaulovic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import scala.reflect.runtime.universe._

class GSet[E] private (val id: Id, val replica: Replica, val elements: component.GSet[E])(implicit paramType: TypeTag[E]) extends CRDT[Set[E], GSet[E]] {
  def value(): Set[E] = elements.value
  
  def add(ele: E): GSet[E] = {
    if (elements.contains(ele)) this
    else new GSet[E](id, replica, elements.add(ele))
  }
  
  def isEmpty(): Boolean = elements.isEmpty
  
  def size(): Int = elements.size
  
  def contains(ele: E): Boolean = elements.contains(ele)
  
  def merge(other: GSet[E]): Option[GSet[E]] = {
    if (other.id == id) Some(new GSet(id, replica, elements /+\ other.elements))
    else None
  }
  
  def leq(other: GSet[E]): Option[Boolean] = {
    if (other.id == id) Some(elements isDominatedBy other.elements)
    else None
  }
  
  protected def canRdtTypeEqual[OTHER: TypeTag](other: Any) = {
    typeOf[OTHER] == typeOf[E] && other.isInstanceOf[GSet[_]]
  }

  def rdtTypeEquals(other: Any) = other match {
    case that: GSet[_] => that.canRdtTypeEqual[E](this)
    case _ => false
  }
  
  def copyForReplica(newReplica: Replica): GSet[E] = new GSet[E](id, newReplica, elements)
  
  override def toString(): String = s"GSet($id, $replica) with elements ${elements.value}"
}

object GSet {
  def apply[T](id: Id, replica: Replica)(implicit paramType: TypeTag[T]): GSet[T] = new GSet[T](id, replica, component.GSet[T]())
  
  def apply[T](id: Id, replica: Replica, elements: T*)(implicit paramType: TypeTag[T]): GSet[T] = new GSet[T](id, replica, component.GSet[T](elements))
}