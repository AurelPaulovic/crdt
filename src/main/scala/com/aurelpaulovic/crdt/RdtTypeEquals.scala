/*
 * Copyright 2014 Aurel Paulovic (aurel.paulovic@gmail.com) (aurelpaulovic.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License. 
 */

package com.aurelpaulovic.crdt

import scala.reflect.runtime.universe._

/** Defines methods for comparing the non-erased types of RDTs
  *
  * RDTs are usually containers of values and have a type parameter, which gets erased during compilation. This poses
  * a problem when sending the RDTs over a network. The RDTs get serialized on node A, sent and deserialized on node B.
  * After that they need to be type checked if the type of the RDT sent from A matches the type of the same RDT (same as
  * having the same id) on node B. If the RDT from node A matches the type of the RDT on node B, we can safely cast the
  * RDT from A to proper type and merge them together. This type check is facilitated using mechanism similar to equals and
  * canEqual from scala.Equals.
  * <p>
  * Sample implementation for a hypothetical RDT type Counter
  * {{{
  * class Counter[T : TypeTag] extends RDT with RdtTypeEquals {
  *   ...
  *   override def rdtTypeEquals(other: Any): Boolean = other match {
  *     case that: Counter[_] => that.canRdtTypeEqual[T](this)
  *     case _ => false
  *   }
  *
  *   override protected def canRdtTypeEqual[OTHER : TypeTag](other: Any): Boolean = {
  *     typeOf[OTHER] == typeOf[T] && other.isInstanceOf[Counter[_]]
  *   }
  *   ...
  * }
  *
  * }}}
  */
trait RdtTypeEquals { self: RDT =>
  protected def canRdtTypeEqual[OTHER : TypeTag](other: Any): Boolean

  def rdtTypeEquals(other: Any): Boolean
}