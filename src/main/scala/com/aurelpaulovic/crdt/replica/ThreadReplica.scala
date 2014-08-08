/*
 * Copyright 2014 Aurel Paulovic (aurel.paulovic@gmail.com) (aurelpaulovic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * 			http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.aurelpaulovic.crdt.replica


/** Replica owned by the current thread
  *
  * The thread is identified using its id. That means, that if you reuse threads (e.g. use a thread pool) or 
  * the thread id is somehow recycled, you could mix up two replicas that were intended to be separate.
  */
class ThreadReplica extends Replica with Ordered[ThreadReplica] {
  private val id = Thread.currentThread().getId()
  
  def compare(other: ThreadReplica): Int = id.compare(other.id)
    
  override def equals(other: Any): Boolean = other match {
    case (that: ThreadReplica) => that.isInstanceOf[ThreadReplica] && this.id == that.id
    case _ => false
  }
}