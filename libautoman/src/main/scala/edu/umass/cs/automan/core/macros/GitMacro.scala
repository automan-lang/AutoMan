package edu.umass.cs.automan.core.macros

import scala.language.experimental.macros
import scala.reflect.macros._
import scala.sys.process._

object GitMacro {
  // this returns the git hash for HEAD
  def currentHash(): String = macro currentHash_impl
  def currentHash_impl(c: blackbox.Context)() : c.Expr[String] = {
    import c.universe._
    reify { ("git rev-parse HEAD" !!).replace("\n","") }
  }
}