/*

expr ::= seq '|' expr
       | seq

seq ::=  rep seq
       | rep

rep ::=  atom '*'
       | atom '+'
       | atom '?'
       | atom '{' num '}'
       | atom '{' num ',' '}'
       | atom '{' num ',' num '}'

atom ::= '(' expr ')'
       | '[^' charclasslist ']'
       | '[' charclasslist ']'
       | char

num ::= /[0-9]+/

char ::= /[^\\?|*+{}\[\^\]()$]|\\./

charclasslist ::= charclass charclasslist | charclass

charclass ::= chartype '-' chartype
            | chartype

chartype ::= /[^\\\^\[\]]|\\./

*/
package edu.umass.cs.automan.core.strategy

import scala.math._

object RegexParser {
	class UnboundedInputException extends Exception
	
	def MatchExpr(input: String): Option[(Int, Int)] = {
		MatchSeq(input) match {
			case Some((l, c)) => {
				if(input.length > l && input(l) == '|') {
					MatchExpr(input.substring(l+1)) match {
						case Some((l2, c2)) => Some((l+1+l2, c+c2))
						case None => None
					}
				} else {
					Some((l, c))
				}
			}
			case None => None
		}
	}
	
	def MatchSeq(input: String): Option[(Int, Int)] = {
		MatchRep(input) match {
			case Some((l, c)) => {
				MatchSeq(input.substring(l)) match {
					case Some((l2, c2)) => Some((l+l2, c*c2))
					case None => Some((l, c))
				}
			}
			case None => None
		}
	}
	
	def MatchRep(input: String): Option[(Int, Int)] = {
		MatchAtom(input) match {
			case Some((l, c)) => {
				if(input.length > l && input(l) == '*') {
					//Some((l+1, "zero or more "+s))
					throw new UnboundedInputException
					
				} else if(input.length > l && input(l) == '+') {
					//Some((l+1, "one or more "+s))
					throw new UnboundedInputException
					
				} else if(input.length > l && input(l) == '?') {
					Some((l+1, c+1))
					
				} else if(input.length > l && input(l) == '{') {
					MatchNum(input.substring(l+1)) match {
						case Some((l2, n1)) => {
							if(input.length > l+l2+1 && input(l+l2+1) == '}') {
								Some((l+l2+2, pow(c, n1).toInt))
								
							} else if(input.length > l+l2+1 && input(l+l2+1) == ',') {
								MatchNum(input.substring(l+l2+2)) match {
									case Some((l3, n2)) => {
										if(input.length > l+l2+l3+2 && input(l+l2+l3+2) == '}') {
											Some((l+l2+l3+3, (n1 to n2).map(x => pow(c, x)).reduce((x,y) => x+y).toInt))
										} else {
											Some((l, c))
										}
									}
									case None => {
										if(input.length > l+l2+2 && input(l+l2+2) == '}') {
											//Some((l+l2+2, s2+" or more "+s))
											throw new UnboundedInputException
										} else {
											Some((l, c))
										}
									}
								}
							} else {
								Some((l, c))
							}
						}
						case None => None
					}
				} else {
					Some((l, c))
				}
			}
			case None => None
		}
	}
	
	def MatchAtom(input: String): Option[(Int, Int)] = {
		if(input.startsWith("(")) {
			MatchExpr(input.substring(1)) match {
				case Some((l, c)) => {
					if(input.length > l+1 && input(l+1) == ')') {
						Some((l+2, c))
					} else {
						None
					}
				}
				case None => None
			}
			
		} else if(input.startsWith("[^")) {
			MatchCharClassList(input.substring(2)) match {
				case Some((l, c)) => {
					if(input.length > l+2 && input(l+2) == ']') {
						Some((l+3, charSetSize - c))
					} else {
						None
					}
				}
				case None => None
			}
			
		} else if(input.startsWith("[")) {
			MatchCharClassList(input.substring(1)) match {
				case Some((l, c)) => {
					if(input.length > l+1 && input(l+1) == ']') {
						Some((l+2, c))
					} else {
						None
					}
				}
				case None => None
			}
			
		} else {
			MatchChar(input)
		}
	}
	
