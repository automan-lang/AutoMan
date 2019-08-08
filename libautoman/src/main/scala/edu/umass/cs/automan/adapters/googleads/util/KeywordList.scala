package edu.umass.cs.automan.adapters.googleads.util

import net.sf.extjwnl.data.Synset
import net.sf.extjwnl.dictionary.Dictionary

import scala.util.Random
import scala.collection.JavaConverters._

object KeywordList {
  def keywords(): Set[String] = {
    Set("science",
      "programming",
      "research",
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

  def generateKeywords(num: Int, words: Set[String]) : Set[String] = {
    def syns(s: String, dictionary: Dictionary): Set[String] = {
      val words = dictionary
        .lookupAllIndexWords(s)
        .getIndexWordArray
        .toSet
      val senses: Set[Synset] = words.flatMap(_.getSenses.asScala)
      senses.flatMap(_.getWords.asScala.map(_.getLemma)).filter(!_.contains(" "))
    }

    def genSyns(depth: Int, words: Set[String]): Set[String] = {
      val dictionary = Dictionary.getFileBackedInstance(getClass.getResource("/dict").getPath)

      def genSynsRec(depth: Int, set: Set[String]): Set[String] = {
        if (depth == 0) set
        else genSynsRec(depth - 1, set.flatMap(s => syns(s, dictionary))
        )
      }

      genSynsRec(depth, words)
    }

    def keyGen(depth: Int, words: Set[String]): Set[String] = {
      val keywords = genSyns(1, words)
      if (keywords.size >= num) return keywords.splitAt(num)._1
      if (depth > 4) keyGen(1,keywords ++ Set(randomWord,randomWord,randomWord,randomWord))
      else keyGen(depth + 1, keywords)
    }

    keyGen(1,words)
  }

}
