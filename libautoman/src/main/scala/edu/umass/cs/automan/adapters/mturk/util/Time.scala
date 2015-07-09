package edu.umass.cs.automan.adapters.mturk.util

import java.text.SimpleDateFormat
import java.util.Date

object Time {
  private val MT_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")

  // handy converters
  protected def acceptTimeFromXML(x: scala.xml.Node) : Date = {
    MT_DATE_FORMAT.parse((x \\ "AcceptTime").text)
  }
  protected def submitTimeFromXML(x: scala.xml.Node) : Date = {
    MT_DATE_FORMAT.parse((x \\ "SubmitTime").text)
  }
}