	def MatchChar(input: String): Option[(Int, Int)] = {
		"""[^\\?|*+{}\[\^\]()$]|\\.""".r.findPrefixOf(input) match {
			case Some(s) => Some((s.length, charCount(s)))
			case None => None
		}
	}
	
	def MatchNum(input: String): Option[(Int, Int)] = {
		"""[0-9]+""".r.findPrefixOf(input) match {
			case Some(s) => Some((s.length, s.toInt))
			case None => None
		}
	}
	
	def MatchCharClassList(input: String): Option[(Int, Int)] = {
		MatchCharClass(input) match {
			case Some((l1, c1)) => {
				MatchCharClassList(input.substring(l1)) match {
					case Some((l2, c2)) => Some((l1+l2, c1+c2))
					case None => Some((l1, c1))
				}
			}
			case None => None
		}
	}
	
	def MatchCharClass(input: String): Option[(Int, Int)] = {
		MatchCharType(input) match {
			case Some((l1, c1)) => {
				if(input.length > l1 && input(l1) == '-') {
					MatchCharType(input.substring(l1+1)) match {
						case Some((l2, c2)) => {
							if(c1 == 1 && c2 == 1) {
								Some((l1+1+l2, 1 + charIndex(input.substring(l1+1,l1+l2+1)) - charIndex(input.substring(0, l1))))
							} else {
								Some((l1, c1))
							}
						}
						case None => Some((l1, c1))
					}
				} else {
					Some((l1, c1))
				}
			}
			case None => None
		}
	}
	
	def MatchCharType(input: String): Option[(Int, Int)] = {
		"""[^\\\^\[\]]|\\.""".r.findPrefixOf(input) match {
			case Some(s) => Some((s.length, charTypeCount(s)))
			case None => None
		}
	}
	
	val charSetSize = 256
	
	def charIndex(input: String): Int = {
		if(input.length == 1) input(0)
		else {
			input match {
				case "\\0" => '\0'
				case "\\n" => '\n'
				case "\\f" => '\f'
				case "\\r" => '\r'
				case "\\t" => '\t'
				case "\\(" => '('
				case "\\)" => ')'
				case "\\[" => '['
				case "\\]" => ']'
				case "\\{" => '{'
				case "\\}" => '}'
				case "\\^" => '^'
				case "\\$" => '$'
				case "\\." => '.'
				case "\\\\" => '\\'
				//case "\\v" => '\v'
				case _ => 0
			}
		}
	}
	
	def charCount(input: String): Int = {
		input match {
			case "." => charSetSize
			case _ => charTypeCount(input)
		}
	}
	
	def charTypeCount(input: String): Int = {
		if(input.length == 1) 1
		else {
			input match {
				// Digit
				case "\\d" => 10
			
				// Not a digit
				case "\\D" => charSetSize - 10
			
				// Whitespace
				case "\\s" => 6
			
				// Not whitespace
				case "\\S" => charSetSize - 6
			
				// Escaped characters
				case "\\0" => 1
				case "\\n" => 1
				case "\\f" => 1
				case "\\r" => 1
				case "\\t" => 1
				case "\\v" => 1
				case _ => throw new UnboundedInputException
			}
		}
	}
	
	def main(args: Array[String]) {
		args.foreach(r =>
			try {
				MatchExpr(r) match {
					case Some((l, s)) => {
						if(l == r.length) {
							println(s)
						} else {
							println("Parse failed to consume \""+r.substring(l)+"\"")
						}
					}
					case None => println("Nope!")
				}
			} catch {
				case e: UnboundedInputException => println("Regular expression has unbounded input size")
			}
		)
	}
}
