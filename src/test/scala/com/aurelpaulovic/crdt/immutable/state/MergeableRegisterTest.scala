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

import com.aurelpaulovic.crdt.TestSpec
import com.aurelpaulovic.crdt.replica.NamedReplica._
import com.aurelpaulovic.crdt.util.Mergeable
import com.aurelpaulovic.crdt.replica.NamedReplica
import com.aurelpaulovic.crdt.RDT

class MergeableRegisterTest extends TestSpec {
  import Mergeable._
  
  implicit object MergeableString extends Mergeable[String] {
    def merge(thisValue: String, thatValue: String) = Seq(thisValue, thatValue).sorted.mkString(", ")
  }
  
  implicit object MergeableInt extends Mergeable[Int] {
    def merge(thisValue: Int, thatValue: Int) = thisValue + thatValue
  }
  
	"A MergableRegister" when {
	  "initiated with a value" should {
	  	val mr = MergeableRegister("mr1", "rep1", "a")
	  			
		  "hold its value" in {
		    assert(mr.value == "a")
		  }
	  	
	  	"be less or equal than itself" in {
	  	  assert((mr leq mr).value == true)
	  	}
	  	
	  	"be less or equal with its new version with new assigned value" in {
	  	  assert((mr leq mr.assign("a")).value == true)
	  	}
	  }
	  
	  "assigned a new value" should {
	    val mr = MergeableRegister("mr1", "rep1", "a")
	    val mr2 = mr.assign("b")
	    
	    "hold the new value" in {
		    assert(mr2.value == "b")
	    }
	    
	    "be not less nor equal than the previous version before assignment" in {
	      assert((mr2 leq mr).value == false)
	    }
	  }
	  
	  "merged with its newer version" should {
	    "have the value of the newer version" in {
	      val mr1 = MergeableRegister("mr1","rep1","a")
	      val mr2 = mr1.assign("b")
	      
	      assert(mr1.merge(mr2).value.value == "b")
	      assert(mr2.merge(mr1).value.value == "b")
	    }
	  }
	  
	  "compared with another different replica" should {
	    val mr1 = MergeableRegister("mr1", "rep1", "a")
	    val mr2 = MergeableRegister("mr1", "rep2", "b")
	    
	    "be concurrent with another non-merged replica" in {
	      assert((mr1 leq mr2).value == false)
	      assert((mr2 leq mr1).value == false)
	    }
	  }
	  
	  "merged with another replica" should {
	    "be equal to the other replica, if it was less or equal than that replica" in {
	      val mr1 = MergeableRegister("mr1", "rep1", "a")
	      val mr2 = MergeableRegister("mr1", "rep2", "b").merge(mr1).value.assign("c")
	      
	      assert((mr1 leq mr2).value)
	      assert(mr1.merge(mr2).value.value == "c")
	      assert(mr2.merge(mr1).value.value == "c")
	    }
	    
	    "have a merged value if the replicas were concurrent" in {
	      val mr1 = MergeableRegister("mr1", "rep1", "a")
	      val mr2 = MergeableRegister("mr1", "rep2", "b")
	      
	      assert(mr1.merge(mr2).value.value == "a, b")
	    }
	  }
	  
	  "rdt type equal compared" should {
      "be different if they are different CRDT types" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MergeableRegister[String]("id1", rep, "a").asInstanceOf[RDT]
        val rdt2 = GCounter[Int]("id2", rep).asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2) == false)
        assert(rdt2.rdtTypeEquals(rdt1) == false)
      }
      
      "be the same if the are of the same CRDT type" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MergeableRegister[String]("id1", rep, "a").asInstanceOf[RDT]
        val rdt2 = MergeableRegister[String]("id2", rep, "b").asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2))
        assert(rdt2.rdtTypeEquals(rdt1))
      }
      
      "be different if the are of the same CRDT type but have different type parameter" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MergeableRegister[String]("id1", rep, "a").asInstanceOf[RDT]
        val rdt2 = MergeableRegister[Int]("id2", rep, 1).asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2) == false)
        assert(rdt2.rdtTypeEquals(rdt1) == false)
      }
    }
	}
}