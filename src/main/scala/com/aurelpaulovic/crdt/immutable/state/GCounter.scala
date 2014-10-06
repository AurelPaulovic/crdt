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
import scala.reflect.runtime.universe._
import scala.reflect.ClassTag

class GCounter[T] private (val id: Id, val replica: Replica, private val counter: component.GCounter[T])(implicit num: Numeric[T], paramType: TypeTag[T]) extends CRDT[T, GCounter[T]] {
  def this(id: Id, replica: Replica)(implicit num: Numeric[T], paramType: TypeTag[T]) = this(id, replica, component.GCounter(replica))

  lazy val value: T = counter.value

  def increment(): GCounter[T] = increment(num.one)

  def increment(by: T): GCounter[T] = new GCounter[T](id, replica, counter.increment(by))

  def leq(other: GCounter[T]): Option[Boolean] = {
    if (other.id == id) Some(counter /<=\ other.counter)
    else None
  }

  def merge(other: GCounter[T]): Option[GCounter[T]] = {
    if (other.id == id) Some(new GCounter[T](id, replica, counter /+\ other.counter))
    else None
  }
  
  protected def canRdtTypeEqual[X: TypeTag](other: Any) = {
    typeOf[X] == typeOf[T] && other.isInstanceOf[com.aurelpaulovic.crdt.immutable.state.GCounter[_]]
  }

  def rdtTypeEquals(other: Any) = other match {
    case that: com.aurelpaulovic.crdt.immutable.state.GCounter[_] => that canRdtTypeEqual this
    case _ => false
  }

  override def toString(): String = s"GCounter($id, $replica, $counter) with value $value"
}

object GCounter {
  def apply[T](id: Id, replica: Replica)(implicit num: Numeric[T], paramtType: TypeTag[T]): GCounter[T] = new GCounter[T](id, replica)

  def apply[T](id: Id, replica: Replica, value: T)(implicit num: Numeric[T], paramType: TypeTag[T]): GCounter[T] = new GCounter[T](id, replica).increment(value)
}