package com.aurelpaulovic.crdt.replica

trait Replica {
  def identity: ReplicaIdentity
  
  abstract trait ReplicaIdentity
}