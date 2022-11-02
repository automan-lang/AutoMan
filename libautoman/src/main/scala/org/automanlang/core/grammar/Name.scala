package org.automanlang.core.grammar

import Expand.KCount

class Name(_text: String, _depth: Option[Int]) { //, _index: Option[Int]
  def text: String = _text
  def depth: Option[Int] = _depth

  // Helper method to check if name is epsilon (todo put this in a type somewhere)
  def isEpsilon(): Boolean = {
    if(text == "Epsilon") true
    else false
  }

  // Helper method to generate a Name at depth i
  def freshName(kc: KCount, i: Int): (Name, KCount) = {
    if(i <= 0) {
      (new Name("Epsilon", None), kc)
    } else {
      text match {
        case "Start" => {
          val newKC = kc + ("Start" -> 0)
          (this, newKC)
        }
        case n => { // update count
          val newKC = if(!kc.contains(n)) kc + (n -> 0) else kc + (n -> (kc(n) + 1))
          val j = newKC(n) // get i
          if(j < i) (new Name(n, Some(j)), newKC) // return new name with depth k if still going
          else (new Name("Epsilon", None), newKC) // else epsilon
        }
      }
    }
  }

  def fullname(): String = {
    (text, depth) match {
      case (n, Some(i)) => n + "_" + i.toString
      case (n, None) => n
    }
  }

  override def hashCode(): Int = {
    depth match {
      case Some(value) => text.hashCode + value
      case None => text.hashCode
    }
  }

  def canEqual(a: Any): Boolean = a.isInstanceOf[Name]

  override def equals(that: Any): Boolean = {
    that match {
      case that: Name => {
        that.canEqual(this) &&
          this.text == that.text &&
          this.depth == that.depth
      }
      case _ => false
    }
  }
}
