package com.aurelpaulovic.crdt.immutable.state.lattice

trait Lattice[T <: Lattice[T]] extends JoinSemilattice[T] with MeetSemilattice[T] { self: T =>

}