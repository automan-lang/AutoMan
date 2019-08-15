package edu.umass.cs.automan.adapters.googleads.util

import scala.util.Random
import scala.collection.JavaConverters._

object KeywordList {
  def keywords(): Set[String] = {
    Set("science", "programming", "research", "explore", "citizen science",
      "analysis", "search", "study", "crowdsource", "open source",
      "high school", "learning", "institute", "college", "education",
      "school", "exam", "student", "textbook", "lessons",
      "coding", "computer", "computer science", "math", "test",
      "astronomy", "professor", "undergraduate", "knowledge", "earth",
      "physics", "biology", "psychology", "chemistry", "ecology",
      "volunteer", "nonprofit", "software", "machine learning", "environment",
      "engineering", "statistics", "medical", "projects",
      "scistarter", "global", "national", "mechanical turk", "mturk")
  }

  //TODO: this only works on Williams lab machines
  def randomWord: String = {
    val dict = scala.io.Source.fromFile("/usr/share/dict/american-english").getLines.toArray

    def word: String = {
      val w = dict(Random.nextInt(dict.length))
      if (w.length < 10) w.replaceAll("'s", ""); else word
    }
    word
  }

}
