package edu.umass.cs.automan.core.logging

import edu.umass.cs.automan.core.scheduler.Thunk

sealed abstract class InsertUpdateOrSkip[A](t: Thunk[A])
case class Insert[A](t: Thunk[A]) extends InsertUpdateOrSkip(t)
case class Update[A](t: Thunk[A]) extends InsertUpdateOrSkip(t)
case class Skip[A](t: Thunk[A]) extends InsertUpdateOrSkip(t)