package edu.umass.cs.automan.core.question

import edu.umass.cs.automan.core.grammar.Grammar

abstract class QuestionOption(val question_id: Symbol, val question_text: String, val question_grammar: Grammar)
