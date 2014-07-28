package com.aurelpaulovic.crdt.immutable.state

import com.aurelpaulovic.crdt.replica.Replica
import scala.collection.immutable
import com.aurelpaulovic.crdt.Id

class GCounter[T] private (val id: Id, private[this] val replica: Replica, private[this] val localValue: T, private[this] val payloadWithOutdatedLocal: immutable.Map[Replica, T])(implicit num: Numeric[T]) {
  import num._
  
  /*
   * The counter efficiently does not update the payload (payloadWithOutdatedLocal) map when incremented or merged and makes
   * use of the localValue. The outdated payload is, however, never leaded.
   * 
   * The only way to get the inner state of the counter is using the state or value fields, which 
   * both fix the local replica value in the state to have value equal to localValue.
   */
  
  protected lazy val state: GCounter.GCounterState[T] = new GCounter.GCounterState(payloadWithOutdatedLocal + (replica -> localValue))

  def increment(): GCounter[T] = new GCounter(id, replica, localValue + num.one, payloadWithOutdatedLocal)
    
  lazy val value: T = state.payload.foldLeft(num.zero)(_ + _._2)
  
  def compare(other: GCounter[T]): Option[Boolean] = other.id match {
    case `id` => Some(compare(other.state))
    case _ => None
  }

  def compare(other: GCounter.GCounterState[T]): Boolean = state.payload.forall {
    case (k, v) => v <= other.payload.getOrElse(k, -num.one)
  }
  
  def merge(other: GCounter[T]): Option[GCounter[T]] = other.id match {
    case `id` => Some(merge(other.state))
    case _ => None 
  }
  
  def merge(other: GCounter.GCounterState[T]): GCounter[T] = {
    val mergedPayload = payloadWithOutdatedLocal ++ (
        for {
			    pair @ (k, v) <- other.payload
			    if payloadWithOutdatedLocal.getOrElse(k, num.zero) < v
			  } yield pair
	  )
	  
	  new GCounter(id, replica, num.max(localValue, other.payload.getOrElse(replica, num.zero)), mergedPayload)
  }
  
  override def toString(): String = s"GCounter($id, $replica) with value $value"
}

object GCounter {
	def apply[T](id: Id, replica: Replica)(implicit num: Numeric[T]): GCounter[T] = new GCounter[T](id, replica, num.zero, immutable.Map[Replica, T]())
	
  def apply[T](id: Id, replica: Replica, value: T)(implicit num: Numeric[T]): GCounter[T] = new GCounter[T](id, replica, value, immutable.Map[Replica, T]())
  
  def apply[T](id: Id, replica: Replica, initState: GCounterState[T])(implicit num: Numeric[T]) = {
    new GCounter(id, replica, initState.payload.getOrElse(replica, num.zero), initState.payload)
  }
  
  protected[GCounter] class GCounterState[T: Numeric](protected[GCounter] val payload: immutable.Map[Replica, T])
}