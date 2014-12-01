package edu.umass.cs.automan.core

trait Plugin {
  def startup(adapter: AutomanAdapter)
  def shutdown()
}
