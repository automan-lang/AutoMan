class Scope(var varBindings: Map[String,String], val grammar: Map[String, Production]) { // or string -> string?
  def assign(name: String, value: String): Unit = {
    if (grammar contains name) varBindings = varBindings + (name -> value)
  }

  // Look up what string a nonterminal is bound to
  def lookup(name: String): String = {
    varBindings(name)
  }

}
