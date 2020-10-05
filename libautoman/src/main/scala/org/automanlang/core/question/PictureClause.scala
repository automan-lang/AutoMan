package org.automanlang.core.question

import scala.math._

object PictureClause {
  def apply(input: String, allow_empty: Boolean): (String, BigInt) = {
    val (pattern,num_possibilities) = Compile(input)
    if (allow_empty) {
      ("(^" + pattern + "$)|(^(N|n)(A|a)$)", num_possibilities + 1)
    } else {
      ("^" + pattern + "$", num_possibilities)
    }
  }

  private def Compile(input: String): (String, BigInt) = {
    if(input.length == 0) {
      return ("", 1)
    }

    if(input.startsWith("A")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[a-zA-Z]" + regex, count * 26)
      }
    } else if(input.startsWith("B")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[a-zA-Z]?" + regex, count * 27)
      }
    } else if(input.startsWith("9")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[0-9]" + regex, count * 10)
      }
    } else if(input.startsWith("0")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[0-9]?" + regex, count * 11)
      }
    } else if(input.startsWith("X")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[a-zA-Z0-9]" + regex, count * 36)
      }
    } else if(input.startsWith("Y")) {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[a-zA-Z0-9]?" + regex, count * 37)
      }
    } else {
      Compile(input.substring(1)) match {
        case (regex, count) => ("[" + input.substring(0, 1) + "]" + regex, count)
      }
    }
  }
}
