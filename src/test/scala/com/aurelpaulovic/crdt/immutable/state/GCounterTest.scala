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

@RunWith(classOf[JUnitRunner])
class GCounterTest extends TestSpec {
	import com.aurelpaulovic.crdt.replica.NamedReplica
	
	trait Replica {
	  val replica = new NamedReplica("rep")
	}
	
	trait MultiReplicas {
	  val replicas = (for (i <- 1 to 5) yield new NamedReplica("rep" + i)).toList
	  val id = "counter1"
	  
  	val counters = replicas.map(GCounter[Int](id, _))
	}
	
	trait Counter1 {
	  this: Replica => 
	    
	  val c1 = GCounter[Int]("c1", replica)
	}
	
	trait Counter2 {
	  this: Replica => 
	    
	  val c2 = GCounter[Int]("c2", replica)
	}
	
	"A GCounter" when {
	  "initialized to zero" should {
	    "have value 0" in new Counter1 with Replica{
	      assert(c1.value == 0)
	    }
	  }
	  
	  "initialized to 100" should {
	    var counter = GCounter("counter", new NamedReplica("rep"), 100)
	    
	    "have value 100" in {
	      assert(counter.value == 100)
	    }
	  }
	  
	  "incremented" should {
	  	var counter = GCounter[Int]("counter", new NamedReplica("rep"))
	  	var counter1 = GCounter("counter", new NamedReplica("rep"), 100)
	  	
	    "have correct value" in {
	  		counter = counter.increment
				assert(counter.value == 1)
	      
	  		counter = counter.increment
	  		assert(counter.value == 2)
	  		
	      counter1 = counter1.increment
	      assert(counter1.value == 101)
	  		
	  		counter1 = counter1.increment
	      assert(counter1.value == 102)
	    }
	  	
	  	"have correct value after merging and then incremented and then merged again with some stale replica" in {
	  	  val c1 = GCounter[Int]("counter", new NamedReplica("rep1")).increment.increment //rep1 (2, 0)
	  	  val c2 = GCounter[Int]("counter", new NamedReplica("rep2")).increment //rep2 (0, 1)
	  	  
	  	  val m21 = c2.merge(c1).value.increment //rep2 (2, 2)
	  	  val c1_2 = c1.increment.increment //rep1 (4, 0)
	  	   
	  	  val c1_2_m21 = c1_2.merge(m21).value //rep1 (4, 2)
	  	  val m21_c12 = m21.merge(c1_2).value //rep2 (4, 2)
	  	  
	  	  val c1_2_c1 = c1_2.merge(c1).value //rep1 (4, 0)
	  	  val m21_c12_m21 = m21_c12.merge(m21).value.increment //rep2 (4, 3)
	  	  
	  	  val c1_2_m21_c12_m21 = c1_2.merge(m21_c12_m21).value //rep1 (4, 3)
	  	  val c2i_c1_2_m21_c12_m21 = c2.increment.increment.increment.merge(c1_2_m21_c12_m21).value //rep2 (4, 4)
	  	  
	  	  assert(c1.value == 2)
	  	  assert(c2.value == 1)
	  	  assert(m21.value == 4)
	  	  assert(c1_2.value == 4)
	  	  assert(c1_2_m21.value == 6)
	  	  assert(m21_c12.value == 6)
	  	  assert(c1_2_c1.value == 4)
	  	  assert(m21_c12_m21.value == 7)
	  	  assert(c1_2_m21_c12_m21.value == 7)
	  	  assert(c2i_c1_2_m21_c12_m21.value == 8)
	  	}
	  }
	  
	  "having two replicas" should {
	    "be mergable" in new MultiReplicas {
	      def inner[T](c1: GCounter[T], c2: GCounter[T], sum: T) {
	        assert(c1.merge(c2).isDefined)
		      assert(c2.merge(c1).isDefined)
		      
		      assert(c1.merge(c2).map(_.value).value == sum)
		      assert(c2.merge(c1).map(_.value).value == sum)
	      }
	      
	      val c1 = counters(0)
	      val c2 = counters(1)
	      val c1_2 = (1 to 10).foldLeft(c1)((c, _) => c.increment)
	      val c2_2 = (1 to 5).foldLeft(c2)((c, _) => c.increment)
	      
	      inner(c1, c2, 0)
	      inner(c2, c1, 0)
	      inner(c1, c1, 0)
	      inner(c1, c1.increment, 1)
	      inner(c1_2, c2_2, 15)
	      inner(c1_2, c2, 10)
	      inner(c1, c2_2, 5)
	    }

	    "be comparable" in new MultiReplicas {
	      def inner[T](c1: GCounter[T], c2: GCounter[T]) {
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
	      inner(c1.increment, c2.increment)
	      
	      assert(c1.leq(c1).value == true)
	      assert(c1.leq(c1.increment).value == true)
	      assert(c1.increment.leq(c1).value == false)
	      assert(c1.increment.leq(c1.increment).value == true)
	    }
	  }
	  
	  "for two different counters" should {
	    "be not mergable" in new Counter1 with Counter2 with Replica {
	      def inner[T](c1: GCounter[T], c2: GCounter[T]) {
	        assert(c1.merge(c2).isEmpty)
	        assert(c2.merge(c1).isEmpty)
	      }
	      
	      inner(c1, c2)
	      inner(c1.increment, c2)
	      inner(c1, c2.increment)
	    }
	    
	    "be not comparable" in new Counter1 with Counter2 with Replica {
	      def inner[T](c1: GCounter[T], c2: GCounter[T]) {
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