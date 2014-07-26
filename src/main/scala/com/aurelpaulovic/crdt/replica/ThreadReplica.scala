package com.aurelpaulovic.crdt.replica


/** Replica owned by the current thread
  *
  * The thread is identified using its id. That means, that if you reuse threads (e.g. use a thread pool) or 
  * the thread id is somehow recycled, you could mix up two replicas that were intended to be separate.
  */
class ThreadReplica extends Replica {
  private val id = Thread.currentThread().getId()
    
  override def equals(other: Any): Boolean = other match {
    case (that: ThreadReplica) => that.isInstanceOf[ThreadReplica] && this.id == that.id
    case _ => false
  }
}