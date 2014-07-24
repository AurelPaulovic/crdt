package com.aurelpaulovic.crdt.replica

trait NamedReplica extends Replica {
  val name: String
  final lazy val identity = NamedIdentity(name)
    
  case class NamedIdentity (name: String) extends ReplicaIdentity {
    override def equals(other: Any): Boolean = other match {
      case (that: NamedReplica#NamedIdentity) => that.isInstanceOf[NamedIdentity] && name == that.name
      case _ => false
    }
  }
}