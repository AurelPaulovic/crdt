package com.aurelpaulovic.crdt.immutable.state.lattice

trait JoinSemilattice[T <: JoinSemilattice[T]] extends LatticeOrderOps[T] { this: T =>
  def join(other: T): T

  def /+\ (other: T): T = this join other

  def /<=\ (other: T): Boolean = this isDominatedBy other

  def />=\ (other: T): Boolean = this dominates other

  def /==\ (other: T): Boolean = this isSameAs other

  def /~\ (other: T): Boolean = this isConcurrentWith other

  def /!=\ (other: T): Boolean = !(this /==\ other)
}