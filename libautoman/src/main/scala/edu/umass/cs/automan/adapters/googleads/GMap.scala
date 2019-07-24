package edu.umass.cs.automan.adapters.googleads

import edu.umass.cs.automan.adapters.googleads.ads.Ad
import edu.umass.cs.automan.adapters.googleads.question.GQuestion

import scala.collection.mutable

class GMap {
  var map = new mutable.HashMap[String, (GQuestion,Ad)]

  protected[googleads] def add(id: String, question: GQuestion, ad: Ad) = {
    map.put(id, (question,ad))
  }

  protected[googleads] def get(id: String) = {
    map.get(id) match { case Some(q) => q case None => throw new Exception("Question does not exist")}
  }

  protected[googleads] def remove(id: String) = {
    map.remove(id)
  }
}
