package edu.umass.cs.automan.adapters.googleads.ads

import java.io.File

object Account {
  /**
    * Construct a new Google Ads account and wrapper class
    * @param name A new name for this ad group
    * @return A new Account class representing a newly created account
    */
  def apply(name: String) : Account = {
    val googleAdsClient = client()
    val acc = new Account(googleAdsClient: GoogleAdsClient)
    acc.build(name, googleAdsClient.getLoginCustomerId.longValue)
    acc
  }
  /**
    * Construct a new wrapper class for an existing account
    * @param accountId The ID of the account to be loaded, found in the format xxx-xxx-xxxx
    * @return A new Account wrapper class representing an existing Google Ads account
    */
  def apply(accountId: Long) : Account = {
    val googleAdsClient = client()
    val acc = new Account(googleAdsClient)
    acc.load(accountId)
    acc
  }

  /**
    * Build a Google Ads Client to access the manager account
    * @param path Path to the properties file directory
    * @return A Google Ads Client built from properties file
    */
  def client(path : String = "credentials/ads.properties") : GoogleAdsClient =  {
    val propertiesFile = new File(path)
    GoogleAdsClient.newBuilder.fromPropertiesFile(propertiesFile).build()
  } //Build a google client
}

class Account(googleAdsClient: GoogleAdsClient){


  private var _account_id : Option[Long] = None

  def account_id : Long = _account_id match {case Some(c) => c case None => throw new Exception("Account not initialized")}

  private def load(id: Long) : Unit = {
    _account_id = Some(id)
  }

  private def build(name: String, managerId : Long) : Unit = {
    val customerServiceClient =
    googleAdsClient.getLatestVersion.createCustomerServiceClient() //Open customer service client
    val response =
    customerServiceClient.createCustomerClient(managerId.toString, Customer.newBuilder() //Build and creates new account (aka customer client)
      .setDescriptiveName(StringValue.of(name))
      .setTestAccount(BoolValue.of(true))
      .setCurrencyCode(StringValue.of("USD"))
      .setTimeZone(StringValue.of("America/New_York"))
      .build())

    println("Created account: " + name)

    //Save fields
    _account_id = Some(customerServiceClient.getCustomer(response.getResourceName).getId.getValue)
    customerServiceClient.shutdown()
  }

  /**
    * Create a new campaign under this account
    * @param dailyBudget The daily budget of created campaign, in dollars.
    * @param name The name of the campaign to be created.
    * @return An AdCampaign wrapper class with a newly created campaign.
    */
  //Create new campaign under this account
  def createCampaign(dailyBudget: BigDecimal, name: String): Campaign = {
    val camp = Campaign(account_id, dailyBudget, name)
    camp
  }
  //TODO maybe make create methods return an option?
}
