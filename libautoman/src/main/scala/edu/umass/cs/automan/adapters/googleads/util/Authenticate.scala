package edu.umass.cs.automan.adapters.googleads.util

import java.io._
import java.net.URI
import java.util.Properties

import com.google.ads.googleads.lib.GoogleAdsClient.Builder.ConfigPropertyKey
import com.google.api.services.script.model.{Content, CreateProjectRequest, File => gFile}
import com.google.auth.oauth2.{ClientId, UserAuthorizer}
import com.google.common.collect.ImmutableList
import edu.umass.cs.automan.adapters.googleads.ads.Account
import edu.umass.cs.automan.adapters.googleads.forms.Form
import edu.umass.cs.automan.adapters.googleads.util.Service._

import scala.io.Source
import scala.io.StdIn.readLine
import scala.collection.JavaConverters._

object Authenticate {
  // initialization routines
  def setup(): Unit = {

    println("Getting started with AdForm:\n" +
      "   Set up a Google Cloud Platform project at https://console.cloud.google.com/\n" +
      "   Go to Select a Project > New Project and fill out 'Project name' and 'Location'")
    print("Enter your Cloud Project Number (under Project Info in the Cloud Console): ")
    val gcp_Num = readLine()

    println("\nSetting up Ads API:\n" +
      "   Make a new Google Ads Manager account (if you do not have one): https://ads.google.com/home/tools/manager-accounts/\n" +
      "   Go to Tools > API Center and fill out the necessary fields to create a token\n" +
      "   This will give you a Test Account developer token. Apply for basic access in the API Center")
    print("Enter your Manager Account ID: ")
    val m_ID = readLine()
    print("Enter your Developer Token: ")
    val dev_token = readLine()

    println("\nGo to https://console.developers.google.com/ and Enable the Google Ads and Apps Script APIs")
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")

    println("\nCreating credentials\n" +
      "   Developer Console > Credentials > Create Credentials > OAuth Client ID > Configure consent screen\n" +
      "   Enter an 'Application Name' and press Save\n" +
      "   Choose 'Other' for Application type and press Create\n" +
      "   Locate the client ID and secret")
    try { buildAdsProperties(properties_path, m_ID, dev_token) } //starts with "enter your client id
    catch {
      case _ : com.google.api.client.http.HttpResponseException =>
        println("Some information was not correct. Try again")
        buildAdsProperties(properties_path,  m_ID, dev_token)
      case _ : Throwable => println("Properties build failed")
    }

    val project_service = service.projects()

    val project = project_service.create(new CreateProjectRequest()
      .setTitle("AutoMan"))
      .execute()

    // Write script id to properties file
    val properties = new Properties
    properties.load(new FileInputStream(new File(properties_path)))
    properties.put("script.id",project.getScriptId)
    properties.store(new FileWriter(new File(properties_path)), null)

    val libPath = "/edu.umass.cs.automan.adapters.googleads.util/library.gs"
    val setup_file = new gFile()
      .setName("Library")
      .setType("SERVER_JS")
      .setSource(Source.fromFile(getClass.getResource(libPath).getFile).mkString)

    val content: Content = new Content().setFiles(List(setup_file, config).asJava)
    project_service.updateContent(Service.script_id, content).execute()

    println(s"\nFinish setting up the project here: https://script.google.com/d/$script_id/edit\n" +
      "1) Run any function then authorize the app in the following dialog\n" +
      s"2) Add your Cloud Project Number '$gcp_Num' to Resources > Cloud Platform project\n" +
      "3) Publish > Deploy as API executable")
    do print("Ready to continue? (y/n): ") while (readLine().toLowerCase() != "y")

    println("Test form: " + Form("authenticate-test").getPublishedUrl)
    println(s"\nAll done. Your ad production account ID is: ${Account("AutoMan").account_id}")
  }

  // Generates the client ID and client secret from the Google Cloud Console
  private def buildAdsProperties(path: String, managerId: String, developerToken: String): Unit = {
    val SCOPES = ImmutableList.builder[String].add("https://www.googleapis.com/auth/adwords",
      "https://www.googleapis.com/auth/script.projects",
      "https://www.googleapis.com/auth/script.deployments",
      "https://www.googleapis.com/auth/forms").build

    val CALLBACK_URI = "urn:ietf:wg:oauth:2.0:oob"

    print("Enter your client ID: ")
    val clientId = readLine()
    print("Enter your client secret: ")
    val clientSecret = readLine()

    val userAuthorizer = UserAuthorizer.newBuilder.setClientId(ClientId.of(clientId, clientSecret))
      .setScopes(SCOPES).setCallbackUri(URI.create(CALLBACK_URI)).build

    val authorizationUrl = userAuthorizer.getAuthorizationUrl(null, null, null)
    printf("Open this link to authorize AutoMan:%n%s%n", authorizationUrl)
    // Waits for the authorization code.
    print("Go to advanced > Go to AutoMan and allow permissions. Type the code you received here: ")
    // Reading from stdin, so default charset is appropriate
    @SuppressWarnings(Array("DefaultCharset"))
    val authorizationCode = new BufferedReader(new InputStreamReader(System.in)).readLine
    // Exchanges the authorization code for credentials and print the refresh token.
    val userCredentials = userAuthorizer.getCredentialsFromCode(authorizationCode, null)
    //printf("Your refresh token is: %s%n. You can ignore this", userCredentials.getRefreshToken)

    // Inputs property file values
    val adsProperties = new Properties
    adsProperties.put(ConfigPropertyKey.CLIENT_ID.getPropertyKey, clientId)
    adsProperties.put(ConfigPropertyKey.CLIENT_SECRET.getPropertyKey, clientSecret)
    adsProperties.put(ConfigPropertyKey.REFRESH_TOKEN.getPropertyKey, userCredentials.getRefreshToken)
    adsProperties.put(ConfigPropertyKey.DEVELOPER_TOKEN.getPropertyKey, developerToken)
    adsProperties.put(ConfigPropertyKey.LOGIN_CUSTOMER_ID.getPropertyKey, managerId)
    adsProperties.store(new FileWriter(new File(path)), null)
  }

  def scriptRevamp(): Unit = {
    try {
      val tokens = new File(tokens_path)
      tokens.listFiles().foreach(_.delete())
      tokens.delete()
    }
    catch { case  _ : Throwable => }
    service
  }
}
