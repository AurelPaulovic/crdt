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
	    var counter = GCounter[Int]("counter", new NamedReplica("rep")) 
	    
	    "have value 0" in {
	      assert(counter.value == 0)
	    }
	    
	    "have value = 1 after first increment" in {
	      counter = counter.increment
	      assert(counter.value == 1)
	    }
	    
	    "have value = 2 after second increment" in {
	      counter = counter.increment
	      assert(counter.value == 2)
	    }
	  }
	  
	  "initialized to 100" should {
	    var counter = GCounter("counter", new NamedReplica("rep"), 100)
	    "have value 100" in {
	      assert(counter.value == 100)
	    }
	    
	    "have value 101 after an increment" in {
	      counter = counter.increment
	      assert(counter.value == 101)
	    }
	  }
	  
	  "with some value" should {
	    "be mergable with itself" in new Counter1 with Replica {
	      val c1_2 = c1.increment
	      
	      assert(c1_2.merge(c1_2).isDefined)
	      assert(c1_2.merge(c1).isDefined)
	      assert(c1.merge(c1_2).isDefined)
	    }
	    
	    "be comparable with itself" in new Counter1 with Replica {
	      val c1_2 = c1.increment
	      
	      assert(c1_2.compare(c1_2).isDefined)
	      assert(c1_2.compare(c1).isDefined)
	      assert(c1.compare(c1_2).isDefined)
	    }
	    
	    "have the newer value after merge with itself" in new Counter1 with Replica {
	      val c1_2 = c1.increment
	      
	      assert(c1_2.merge(c1).value.value == 1)
	      assert(c1.merge(c1_2).value.value == 1)
	      assert(c1_2.merge(c1_2).value.value == 1)
	    }
	    
	    "be comparable with itself and preserve the semilattice ordering" in new Counter1 with Replica {
	      val c1_2 = c1.increment
	      
	      assert(c1_2.compare(c1_2).value == true)
	      assert(c1_2.compare(c1).value == false)
	      assert(c1.compare(c1_2).value == true)
	    }
	  }
	  
	  "initialized with a number of replicas" should {
	  	"be mergable" in new MultiReplicas {
	  	  for (
	  	    List(c1, c2) <- counters.combinations(2)
	  	  ) {
	  	    assert(c1.merge(c2).isDefined)
	  	    assert(c2.merge(c1).isDefined)
	  	  }
	  	}
	  	
	  	"be comparable" in new MultiReplicas {
	  	  for (
	  	    List(c1, c2) <- counters.combinations(2)
	  	  ) {
	  	    assert(c1.compare(c2).isDefined)
	  	    assert(c2.compare(c1).isDefined)
	  	  }
	  	}
	  	
	  	"be mergable even with all replicas incremented" in new MultiReplicas {
	  	  val incremented = counters.map(_.increment)
	  	  
  	    for (
	  	    List(c1, c2) <- incremented.combinations(2)
	  	  ) {
	  	    assert(c1.merge(c2).isDefined)
	  	    assert(c2.merge(c1).isDefined)
	  	  }
	  	}
	  	
	  	"be comparable even with all replicas incremented" in new MultiReplicas {
	  	  val incremented = counters.map(_.increment)
	  	  
  	    for (
	  	    List(c1, c2) <- incremented.combinations(2)
	  	  ) {
	  	    assert(c1.compare(c2).isDefined)
	  	    assert(c2.compare(c1).isDefined)
	  	  }
	  	}
	  	
	  	"after merge contain a sum of the replicas' values" in new MultiReplicas {
	  	  val c1 = (1 to 10).foldLeft(counters(0))((c, _) => c.increment)
	  	  val c2 = (1 to 5).foldLeft(counters(1))((c, _) => c.increment)
	  	  
	  	  val m12 = c1.merge(c2)
	  	  val m21 = c2.merge(c1)
	  	  
	  	  assert(m12.value.value == m21.value.value) //commutative
	  	  assert(m12.value.value == 15)
	  	}
	  	
	  	"after merge preserve the semilattice ordering" in new MultiReplicas {
	  	  val c1 = (1 to 10).foldLeft(counters(0))((c, _) => c.increment)
	  	  val c2 = (1 to 5).foldLeft(counters(1))((c, _) => c.increment)
	  	  
	  	  val om12 = c1.merge(c2)
	  	  val om21 = c2.merge(c1)
	  	  
	  	  assert(c1.compare(c2).value == false)
	  	  assert(c2.compare(c1).value == false)
	  	  
	  	  assert((for(x <- om12; y <- om21; z <- x.compare(y)) yield z).value == true)
	  	  assert((for(x <- om21; y <- om12; z <- x.compare(y)) yield z).value == true)
	  	  
	  	  assert((for(x <- om12; z <- x.compare(c1)) yield z).value == false)
	  	  assert((for(x <- om12; z <- x.compare(c2)) yield z).value == false)
	  	  assert((for(x <- om21; z <- x.compare(c1)) yield z).value == false)
	  	  assert((for(x <- om21; z <- x.compare(c2)) yield z).value == false)
	  	  
	  	  assert((for(x <- om12; z <- c1.compare(x)) yield z).value == true)
	  	  assert((for(x <- om12; z <- c2.compare(x)) yield z).value == true)
	  	  assert((for(x <- om21; z <- c1.compare(x)) yield z).value == true)
	  	  assert((for(x <- om21; z <- c2.compare(x)) yield z).value == true)
	  	  
	  	  assert(c1.compare(c1.increment).value == true)
	  	  assert(c1.increment().compare(c1).value == false)
	  	  
	  	  assert(c1.compare(om12.value.increment).value == true)
	  	}
	  }
	}
	
	"Two different GCounters" should {
	  "not be mergable" in new Counter1 with Counter2 with Replica {
	    assert(c1.merge(c2) == None)
	    assert(c2.merge(c1) == None)
	  }
	  
	  "not be comparable" in new Counter1 with Counter2 with Replica {
	    assert(c1.compare(c2) == None)
	    assert(c2.compare(c1) == None)
	  }
	}
}