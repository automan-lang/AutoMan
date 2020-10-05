package org.automanlang.core.util

import scala.reflect.ClassTag

class BidirectionalMap[T : ClassTag, U : ClassTag](elems: (T,U)*) {
  val forward_map = Map[T,U](elems: _*)
  val backward_map = Map[U,T](elems.map { case (t: T, u: U) => (u,t) }: _*)
  def containsTKey(t: T) = forward_map.contains(t)
  def containsUKey(u: U) = backward_map.contains(u)
  def getU(t: T) = forward_map(t)
  def getT(u: U) = backward_map(u)
}
