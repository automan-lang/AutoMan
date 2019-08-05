package edu.umass.cs.automan.adapters.googleads.util

import scala.util.Random

object KeywordList {
  def keywords(): List[String] = {
    List("science",
      "programming",
      "research",
      "zoology",
      "analysis",
      "search",
      "study",
      "inquiry",
      "scientist",
      "survey",
      "explore",
      "institute",
      "scientific",
      "researcher",
      "studies",
      "searcher",
      "explorers",
      "ruby",
      "c++",
      "c#",
      "mit app inventor",
      "learn python",
      "lisp",
      "heap",
      "python programming",
      "online java compiler",
      "coding for kids",
      "visual basic",
      "r programming",
      "computer programming",
      "object oriented programming",
      "programming languages",
      "computer", "computer science", "math",
      "astronomy", "space", "test", "crowdsource", "planets",
      "solar system", "astronaut", "college", "undergraduate", "professor",
      "college", "education", "school", "high school", "knowledge",
      "learning", "earth", "physics", "biology", "chemistry",
      "textbook", "lessons", "student", "psychology")
  }

  def randomWord: String = {
    val dict = scala.io.Source.fromFile("/usr/share/dict/american-english").getLines.toArray

    def word: String = {
      val w = dict(Random.nextInt(dict.length))
      if (w.length < 10) w.replaceAll("'s", "") else word
    }

    word
  }

}
