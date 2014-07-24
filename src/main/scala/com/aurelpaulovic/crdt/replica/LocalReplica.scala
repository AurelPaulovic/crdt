package com.aurelpaulovic.crdt.replica

trait LocalReplica extends Replica {
  final val identity = LocalIdentity
  
  object LocalIdentity extends ReplicaIdentity
}

