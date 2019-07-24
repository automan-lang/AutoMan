package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.question.GQuestion
import scala.collection.mutable

class GMap {
  var map = new mutable.HashMap[String, GQuestion]

  protected def add(id: String, question: GQuestion) = {
    map.put(id, question)
  }

  protected def get(id: String) = {
    map.get(id) match { case Some(q) => q case None => throw new Exception("Question does not exist")}
  }

  protected def remove(id: String) = {
    map.remove(id)
  }
}
