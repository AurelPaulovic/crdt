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

package com.aurelpaulovic.crdt.util

import com.aurelpaulovic.crdt.replica.Replica

case class TotalTimeClock[R <: Replica] private (val time: Long, val replica: R with Ordered[R], val ticks: Int = 0) extends TotalOrdering[TotalTimeClock[R]] {
  def compare(other: TotalTimeClock[R]): Int = other match {
    case TotalTimeClock(otherTime, _, _) if otherTime != time => time.compare(otherTime)
    case TotalTimeClock(_, otherReplica, _) if otherReplica != replica => replica.compare(otherReplica)
    case TotalTimeClock(_, _, otherTicks) => ticks.compare(otherTicks)
  }
  
  def makeGreaterThan(other: TotalTimeClock[R]): TotalTimeClock[R] = TotalTimeClock.makeGreaterThan(replica, other)
}

object TotalTimeClock {
  private var highestSeenTime: Long = System.currentTimeMillis()
  private var currentTick: Int = 0
  
  def apply[R <: Replica](replica: R with Ordered[R]): TotalTimeClock[R] = this.synchronized {
    val currentMillis = System.currentTimeMillis()
    if (currentMillis <= highestSeenTime) currentTick += 1
    else {
      highestSeenTime = currentMillis
      currentTick = 0
    }
    
    new TotalTimeClock(highestSeenTime, replica, currentTick)
  }
  
  def makeGreaterThan[R <: Replica](replica: R with Ordered[R], other: TotalTimeClock[R]): TotalTimeClock[R] = this.synchronized {
    if (other.time > highestSeenTime) highestSeenTime = other.time
    
    TotalTimeClock(replica)
  }
}