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

package com.aurelpaulovic.crdt.immutable.state.lattice

trait LatticeOrderOps[T <: LatticeOrderOps[T]] { this: T =>
  def compareTo(that: T): Option[Int] = {
    if      (this isSameAs that)      Some(0) // equal
    else if (this isDominatedBy that) Some(-1) // less
    else if (this dominates that)     Some(1) // greater
    else    None // concurrent
  }

  def isConcurrentWith(other: T): Boolean = compareTo(other).isEmpty

  def dominates(other: T): Boolean

  def isDominatedBy(other: T): Boolean

  def isSameAs(other: T): Boolean
}