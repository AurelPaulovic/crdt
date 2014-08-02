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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import com.aurelpaulovic.crdt.replica.NamedReplica._

@RunWith(classOf[JUnitRunner])
class VectorClockTest extends TestSpec {
	"A VectorClock" when {
	  "blank" should {
	    val c = new VectorClock("rep1")
	    
	    "be equal to itself" in {
	      val c2 = new VectorClock("rep1")
	      
	      assert(c == c2)
	      assert(c != c2 == false)
	    }
	    
	    "be less or equals than itself" in  {
	      assert(c <= new VectorClock("rep1"))
	    }
	    
	    "not be less than itself" in {
	      assert(c < new VectorClock("rep1") == false)
	    }
	    
	    "be concurrent with another blank VectorClock" in {
	      val c2 = new VectorClock("rep2")
	      
	      assert(c < c2 == false)
	      assert(c2 > c == false)
	      assert(c > c2 == false)
	      assert(c2 < c == false)
	      assert(c == c2 == false)
	    }
	    
	    "be concurrent with another (not merged) VectorClock with single increment" in {
	      val c2 = (new VectorClock("rep2")).increment
	      
	      assert(c < c2 == false)
	      assert(c2 > c == false)
	      assert(c > c2 == false)
	      assert(c2 < c == false)
	      assert(c == c2 == false)
	    }
	  }
	  
	  "in some state" should {
	    val c = new VectorClock("rep1").increment
	    
	    "be equal to itself" in {
	      val c2 = new VectorClock("rep1").increment
	      
	      assert(c == c2)
	      assert(c != c2 == false)
	    }
	    
	    "be less or equals than itself" in  {
	      assert(c <= new VectorClock("rep1").increment)
	    }
	    
	    "not be less than itself" in {
	      assert(c < new VectorClock("rep1").increment == false)
	    }
	    
	    "be less than its incremented instance" in {
	      assert(c < c.increment)
	    }
	    
	    "be greater when incremented than its non-incremented instance" in {
	      assert(c.increment > c)
	    }
	    
	    "be concurrent with another VectorClock, that has never beed merged with it" in {
	      val c2 = new VectorClock("rep2").increment.increment
	      
	      assert(c < c2 == false)
	      assert(c > c2 == false)
	    }
	    
	    "be less than another VectorClock, that has been merged with it" in {
	      val c2 = new VectorClock("rep2")
	      val m12 = c + c2
	      
	      assert(c < m12)
	      assert(m12 > c)
	    }
	    
	    "when incremented be concurrent with another VectorClock, that has been merged with it before the increment" in {
	      val c2 = new VectorClock("rep2")
	      val m12 = c + c2
	      val c2_2 = c2.increment
	      
	      assert(c < m12)
	      assert(c > m12 == false)
	      
	      assert(c2_2 < m12 == false)
	      assert(c2_2 > m12 == false)
	    }
	  }
	}
	
	"Two vector clocks" when {
	  "merged with the same VectorClock" should {
	    "be concurrent" in {
	      val c = new VectorClock("rep1")
	      val c2 = new VectorClock("rep2") + c
	      val c3 = new VectorClock("rep3") + c
	      
	      assert(c2 < c3 == false)
	      assert(c2 > c3 == false)
	      assert(c2 != c3)
	    }
	  }
	}
}