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
import com.aurelpaulovic.crdt.replica.NamedReplica
import com.aurelpaulovic.crdt.RDT

class MPNSetTest extends TestSpec {
  def emptySet: MPNSet[String] = MPNSet[String]("set", new NamedReplica("rep"))
  
  def nonEmptySet: MPNSet[String] = MPNSet("set", new NamedReplica("rep"), "a", "b", "c")
  
	"A MPNSet" when {
	  "initialized to empty" should {
	    "be empty" in {
        val set = emptySet
        
	      assert(set.isEmpty)
	      assert(!set.contains("a"))
	      assertResult(0)(set.size())
	    }
	    
	    "after adding an element be non-empty and contain that element" in {
        val set = emptySet
	      val newSet = set.add("a")
	      
	      assert(!newSet.isEmpty)
	      assert(newSet.contains("a"))
	      assertResult(1)(newSet.size())
	    }
	    
	    "after adding and then removing an element be empty" in {
        val set = emptySet
	      val newSet = set.add("a").remove("a")
	      
	      assert(!newSet.contains("a"))
	      assert(newSet.isEmpty)
	      assertResult(0)(newSet.size())
	    }
	    
	    "after removing an non existing element be still empty" in {
        val set = emptySet
	      val newSet = set.remove("a")
	      
	      assert(newSet.isEmpty)
	      assert(!newSet.contains("a"))
	      assertResult(0)(newSet.size())
	    }
	    
	    "after adding the element twice and removing it once be empty" in {
        val set = emptySet
	      val newSet = set.add("a").add("a").remove("a")
	      
	      assert(newSet.isEmpty)
	      assert(!newSet.contains("a"))
	      assertResult(0)(newSet.size())
	    }
	    
	    "after adding some element be greater than the empty set" in {
        val set = emptySet
	      val newSet = set.add("a")
	      
	      assert((set leq newSet).value)
	      assert((newSet leq set).value == false)
	    }
	    
	    "stay empty when merged with itself" in {
        val set = emptySet
	      val newSet = (set merge set).get
	      
	      assert((newSet leq set).value)
	      assert((set leq newSet).value)
	      assert(newSet.isEmpty)
	      assertResult(0)(newSet.size)
	    } 
	    
	    "be the same as another empty replica" in {
	      val set1 = MPNSet[String]("set", new NamedReplica("rep1"))
	      val set2 = MPNSet[String]("set", new NamedReplica("rep2"))
	      
	      assert((set1 leq set2).value)
	      assert((set2 leq set1).value)
	    }
	  }
	  
	  "with some elements" should {
	    "contain all the elements" in {
        val set = nonEmptySet
        
	      assert(!set.isEmpty)
	      assertResult(3)(set.size)
	      assert(set.contains("a"))
	      assert(set.contains("b"))
	      assert(set.contains("c"))
	    }
	    
	    "be equal to iself after removing an nonexisting element or adding an already existing element" in {
        val set = nonEmptySet
	      val newSet = set.add("a")
	      assert((newSet leq set).value && (set leq newSet).value)
	      
	      val newSet2 = set.remove("x")
	      assert((newSet2 leq set).value && (set leq newSet2).value)
	    }
	    
	    "be leq than itself with some new element added" in {
        val set = nonEmptySet
	      val newSet = set.add("d")
	      
	      assert((set leq newSet).value)
	      assert((newSet leq set).value == false)
	    }
	    
	    "be leq than itself with some element removed" in {
        val set = nonEmptySet
	      val newSet = set.remove("b")
	      
	      assert((set leq newSet).value)
	      assert((newSet leq set).value == false)
	    }
	    
	    "stay the same when merged with itself" in {
        val set = nonEmptySet
	      val newSet = (set merge set).get
	      
	      assert((newSet leq set).value && (set leq newSet).value)
	      assert(!newSet.isEmpty)
	      assertResult(3)(newSet.size)
	      assert(newSet.contains("a"))
	      assert(newSet.contains("b"))
	      assert(newSet.contains("c"))
	      assert(!newSet.contains("d"))
	    } 
	  }
	  
	  "with 2 replicas" should {
	    "be concurrent if they are unmerged" in {
	      val set1 = MPNSet("set", new NamedReplica("rep1"), "a")
	      val set2 = MPNSet("set", new NamedReplica("rep2"), "a")
	      
	      assert((set1 leq set2).value == false)
	      assert((set2 leq set1).value == false)
	    }
	    
	    "contain union when merged" in {
	      val set1 = MPNSet("set", new NamedReplica("rep1"), "a")
	      val set2 = MPNSet("set", new NamedReplica("rep2"), "b")
	      
	      val merged12 = (set1 merge set2).value
	      val merged21 = (set2 merge set1).value
	      
	      assert(merged12.contains("a"))
	      assert(merged12.contains("b"))
	      assertResult(2)(merged12.size)
	      
	      assert(merged21.contains("a"))
	      assert(merged21.contains("b"))
	      assertResult(2)(merged21.size)
	    } 
	    
	    "conform to leq rules when merged a few times" in {
	      val set1 = MPNSet("set", new NamedReplica("rep1"), "a")
	      val set2 = MPNSet("set", new NamedReplica("rep2"), "b")
	      
	      assert((set1 leq set2).value == false)
	      assert((set2 leq set1).value == false)
	      
	      val set12 = set1.merge(set2).get
	      val set21 = set2.merge(set1).get
	      
	      assert((set1 leq set12).value)
	      assert((set2 leq set12).value)
	      assert((set1 leq set21).value)
	      assert((set1 leq set12).value)
	      
	      assert((set12 leq set21).value)
	      assert((set21 leq set12).value)
	      
	      val set12_a = set12.remove("a")
	      val set12_b = set12.remove("b")
	      
	      assert((set1 leq set12_a).value && (set12_a leq set1).value == false)
	      assert((set12 leq set12_a).value && (set12_a leq set12).value == false)
	      assert((set21 leq set12_a).value & (set12_a leq set21).value == false)
	      
	      /* 
	       * CURRENT RULES - CRDTs expect a linear history for a single replica 
	       * -> you are not allowed to split history from a single replica
	       */
	      assert((set12_a leq set12_b).value && (set12_b leq set12_a).value) 
	    }
	  }
	  
	  "rdt type equal compared" should {
      "be different if they are different CRDT types" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MPNSet[Int]("id1", rep).asInstanceOf[RDT]
        val rdt2 = GCounter[Int]("id2", rep).asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2) == false)
        assert(rdt2.rdtTypeEquals(rdt1) == false)
      }
      
      "be the same if the are of the same CRDT type" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MPNSet[Int]("id1", rep).asInstanceOf[RDT]
        val rdt2 = MPNSet[Int]("id2", rep).asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2))
        assert(rdt2.rdtTypeEquals(rdt1))
      }
      
      "be different if the are of the same CRDT type but have different type parameter" in {
        val rep = NamedReplica("rep1")
        val rdt1 = MPNSet[Int]("id1", rep).asInstanceOf[RDT]
        val rdt2 = MPNSet[Long]("id2", rep).asInstanceOf[RDT]
        
        assert(rdt1.rdtTypeEquals(rdt2) == false)
        assert(rdt2.rdtTypeEquals(rdt1) == false)
      }
    }
	} 
}