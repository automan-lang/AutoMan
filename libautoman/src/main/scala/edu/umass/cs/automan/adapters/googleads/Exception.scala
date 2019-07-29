package edu.umass.cs.automan.adapters.googleads

case class ScriptError(err: String) extends Exception(err)