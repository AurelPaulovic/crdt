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

package com.aurelpaulovic.crdt.immutable.state.component

import com.aurelpaulovic.crdt.immutable.state.lattice.JoinSemilattice
import com.aurelpaulovic.crdt.replica.Replica
import scala.collection.immutable

trait GSet[T] extends JoinSemilattice[GSet[T]]{
  def add(ele: T): GSet[T]
  
  def + (ele: T): GSet[T] = add(ele)
  
  protected[component] def payload: Set[T]
  
  def isEmpty: Boolean
  
  def size: Int
  
  def value(): Set[T]
  
  def contains(ele: T): Boolean
}

object GSet {
  def apply[T](): GSet[T] = new EmptyGSet[T]()
  
  def apply[T](elements: Seq[T]): GSet[T] = new NonEmptyGSet[T](elements.toSet)
}

final case class EmptyGSet[T]() extends GSet[T] {
  def contains(elem: T): Boolean = false
  
  def add(ele: T): GSet[T] = new NonEmptyGSet[T](immutable.Set(ele))
  
  override val isEmpty: Boolean = true
  
  val size: Int = 0
  
  protected[component] val payload = Set.empty[T]
  
  def dominates(other: GSet[T]): Boolean = other.isEmpty

  def isDominatedBy(other: GSet[T]): Boolean = true

  def isSameAs(other: GSet[T]): Boolean = other.isEmpty

  def join(other: GSet[T]): GSet[T] = {
    if (other.isEmpty) this
    else new NonEmptyGSet(other.payload)
  } 
  
  def value(): Set[T] = immutable.Set.empty[T]
  
  val contains: Boolean = false
}

case class NonEmptyGSet[T](val elements: Set[T]) extends GSet[T] {
  protected[component] val payload = elements
  
  override val isEmpty: Boolean = false
  
  lazy val size: Int = elements.size
  
  def value(): Set[T] = elements
  
  def contains(elem: T): Boolean = elements contains elem
  
  def add(ele: T): GSet[T] = new NonEmptyGSet[T](elements + ele)
  
  def isDominatedBy(other: GSet[T]): Boolean = elements subsetOf other.payload

  def dominates(other: GSet[T]): Boolean = other.payload subsetOf elements

  def isSameAs(other: GSet[T]): Boolean = elements equals other.payload

  def join(other: GSet[T]): GSet[T] = new NonEmptyGSet(payload union other.payload)
}