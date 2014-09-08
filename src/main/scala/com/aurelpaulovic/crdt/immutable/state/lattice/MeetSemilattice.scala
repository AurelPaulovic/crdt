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