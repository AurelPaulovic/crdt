package com.aurelpaulovic.crdt.replica

class NamedReplica(private val name: String) extends Replica {
  override def equals(other: Any): Boolean = other match {
    case (that: NamedReplica) => that.isInstanceOf[NamedReplica] && name == that.name
    case _ => false
  }
}