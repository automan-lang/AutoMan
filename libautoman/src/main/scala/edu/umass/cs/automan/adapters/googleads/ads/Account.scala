package edu.umass.cs.automan.adapters.googleads.ads

import java.util.UUID

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.resources.Customer
import com.google.protobuf.{BoolValue, StringValue}
import edu.umass.cs.automan.adapters.googleads.util.Service._
import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelInfo, LogType}

object Account {
  /**
    * Construct a new Google Ads account and wrapper class
    * @param name A new name for this account
    * @return A new Account class representing a newly created account
    */
  def apply(name: String) : Account = {
    val googleAdsClient = googleClient
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
    val googleAdsClient = googleClient
    val acc = new Account(googleAdsClient)
    acc.load(accountId)
    acc
  }
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

    DebugLog(
      "Created account " + name, LogLevelInfo(), LogType.ADAPTER, UUID.fromString(managerId.toString)
    )

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
  def createCampaign(dailyBudget: BigDecimal, name: String, qID: UUID): Campaign = {
    val camp = Campaign(account_id, dailyBudget, name, qID)
    camp
  }
  //TODO maybe make create methods return an option?
}
