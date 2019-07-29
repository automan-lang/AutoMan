package edu.umass.cs.automan.adapters.googleads.util

import java.io._
import java.util.Properties

import scala.collection.JavaConverters._
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver
import com.google.api.client.googleapis.auth.oauth2.{GoogleAuthorizationCodeFlow, GoogleClientSecrets}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.services.script.Script
import com.google.api.services.script.model.{File => gFile}

object Service {
  protected[util] val credentials_json_path = "credentials/credentials.json"
  protected[util] val properties_path = "credentials/ads.properties"
  protected[util] val tokens_path = "credentials/tokens"

  protected[util] val json_factory: JacksonFactory = JacksonFactory.getDefaultInstance

  protected[util] val scopes: java.util.List[String] = List("https://www.googleapis.com/auth/script.projects",
    "https://www.googleapis.com/auth/script.deployments",
    "https://www.googleapis.com/auth/script.external_request",
    "https://www.googleapis.com/auth/forms").asJava
  protected[util] val config: gFile = new gFile()
    .setName("appsscript")
    .setType("JSON")
    .setSource("{ \"timeZone\":\"America/New_York\",\n" +
      "  \"oauthScopes\": [\"https://www.googleapis.com/auth/script.projects\",\n" +
      "                    \"https://www.googleapis.com/auth/script.deployments\",\n" +
      "                    \"https://www.googleapis.com/auth/script.external_request\",\n" +
      "                    \"https://www.googleapis.com/auth/forms\"],\n" +
      "  \"exceptionLogging\":\"STACKDRIVER\" }")


  /**
    * Build a Google Ads Client to access the manager account
    * @param path Path to the properties file directory
    * @return A Google Ads Client built from properties file
    */
  def googleClient : GoogleAdsClient =  {
    val propertiesFile = new File(properties_path)
    GoogleAdsClient.newBuilder.fromPropertiesFile(propertiesFile).build()
  } //Build a google client

  // Accessors
  def script_id: String = {
    val properties = new Properties
    properties.load(new FileInputStream(new File(properties_path)))
    properties.getProperty("script.id")
  }

  def client_id: String = {
      val properties = new Properties
      properties.load(new FileInputStream(new File(properties_path)))
      properties.getProperty("api.googleads.clientId")
  }

  def client_secret: String = {
      val properties = new Properties
      properties.load(new FileInputStream(new File(properties_path)))
      properties.getProperty("api.googleads.clientSecret")
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
    * @return A Credential object
    */
  protected[util] def getCredentials(HTTP_TRANSPORT: NetHttpTransport): Credential = {
    // Load client secrets

    val details = new GoogleClientSecrets.Details()
      .setAuthUri("https://accounts.google.com/o/oauth2/auth")
      .setClientId(client_id)
      .setClientSecret(client_secret)
      .setRedirectUris(java.util.Arrays.asList("urn:ietf:wg:oauth:2.0:oob","http://localhost"))
      .setTokenUri("https://oauth2.googleapis.com/token")
    val clientSecrets = new GoogleClientSecrets().setInstalled(details)

    // Build flow and trigger user authorization request
    val flow = new GoogleAuthorizationCodeFlow.Builder(HTTP_TRANSPORT, json_factory, clientSecrets, scopes)
      .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokens_path)))
      .setAccessType("offline")
      .build()
    val receiver = new LocalServerReceiver.Builder().setPort(8888).build
    new AuthorizationCodeInstalledApp(flow, receiver).authorize("user")
  }
}
