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

trait MeetSemilattice[T <: MeetSemilattice[T]] extends LatticeOrderOps[T] { this: T =>
  def meet(other: T): T

  def \+/ (other: T): T = this meet other

  def \<=/ (other: T): Boolean = this isDominatedBy other

  def \>=/ (other: T): Boolean = this dominates other

  def \==/ (other: T): Boolean = this isSameAs other

  def \~/ (other: T): Boolean = this isConcurrentWith other

  def \!=/ (other: T): Boolean = !(this \==/ other)
}