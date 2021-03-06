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
import com.aurelpaulovic.crdt.Id
import com.aurelpaulovic.crdt.RDT

trait CRDT[E, T <: CRDT[E, T]] extends RDT with Serializable {
  val id: Id
  val replica: Replica
	def value(): E
	
	def leq(other: T): Option[Boolean]
  def merge(other: T): Option[T]
  
  def leqRDT(other: RDT): Option[Boolean] = leq(other.asInstanceOf[T])
      
  def mergeRDT(other: RDT): Option[RDT] = merge(other.asInstanceOf[T]).map(_.asInstanceOf[RDT])
}
