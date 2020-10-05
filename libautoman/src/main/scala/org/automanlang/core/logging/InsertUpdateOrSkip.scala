package org.automanlang.core.logging

sealed abstract class InsertUpdateOrSkip[T](t: T)
case class Insert[T](t: T) extends InsertUpdateOrSkip(t)
case class Update[T](t: T) extends InsertUpdateOrSkip(t)
case class Skip[T](t: T) extends InsertUpdateOrSkip(t)