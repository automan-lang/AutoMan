package edu.umass.cs.automan.adapters.googleads.forms

import java.io.{File => jFile, _}
import java.util.Properties

import scala.io.Source
import scala.io.StdIn.readLine
import scala.collection.JavaConverters._
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.script.Script
import com.google.api.services.script.model._

object Project {

  // Instance variables
  private val credentials_json_path = "credentials/credentials.json"
  private val properties_path = "credentials/ads.properties"
  private val tokens_path = "credentials/tokens"

  protected val json_factory: JacksonFactory = JacksonFactory.getDefaultInstance

  protected val scopes: java.util.List[String] = List("https://www.googleapis.com/auth/script.projects",
    "https://www.googleapis.com/auth/script.deployments",
    "https://www.googleapis.com/auth/script.external_request",
    "https://www.googleapis.com/auth/forms").asJava
  protected val config: File = new File()
    .setName("appsscript")
    .setType("JSON")
    .setSource("{ \"timeZone\":\"America/New_York\",\n" +
      "  \"oauthScopes\": [\"https://www.googleapis.com/auth/script.projects\",\n" +
      "                    \"https://www.googleapis.com/auth/script.deployments\",\n" +
      "                    \"https://www.googleapis.com/auth/script.external_request\",\n" +
      "                    \"https://www.googleapis.com/auth/forms\"],\n" +
      "  \"exceptionLogging\":\"STACKDRIVER\" }")

  // Accessors
  def script_id: String = {
    val properties = new Properties
    properties.load(new FileInputStream(new jFile(properties_path)))
    properties.getProperty("script.id")
  }

  def service: Script = {
    val http_transport = GoogleNetHttpTransport.newTrustedTransport()
    new Script.Builder(
      http_transport, json_factory, getCredentials(http_transport))
      .setApplicationName("appscript")
      .build()
  }

  /**
    * Run the specified script function
    * @param HTTP_TRANSPORT Automatically created
    * @param CREDENTIALS_PATH A directory path to the credentials.json file
    * @return A Credential object
    */
  @throws[IOException]
  def getCredentials(HTTP_TRANSPORT: NetHttpTransport,
                     CREDENTIALS_PATH: String = credentials_json_path): Credential = {
    // Load client secrets
    val in: InputStream = new FileInputStream(new jFile(credentials_json_path))
    if (in == null) {
      throw new FileNotFoundException("Resource not found: credentials.json")
    }
    val clientSecrets = GoogleClientSecrets.load(json_factory, new InputStreamReader(in))

    // Build flow and trigger user authorization request
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, json_factory, clientSecrets, scopes)
      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokens_path)))
      .setAccessType("offline")
      .build()
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }

  /**
    * Set up a project that contains a library of Google Form functions
    * @param project_name The name of the project
    * @return nothing
    */
  def setup(project_name: String = "Form Library", gcpNum: String): Unit = {
    val http_transport = GoogleNetHttpTransport.newTrustedTransport()
    val service = new Script.Builder(
      http_transport, json_factory, Project.getCredentials(http_transport))
      .setApplicationName("Forms Test")
      .build()
    val project_service = service.projects()

    val project = project_service.create(new CreateProjectRequest()
      .setTitle(project_name))
      .execute()

    //Write script id to properties file
    val properties = new Properties
    properties.load(new FileInputStream(new jFile(properties_path)))
    properties.put("script.id",project.getScriptId)
    properties.store(new FileWriter(new jFile(properties_path)), null)

    println("Project has been added.")

    val setup_file = new File()
      .setName("Library")
      .setType("SERVER_JS")
      .setSource(Source.fromFile("library.txt").mkString)

    val content: Content = new Content().setFiles(List(setup_file, config).asJava)
    project_service.updateContent(script_id, content).execute()

    println(s"Finish setting up the project here: https://script.google.com/d/$script_id/edit\n" +
      "1) Run any function and authorize the app in the following dialog\n" +
      s"2) Add your Cloud Project Number '$gcpNum' to Resources > Cloud Platform project\n" +
      "3) Publish > Deploy as API executable")
    // STALL PROGRAM: go to project, authorize, add GCP project number, and deploy as API executable
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")
  }

}
