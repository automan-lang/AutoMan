package edu.umass.cs.automan.adapters.googleads.util

import java.io._
import java.net.URI
import java.util.Properties

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.lib.GoogleAdsClient.Builder.ConfigPropertyKey
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.services.script.Script
import com.google.api.services.script.model.{File=>gFile,Content, CreateProjectRequest}
import com.google.auth.oauth2.{ClientId, UserAuthorizer}
import com.google.common.collect.ImmutableList
import edu.umass.cs.automan.adapters.googleads.ads.Account
import edu.umass.cs.automan.adapters.googleads.util.Service.{config, getCredentials, json_factory, properties_path}

import scala.io.Source
import scala.io.StdIn.readLine
import scala.collection.JavaConverters._

object Authenticate {

  private def buildAdsProperties(path: String): Unit = { // Generates the client ID and client secret from the Google Cloud Console:
    // https://console.cloud.google.com
    val SCOPES = ImmutableList.builder[String].add("https://www.googleapis.com/auth/adwords",
      "https://www.googleapis.com/auth/script.projects",
      "https://www.googleapis.com/auth/script.deployments",
      "https://www.googleapis.com/auth/forms").build

    val CALLBACK_URI = "urn:ietf:wg:oauth:2.0:oob"

    printf("Enter your client ID:%n")
    val clientId = readLine()
    printf("Enter your client secret:%n")
    val clientSecret = readLine()
    printf("Enter your manager account ID:%n")
    val managerId = readLine()
    printf("Enter your developer token:%n")
    val developerToken = readLine()

    val userAuthorizer = UserAuthorizer.newBuilder.setClientId(ClientId.of(clientId, clientSecret)).setScopes(SCOPES).setCallbackUri(URI.create(CALLBACK_URI)).build
    val authorizationUrl = userAuthorizer.getAuthorizationUrl(null, null, null)
    printf("Paste this url in your browser:%n%s%n", authorizationUrl)
    // Waits for the authorization code.
    println("Go to advanced and allow permissions. Type the code you received here: ")
    @SuppressWarnings(Array("DefaultCharset")) val authorizationCode = // Reading from stdin, so default charset is appropriate.
      new BufferedReader(new InputStreamReader(System.in)).readLine
    // Exchanges the authorization code for credentials and print the refresh token.
    val userCredentials = userAuthorizer.getCredentialsFromCode(authorizationCode, null)
    printf("Your refresh token is: %s%n", userCredentials.getRefreshToken)
    // Prints the configuration file contents.
    val adsProperties = new Properties
    adsProperties.put(ConfigPropertyKey.CLIENT_ID.getPropertyKey, clientId)
    adsProperties.put(ConfigPropertyKey.CLIENT_SECRET.getPropertyKey, clientSecret)
    adsProperties.put(ConfigPropertyKey.REFRESH_TOKEN.getPropertyKey, userCredentials.getRefreshToken)
    adsProperties.put(ConfigPropertyKey.DEVELOPER_TOKEN.getPropertyKey, developerToken)
    adsProperties.put(ConfigPropertyKey.LOGIN_CUSTOMER_ID.getPropertyKey, managerId)
    adsProperties.store(new FileWriter(new File(path)), null)

    def showConfigurationFile(adsProperties: Properties): Unit = {
      println("Verify that the following is good to go:", GoogleAdsClient.Builder.DEFAULT_PROPERTIES_CONFIG_FILE_NAME)
      println("######################## Configuration file start ########################")
      adsProperties.store(System.out, null)
      println("######################## Configuration file end ##########################")
    }

    //showConfigurationFile(adsProperties)
  }

  // initialization routines
  def setup(): Unit = {
    println("Getting started with AdForm:\n" +
      "1) Set up a Google Cloud Platform project at https://console.cloud.google.com/\n" +
      "   Go to Select a Project > New Project and fill out 'Project name' and 'Location'")
    print("Enter your Cloud Project Number (under Project Info in the Cloud Console): ")
    val gcp_Num = readLine()

    println("2) Enable the Google Ads and Apps Script APIs in the Developer Console > Enable APIs and Services\n" +
      "3) Create credentials\n" +
      "   Developer Console > Credentials > Create Credentials > OAuth Client ID > Configure consent screen\n" +
      "   Enter an 'Application Name' and press Save\n" +
      "   Choose 'Other' for Application type and press Create\n" +
      "   Locate the client ID and secret")
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")

    println("\nSetting up Ads API:\n" +
      "4) Make a new Google Ads Manager account\n" +
      "   Go to Tools > API Center and fill out the necessary fields to create a token\n" +
      "   This will give you a Test Account developer token. Apply for basic access in the API Center\n")
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")

    try { buildAdsProperties(properties_path) }
    catch {
      case _ : com.google.api.client.http.HttpResponseException => {
        println("Some information was not correct. Try again")
        buildAdsProperties(properties_path)
      }
      case _ : Throwable => println("Properties build failed")
    }

    println("\nSetting up Apps Script API:\n" +
      "5) Download credentials\n" +
      "   Go to https://console.cloud.google.com/home/ and navigate to APIs & Services > Credentials\n" +
      "   Select the application you created earlier\n" +
      "   Click Download JSON and save the file as credentials.json in your working directory\n")
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")

    setup(project_name = "AutoMan", gcpNum = gcp_Num)
    println("Your production account ID is: " + Account("AutoMan").account_id)
  }

  /**
    * Set up a project that contains a library of Google Form functions
    * @param project_name The name of the project
    * @return nothing
    */
  def setup(project_name: String = "Form Library", gcpNum: String): Unit = {
    val http_transport = GoogleNetHttpTransport.newTrustedTransport()
    val service = new Script.Builder(
      http_transport, json_factory, getCredentials(http_transport))
      .setApplicationName("Forms Test")
      .build()
    val project_service = service.projects()

    val project = project_service.create(new CreateProjectRequest()
      .setTitle(project_name))
      .execute()

    //Write script id to properties file
    val properties = new Properties
    properties.load(new FileInputStream(new File(properties_path)))
    properties.put("script.id",project.getScriptId)
    properties.store(new FileWriter(new File(properties_path)), null)

    println("Project has been added.")

    val setup_file = new gFile()
      .setName("Library")
      .setType("SERVER_JS")
      .setSource(Source.fromFile("library.txt").mkString)

    val content: Content = new Content().setFiles(List(setup_file, config).asJava)
    project_service.updateContent(Service.script_id, content).execute()

    println(s"Finish setting up the project here: https://script.google.com/d/${Service.script_id()}/edit\n" +
      "1) Run any function and authorize the app in the following dialog\n" +
      s"2) Add your Cloud Project Number '$gcpNum' to Resources > Cloud Platform project\n" +
      "3) Publish > Deploy as API executable")
    // STALL PROGRAM: go to project, authorize, add GCP project number, and deploy as API executable
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")
  }

}
