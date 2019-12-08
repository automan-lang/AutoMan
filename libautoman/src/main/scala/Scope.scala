class Scope(val grammar: Map[String, Production], val curPos: Int) { // or string -> string?
  // grammar is grammar scope is associated with
  // curPos is the position in assignment array at which this production was assigned
  var varBindings: Map[String,String] = Map[String, String]()
  var pos: Int = curPos

  def assign(name: String, value: String): Unit = {
    if (grammar contains name){
      varBindings = varBindings + (name -> value) // TODO: recursive bindings
    } else {
      throw new Exception("")
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

  def combineScope(other: Scope): Scope = {
    for(e <- other.getBindings()) varBindings = varBindings + e
    this
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

  def setPos(newPos: Int) = {
    pos = newPos
  }

  def getBindings(): Map[String,String] = varBindings

  def getPos(): Int = pos

}
