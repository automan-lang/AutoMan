class Scope(val grammar: Map[String, Production]) { // or string -> string?
  var varBindings: Map[String,String] = Map[String, String]()

  def assign(name: String, value: String): Unit = {
    if (grammar contains name){
      varBindings = varBindings + (name -> value)
      //println(s"\nBinding ${name} to ${value}")
    }
    //this.toString()
  }

  // Look up what string a nonterminal is bound to
  def lookup(name: String): String = {
    varBindings(name)
  }

  // Check if a name is already bound to a string
  def isBound(name: String): Boolean = {
    //println(s"\nScope contains ${name}: ${varBindings contains name}")
    varBindings contains name
  }

//  def getBindings(): Map[String, String] = {
//    varBindings
//  }

  override def toString(): String = {
    //varBindings.foreach(println(_.toString()))
    var toRet: String = ""
    for(e <- varBindings) {
      //println(s"${b} -> ${v}")
      toRet += e.toString() + "\n"
    }
    toRet
    //varBindings.toString()
  }

}
