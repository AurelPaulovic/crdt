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
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import com.aurelpaulovic.crdt.replica.NamedReplica._
import com.aurelpaulovic.crdt.util.Mergeable

@RunWith(classOf[JUnitRunner])
class MergeableRegisterTest extends TestSpec {
  import Mergeable._
  
  implicit object MergeableString extends Mergeable[String] {
    def merge(thisValue: String, thatValue: String) = Seq(thisValue, thatValue).sorted.mkString(", ")
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
	}
}