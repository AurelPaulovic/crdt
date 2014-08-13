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

class NamedReplica (val name: String) extends Replica with Ordered[NamedReplica] with Serializable with Equals {
  def compare(other: NamedReplica): Int = name.compare(other.name)
  
  override def toString = s"NamedReplica($name)"

  def canEqual(other: Any) = {
    other.isInstanceOf[com.aurelpaulovic.crdt.replica.NamedReplica]
  }

  override def equals(other: Any) = other match {
    case that: com.aurelpaulovic.crdt.replica.NamedReplica => that.canEqual(NamedReplica.this) && name == that.name
    case _ => false
  }

  override def hashCode() = 41 + name.hashCode
}

object NamedReplica {
  implicit def stringToReplica(str: String): NamedReplica = new NamedReplica(str)
  
  def apply(name: String): NamedReplica = new NamedReplica(name) 
}