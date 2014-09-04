package com.aurelpaulovic.crdt.immutable.state

trait Lattice[T <: Lattice[T]] extends JoinSemilattice[T] with MeetSemilattice[T] { self: T =>

}