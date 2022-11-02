package org.automanlang.core.grammar

import org.automanlang.core.grammar.Expand._

object LindaGrammar {
  def main(args: Array[String]): Unit = {
    val pronouns = Map[String, String](
      "Linda" -> "she",
      "Dan" -> "he",
      "Emmie" -> "she",
      "Xavier the bloodsucking spider" -> "it"
    )

    val articles = Map[String, String](
      "bank teller" -> "a",
      "almond paste mixer" -> "an",
      "tennis scout" -> "a",
      "lawyer" -> "a",
      "professor" -> "a"
    )

    val lindaGrammar = Map[Name, Expression](
      Start -> ref("A"),
      nt("A") -> seq(Array(
        binding(nt("Name")),
        term(" is "),
        binding(nt("Age")),
        term(" years old, single, outspoken, and very bright. "),
        fun(pronouns, nt("Name"), capitalize = true),
        term(" majored in "),
        binding(nt("Major")),
        term(". As a student, "),
        fun(pronouns, nt("Name"), capitalize = false),
        term(" was deeply concerned with issues of "),
        binding(nt("Issue")),
        term(", and also participated in "),
        binding(nt("Demonstration")),
        term(" demonstrations.\nWhich is more probable?\n"),
        ref("Opt1"),
        term("\n"),
        ref("Opt2")
      )),
      nt("Name") -> ch(Array(
        term("Linda"),
        term("Dan"),
        term("Emmie"),
        term("Xavier the bloodsucking spider")
      )),
      nt("Age") -> ch(Array(
        term("21"),
        term("31"),
        term("41"),
        term("51"),
        term("61")
      )),
      nt("Major") -> ch(Array(
        term("chemistry"),
        term("psychology"),
        term("english literature"),
        term("philosophy"),
        term("women's studies"),
        term("underwater basket weaving")
      )),
      nt("Issue") -> ch(Array(
        term("discrimination and social justice"),
        term("fair wages"),
        term("animal rights"),
        term("white collar crime"),
        term("unemployed circus workers")
      )),
      nt("Demonstration") -> ch(Array(
        term("anti-nuclear"),
        term("anti-war"),
        term("pro-choice"),
        term("anti-abortion"),
        term("anti-animal testing")
      )),
      nt("Job") -> ch(Array(
        term("bank teller"),
        term("almond paste mixer"),
        term("tennis scout"),
        term("lawyer"),
        term("professor")
      )),
      nt("Movement") -> ch(Array(
        term("feminist"),
        term("anti-plastic water bottle"),
        term("pro-pretzel crisp"),
        term("pro-metal straw"),
        term("environmental justice")
      )),
      nt("Opt1") -> opt(seq(Array(
        binding(nt("Name")),
        term(" is a "),
        binding(nt("Job")),
        term(".")
      ))),
      nt("Opt2") -> opt(seq(Array(
        binding(nt("Name")),
        term(" is a "),
        binding(nt("Job")),
        term(" and is active in the "),
        binding(nt("Movement")),
        term(" movement.")
      )))
    )

    val lindaQuestionProduction: RadioQuestionProduction = new RadioQuestionProduction(lindaGrammar)
    val text = lindaQuestionProduction.toQuestionText(24375, 2)
//    val text = lindaQuestionProduction.toQuestionText(5625, 2)
    println(text)
  }
}
