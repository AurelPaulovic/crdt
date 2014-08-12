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

package com.aurelpaulovic.crdt.immutable.state

import com.aurelpaulovic.crdt.immutable.TestSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.aurelpaulovic.crdt.replica.NamedReplica

@RunWith(classOf[JUnitRunner])
class PNCounterTest extends TestSpec {
  trait Replica {
	  val replica = new NamedReplica("rep")
	}
	
	trait MultiReplicas {
	  val replicas = (for (i <- 1 to 5) yield new NamedReplica("rep" + i)).toList
	  val id = "counter1"
	  
  	val counters = replicas.map(PNCounter[Int](id, _))
	}
	
	trait Counter1 {
	  this: Replica => 
	    
	  val c1 = PNCounter[Int]("c1", replica)
	}
	
	trait Counter2 {
	  this: Replica => 
	    
	  val c2 = PNCounter[Int]("c2", replica)
	}
  
  
	"A PNCounter" when {
	  "initialized to 0" should {
	    "have value 0" in new Counter1 with Replica {
	      assert(c1.value == 0)
	      assert(c1.isZero)
	      assert(c1.isNegative == false)
	    }
	  }
	  
	  "initialized to 100" should {
	    "have value 100" in new Replica {
	      val c = PNCounter("c1", replica, 100)
	      assert(c.value == 100)
	      assert(c.isZero == false)
	      assert(c.isNegative == false)
	    }
	  }
	  
	  "when decremented and/or incremented" should {
	    "have correct value" in {
	      var counter = PNCounter[Int]("counter", new NamedReplica("rep"))
	      
	      counter = counter.increment
	      assert(counter.value == 1)
	      assert(counter.isZero == false)
	      assert(counter.isNegative == false)
	      
	      counter = counter.increment
	      assert(counter.value == 2)
	      assert(counter.isZero == false)
	      assert(counter.isNegative == false)
	      
	      counter = counter.decrement
	      assert(counter.value == 1)
	      assert(counter.isZero == false)
	      assert(counter.isNegative == false)
	      
	      counter = counter.decrement
	      assert(counter.value == 0)
	      assert(counter.isZero)
	      assert(counter.isNegative == false)
	      
	      counter = counter.decrement
	      assert(counter.value == -1)
	      assert(counter.isZero == false)
	      assert(counter.isNegative)
	      
	      counter = counter.increment
	      assert(counter.value == 0)
	      assert(counter.isZero)
	      assert(counter.isNegative == false)
	    }
	    
	    "have value 0 after set to zero" in new Replica {
	      val c = PNCounter("c1", replica, 100) 
	      assert(c.setToZero.value == 0)
	      
	      val c2 = c.increment.increment.decrement.decrement
	      assert(c2.setToZero.value == 0)
	      
	      val c3 = PNCounter[Int]("counter", replica).decrement.decrement
	      assert(c3.setToZero.value == 0)
	    }
	    
	    "have value 1 after set to one" in new Replica {
	      val c = PNCounter("c1", replica, 100) 
	      assert(c.setToOne.value == 1)
	      
	      val c2 = c.increment.increment.increment.decrement.decrement
	      assert(c2.setToOne.value == 1)
	      
	      val c3 = PNCounter[Int]("counter", replica).decrement.decrement
	      assert(c3.setToOne.value == 1)
	    }
	  }
	  
	  "having two replicas" should {
	    "be mergable" in new MultiReplicas {
	      def inner[T](c1: PNCounter[T], c2: PNCounter[T], sum: T) {
	        assert(c1.merge(c2).isDefined)
		      assert(c2.merge(c1).isDefined)
		      
		      assert(c1.merge(c2).map(_.value).value == sum)
		      assert(c2.merge(c1).map(_.value).value == sum)
	      }
	      
	      val c1 = counters(0)
	      val c2 = counters(1)
	      val c1_2 = (1 to 10).foldLeft(c1)((c, _) => c.increment)
	      val c2_2 = (1 to 5).foldLeft(c2)((c, _) => c.increment)
	      val c1_3 = (1 to 5).foldLeft(c1_2)((c, _) => c.decrement)
	      val c2_3 = (1 to 8).foldLeft(c2_2)((c, _) => c.decrement)
	      
	      inner(c1, c2, 0)
	      inner(c2, c1, 0)
	      inner(c1, c1, 0)
	      inner(c1, c1_2, 10)
	      inner(c1_2, c1, 10)
	      inner(c1_2, c2_2, 15)
	      inner(c1_2, c2, 10)
	      inner(c1, c2_2, 5)
	      inner(c1_3, c2_3, 2)
	    }
	    
	    "be comparable" in new MultiReplicas {
	      def inner[T](c1: PNCounter[T], c2: PNCounter[T]) {
	        assert(c1.leq(c2).value == false)
		      assert(c2.leq(c1).value == false)
		      
		      val c12 = c1.merge(c2).value
		      
		      assert(c12.leq(c2).value == false)
		      assert(c12.leq(c1).value == false)
		      
		      assert(c1.leq(c12).value == true)
		      assert(c2.leq(c12).value == true)
		      
		      val c21 = c2.merge(c1).value
		      
		      assert(c12.leq(c21).value == true)
		      assert(c21.leq(c12).value == true)
	      }
	      
	      val c1 = counters(0)
	      val c2 = counters(1)
	      
	      inner(c1, c2)
	      inner(c1.increment, c2)
	      inner(c1, c2.increment)
	      inner(c1.decrement, c2.increment)
	      
	      assert(c1.leq(c1).value == true)
	      assert(c1.leq(c1.increment).value == true)
	      assert(c1.increment.leq(c1).value == false)
	      assert(c1.increment.leq(c1.increment).value == true)
	    }
	  }
	  
	  "for two different counters" should {
	    "be not mergable" in new Counter1 with Counter2 with Replica {
	      def inner[T](c1: PNCounter[T], c2: PNCounter[T]) {
	        assert(c1.merge(c2).isEmpty)
	        assert(c2.merge(c1).isEmpty)
	      }
	      
	      inner(c1, c2)
	      inner(c1.increment, c2)
	      inner(c1, c2.increment)
	    }
	    
	    "be not comparable" in new Counter1 with Counter2 with Replica {
	      def inner[T](c1: PNCounter[T], c2: PNCounter[T]) {
	        assert(c1.leq(c2).isEmpty)
	        assert(c2.leq(c1).isEmpty)
	      }
	      
	      inner(c1, c2)
	      inner(c1.increment, c2)
	      inner(c1, c2.increment)
	    }
	  }
	}
}