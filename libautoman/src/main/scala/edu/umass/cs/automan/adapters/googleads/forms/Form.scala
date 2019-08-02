package edu.umass.cs.automan.adapters.googleads.forms

import scala.collection.JavaConverters._
import com.google.api.services.script.model.ExecutionRequest
import com.google.api.services.script.model._
import edu.umass.cs.automan.adapters.googleads.util.Service._
import edu.umass.cs.automan.adapters.googleads.ScriptError
import edu.umass.cs.automan.core.logging._

object Form {
  /**
    * Construct a new form and wrapper class
    * @param title A new title for this form
    * @param limit Sets whether the form allows only one response per respondent.
    *              If true, requires respondent to sign in to their Google account.
    * @return A new Form wrapper class representing a newly created form
    */
  def apply(title: String, limit: Boolean = false): Form = {
    val f: Form = new Form()
    f.build(title, limit)
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
  private def build(title : String, limit: Boolean) : Unit = {
    val form_request = new ExecutionRequest()
      .setFunction("addForm")
      .setParameters(List(title, limit).map(_.asInstanceOf[AnyRef]).asJava)
    val form_op = service.scripts()
      .run(script_id, form_request)
      .execute()

    val err = Option(form_op.getError)
    _id = err match {
      case None => {
        DebugLog(
          "Posted form with title: '" + title + "'", LogLevelInfo(), LogType.ADAPTER, null
        )
        Some(form_op.getResponse.get("result").toString)
      }
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
    }
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

    val err = Option(op.getError)
    err match {
      case None =>
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
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

    val err = Option(op.getError)
    err match {
      case None => op.getResponse.get("result").toString
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
    }
  }

  // return item (question) id
  def addQuestion(question_type: String, params: java.util.List[AnyRef]): String = {
    formResponse(question_type, params)
  }

  // returns a List of all responses to the form
  // TODO: rewrite the three get responses to refer to one function
  def getResponses[T]: List[T] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getResponses")
      .setParameters(List(id.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = Option(op.getError)
    err match {
      case None => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
    }
  }

  // returns List of responses to a question,
  // starting from the point we last left off
  def getItemResponses[T](item_id: String, read_so_far: Int = 0): List[T] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getItemResponses")
      .setParameters(List(id,item_id, read_so_far).map(_.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = Option(op.getError)
    err match {
      case None => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
    }
  }

  def getMultiResponses[T] (item_id: String, read_so_far: Int = 0, dim: Int): List[T] = {
    val request: ExecutionRequest = new ExecutionRequest()
      .setFunction("getMultiResponses")
      .setParameters(List(id, item_id, read_so_far, dim).map(_.asInstanceOf[AnyRef]).asJava)

    val op: Operation = service.scripts()
      .run(script_id, request)
      .execute()

    val err = Option(op.getError)
    err match {
      case None => op.getResponse.get("result").asInstanceOf[java.util.ArrayList[T]].asScala.toList
      case Some(e) => throw ScriptError(e.getMessage, e.getDetails.get(0).get("errorMessage").toString)
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

  // TODO: only allow these to be called with estimation questions
  // data validation for estimation questions only
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