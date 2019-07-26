package edu.umass.cs.automan.adapters.googleads.forms

import scala.collection.JavaConverters._
import com.google.api.services.script.model.ExecutionRequest
import com.google.api.services.script.model._
import edu.umass.cs.automan.adapters.googleads.util.Service._

object Form {
  /**
    * Construct a new form and wrapper class
    * @param title A new title for this form
    * @return A new Form wrapper class representing a newly created form
    */
  def apply(title: String): Form = {
    val f: Form = new Form()
    f.build(title)
    f
  }
  /**
    * Construct a new wrapper class for an existing form
    * @param id The ID of the form to be loaded
    * @return A new Form wrapper class representing an existing form
    */
  def apply(id: Symbol): Form = {
    val f: Form = new Form()
    f.load(id.toString().drop(1))
    f
  }
}

class Form() {

  protected var _id : Option[String] = None
  def id: String = _id match {case Some(s) => s; case None => throw new Exception("Form not initialized")}

  // creates a new form, saving the id
  private def build(title : String) : Unit = {
    val form_request = new ExecutionRequest()
      .setFunction("addForm")
      .setParameters(List(title.asInstanceOf[AnyRef]).asJava)
    val form_op = service.scripts()
      .run(script_id, form_request)
      .execute()
    _id = Some(form_op.getResponse.get("result").toString)
  }

  private def load(id : String) : Unit = {
    _id = Some(id)
  }

  //----------- FORM UTILITIES --------------------------------------------------------------------


  // performs an execution request with no returned response
  def formRequest(func: String, params: java.util.List[AnyRef]): Unit = {
    val request = new ExecutionRequest()
      .setFunction(func)
      .setParameters(params)
    val op: Operation= service.scripts()
      .run(script_id, request)
      .execute()

    val err = op.getError
    err match {
      case null => op.getResponse.get("result").toString
      case _ => throw ScriptError(err.getMessage)
    }
  }

  // performs an execution request that returns a String
  def formResponse(func: String, params: java.util.List[AnyRef]): String = {
    val request = new ExecutionRequest()
      .setFunction(func)
      .setParameters(params)
    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = op.getError
    err match {
      case null => op.getResponse.get("result").toString
      case _ => throw ScriptError(err.getMessage)
    }
  }

  // return item (question) id
  def addQuestion(question_type: String, params: java.util.List[AnyRef]): String = {
    formResponse(question_type, params)
    //    println(s"$question_type question '$text' created")
  }

  // returns a List of all responses to the form
  def getResponses[T]: List[T] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getResponses")
      .setParameters(List(id.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = op.getError
    err match {
      case null => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
      case _ => throw ScriptError(err.getMessage)
    }
  }

  // returns List of responses to a question
  def getItemResponses[T](item_id: String): List[T] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getItemResponses")
      .setParameters(List(id.asInstanceOf[AnyRef],item_id.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = op.getError
    err match {
      case null => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
      case _ => throw ScriptError(err.getMessage)
    }
  }

  // get the url of the published form
  def getPublishedUrl: String = {
    formResponse("getPublishedUrl", List(id.asInstanceOf[AnyRef]).asJava)
  }

  // get the editor-enabled url of the form
  def getEditUrl: String = {
    formResponse("getEditUrl", List(id.asInstanceOf[AnyRef]).asJava)
  }

  //---------- FORM ADD-ONS -----------------------------------------------------------------------

  def addImage(url: String,
               title: String = "",
               help_text: String = ""): Unit = {
    formRequest("addImage", List(id, url, title, help_text).map(_.asInstanceOf[AnyRef]).asJava)
  }

  // sets a description for the form, shown at the top
  def setDescription(description: String): Unit = {
    formRequest("setDescription", List(id, description).map(_.asInstanceOf[AnyRef]).asJava)
  }

  // sets a confirmation message to show to respondents after they finish a form
  def setConfirmation(message: String): Unit = {
    formRequest("setConfirmation", List(id, message).map(_.asInstanceOf[AnyRef]).asJava)
  }

  // randomizes the order of questions
  def setShuffle(): Unit = {
    formRequest("setShuffle", List(id.asInstanceOf[AnyRef]).asJava)
  }

  // shuffles all questions in the form
  def shuffle(): Unit = {
    formRequest("shuffleForm", List(id.asInstanceOf[AnyRef]).asJava)
  }

  // data validation for estimation questions only
  //TODO: only allow these to be called with estimation questions
  def requireNum(item_id: String, help: String = ""): Unit = {
    formRequest("validateNum", List(id, item_id, help).map(_.asInstanceOf[AnyRef]).asJava)
  }

  def requireInt(item_id: String, help: String = ""): Unit = {
    formRequest("validateInt", List(id, item_id, help).map(_.asInstanceOf[AnyRef]).asJava)
  }

  def requireGreater(item_id: String, min: Double, help: String = ""): Unit = {
    formRequest("validateGreater", List(id, item_id, help, min).map(_.asInstanceOf[AnyRef]).asJava)
  }

  def requireLess(item_id: String, max: Double, help: String = ""): Unit = {
    formRequest("validateLess", List(id, item_id, help, max).map(_.asInstanceOf[AnyRef]).asJava)
  }

  def requireRange(item_id: String, min: Double, max: Double, help: String = ""): Unit = {
    formRequest("validateRange", List(id, item_id, help, min, max).map(_.asInstanceOf[AnyRef]).asJava)
  }

  def requireRegex(item_id: String, regex: String, help: String = ""): Unit = {
    formRequest("validateRegex", List(id, item_id, help, regex).map(_.asInstanceOf[AnyRef]).asJava)
  }

}