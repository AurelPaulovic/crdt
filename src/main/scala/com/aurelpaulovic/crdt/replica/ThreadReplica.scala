package com.aurelpaulovic.crdt.replica


/** Replica owned by the current thread
  *
  * The thread is identified using its id. That means, that if you reuse threads (e.g. use a thread pool) or 
  * the thread id is somehow recycled, you could mix up two replicas that were intended to be separate.
  */
trait ThreadReplica extends Replica {
  override val identity = ThreadIdentity(Thread.currentThread())
    
  case class ThreadIdentity (thread: Thread) extends ReplicaIdentity {
    override def equals(other: Any): Boolean = other match {
      case (that: ThreadReplica#ThreadIdentity) => that.isInstanceOf[ThreadIdentity] && thread.getId == that.thread.getId
      case _ => false
    }
  }
}