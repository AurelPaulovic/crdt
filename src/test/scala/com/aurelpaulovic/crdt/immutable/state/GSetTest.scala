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

package com.aurelpaulovic.crdt.immutable.state

import com.aurelpaulovic.crdt.TestSpec
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.aurelpaulovic.crdt.replica.NamedReplica

@RunWith(classOf[JUnitRunner])
class GSetTest extends TestSpec {
  trait EmptySet {
    val set = GSet[String]("set", new NamedReplica("rep"))
  }
  
  trait NonEmptySet {
    val set = GSet("set", new NamedReplica("rep"), "a", "b", "c")
  }
  
  "A GSet" when {
    "initialized to empty" should {
      "be empty" in new EmptySet {
        assert(set.isEmpty)
        assert(!set.contains("a"))
        assert(set.value.isEmpty)
        assertResult(0)(set.size())
      }
      
      "after adding an element be non-empty and contain that element" in new EmptySet {
        val newSet = set.add("a")
        
        assert(!newSet.isEmpty)
        assert(newSet.contains("a"))
        assertResult(1)(newSet.size())
      }
      
      "after adding the same element twice still contain the element only once" in new EmptySet {
        val newSet = set.add("a").add("a")
        
        assert(!newSet.isEmpty)
        assert(newSet.contains("a"))
        assertResult(1)(newSet.size())
      }
      
      "after adding some element be greater than the empty set" in new EmptySet {
        val newSet = set.add("a")
        
        assert((set leq newSet).value)
        assert((newSet leq set).value == false)
      }
      
      "stay empty when merged with itself" in new EmptySet {
        val newSet = (set merge set).get
        
        assert((newSet leq set).value)
        assert((set leq newSet).value)
        assert(newSet.isEmpty)
        assertResult(0)(newSet.size)
      } 
      
      "be the same as another empty replica" in {
        val set1 = GSet[String]("set", new NamedReplica("rep1"))
        val set2 = GSet[String]("set", new NamedReplica("rep2"))
        
        assert((set1 leq set2).value)
        assert((set2 leq set1).value)
      }
    }
    
    "with some elements" should {
      "contain all the elements" in new NonEmptySet {
        assert(!set.isEmpty)
        assertResult(3)(set.size)
        assert(set.contains("a"))
        assert(set.contains("b"))
        assert(set.contains("c"))
      }
      
      "be equal to iself after adding an already existing element" in new NonEmptySet {
        val newSet = set.add("a")
        assert((newSet leq set).value && (set leq newSet).value)
      }
      
      "be leq than itself with some new element added" in new NonEmptySet {
        val newSet = set.add("d")
        
        assert((set leq newSet).value)
        assert((newSet leq set).value == false)
      }

      "stay the same when merged with itself" in new NonEmptySet {
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
      "be the same when they are nonmerged but have the same elements" in {
        val set1 = GSet("set", new NamedReplica("rep1"), "a")
        val set2 = GSet("set", new NamedReplica("rep2"), "a")
        
        assert((set1 leq set2).value)
        assert((set2 leq set1).value)
      }
      
      "contain union when merged" in {
        val set1 = GSet("set", new NamedReplica("rep1"), "a")
        val set2 = GSet("set", new NamedReplica("rep2"), "b")
        
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
        val set1 = GSet("set", new NamedReplica("rep1"), "a")
        val set2 = GSet("set", new NamedReplica("rep2"), "b")
        
        assert((set1 leq set2).value == false)
        assert((set2 leq set1).value == false)
        
        val set12 = set1.merge(set2).get
        val set21 = set2.merge(set1).get
        
        assert((set1 leq set12).value)
        assert((set2 leq set12).value)
        assert((set1 leq set21).value)
        assert((set1 leq set12).value)
        
        assert((set12 leq set1).value == false)
        assert((set12 leq set2).value == false)
        assert((set21 leq set1).value == false)
        assert((set21 leq set2).value == false)
        
        assert((set12 leq set21).value)
        assert((set21 leq set12).value)
      }
    }
  } 
}