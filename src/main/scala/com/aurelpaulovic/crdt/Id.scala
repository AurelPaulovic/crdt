package com.aurelpaulovic.crdt

trait Id

case class StringId(id: String) extends Id
case class IntegerId(id: Int) extends Id

object Id {
  implicit final def StringId(id: String) = new StringId(id)
  implicit final def IntegerId(id: Int) = new IntegerId(id)
  
  def apply(id: String) = new StringId(id)
  def apply(id: Int) = new IntegerId(id)
}