package log

import java.io.{BufferedWriter, File, FileWriter}

class CSV(filename: String) {
  val file = new File(filename)
  val log = new BufferedWriter(new FileWriter(file))

  def addRow(elems: String*) {
    log.write(elems.mkString(",") + String.format("%n"))
  }
  def close() { log.flush(); log.close() }

}
