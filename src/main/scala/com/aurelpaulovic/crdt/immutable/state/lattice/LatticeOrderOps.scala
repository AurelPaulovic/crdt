package com.aurelpaulovic.crdt.immutable.state

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