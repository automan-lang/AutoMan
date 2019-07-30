package edu.umass.cs.automan.adapters.googleads

case class ScriptError(err: String, details: String) extends Exception(err)