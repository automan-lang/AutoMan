package edu.umass.cs.automan.adapters.googleads.ads

import com.google.ads.googleads.v2.{common,resources,enums,services,errors,utils}
import resources.{CampaignBudget, CampaignCriterion, LanguageConstantName, Campaign => GoogleCampaign}
import services.{CampaignBudgetOperation, CampaignOperation, GoogleAdsRow, SearchGoogleAdsRequest,CampaignCriterionOperation}

import GoogleCampaign.NetworkSettings
import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.lib.utils.FieldMasks
import common.LanguageInfo
import common.ManualCpc
import enums.AdvertisingChannelTypeEnum.AdvertisingChannelType
import enums.BiddingStrategyTypeEnum.BiddingStrategyType
import enums.BudgetDeliveryMethodEnum.BudgetDeliveryMethod
import enums.CampaignCriterionStatusEnum.CampaignCriterionStatus
import enums.CampaignStatusEnum.CampaignStatus
import errors.GoogleAdsError
import utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.{BoolValue, Int64Value, StringValue}

import edu.umass.cs.automan.adapters.googleads.util.Service._
import edu.umass.cs.automan.core.logging._

import scala.collection.JavaConverters._
import scala.io.StdIn.readLine
import scala.math.BigDecimal.RoundingMode
import scala.util.Random

import java.util.UUID


object Campaign {
  //Construct a new campaign and wrapper class
  def apply(accountId: Long,
            dailyBudget: BigDecimal,
            name: String,
            qID: UUID): Campaign = {

    val googleAdsClient = googleClient

    val camp: Campaign = new Campaign(googleAdsClient, accountId, qID)
    camp.build(dailyBudget, name)
  }

  //Construct a new wrapper class for an existing campaign
  def apply(accountId: Long,
            campId: Long,
            qID: UUID): Campaign = {

    val googleAdsClient = googleClient

    val camp: Campaign = new Campaign(googleAdsClient, accountId, qID)
    camp.load(accountId,campId)
  }
}

class Campaign(googleAdsClient: GoogleAdsClient, accountID: Long, qID: UUID) {

  private var _campaign_id: Option[Long] = None
  private var _budget_id: Option[Long] = None
  private var _name: Option[String] = None


  def campaign_id: Long = _campaign_id match {
    case Some(id) => id
    case None => throw new Exception("Campaign ID not initialized")
  }

  def name: String = _name match {
    case Some(n) => n
    case None => throw new Exception("Campaign name not initialized")
  }

  def budget_id: Long = _budget_id match {
    case Some(id) => id
    case None => throw new Exception("Budget ID not initialized")
  }

  def budget_amount: BigDecimal = {
    val qResponse: List[GoogleAdsRow] = queryFilter(
      "campaign_budget.amount_micros",
      "campaign_budget",
      List(s"campaign_budget.id = $budget_id",s"customer.id = $accountID")
    ).toList

    BigDecimal(qResponse
      .head
      .getCampaignBudget
      .getAmountMicros
      .getValue*2) / BigDecimal(1000000)
  }

  def is_paused: Boolean = {
    val qResponse: List[GoogleAdsRow] = query(
      "campaign.status",
      "campaign").toList

    qResponse
      .head
      .getCampaign
      .getStatus == CampaignStatus.PAUSED
  }

  def cpc: BigDecimal = getAdGroups.head.cpc

  //Load a campaign given an ID (only called in apply)
  private def load(customerID: Long, campaignID: Long): Campaign = {
    // Initialize global variables, querying for name and budget
    _campaign_id = Some(campaignID)

    //Query for campaign name
    _name = Some(query(
      "campaign.id",
      "campaign")
      .head.getCampaign
      .getName
      .getValue)

    //Query for budget
    _budget_id = Some(query(
      "campaign_budget.id",
      "campaign_budget")
      .head
      .getCampaignBudget
      .getId.getValue)

    DebugLog(
      "Loaded campaign " + name + " in account " + accountID, LogLevelInfo(), LogType.ADAPTER, qID
    )

    this
  }

  //Create a new campaign (only called in apply)
  private def build(dailyBudget: BigDecimal, name: String): Campaign = {

    //Make sure that Max doesn't spend the whole endowment
    if (dailyBudget > 50) do {
      println("Are you sure you want to spend >$50? y/n")
    } while (readLine() != "y")
    if (dailyBudget > 100) do {
      println("Are you SURE you want to spend >$100? y/n")
    } while (readLine() != "y")
    if (dailyBudget > 500) do {
      println("Are you ABSOLUTELY SURE you want to spend >$500? y/n")
    } while (readLine() != "y")

    //Name must be less than 30 chars
    if (name.length > 30) {
      DebugLog("Budget '" + name + "' too long. Renamed to " + name.substring(0, 29), LogLevelWarn(), LogType.ADAPTER, qID)
      return build(dailyBudget, name.substring(0, 29))  //Try again
    }

    //Budget must be at least $0.02
    if (dailyBudget < 0.02) {
      DebugLog("Budget less than $0.02 set to $0.02 in account " + accountID, LogLevelWarn(), LogType.ADAPTER, qID)
      return build(0.02, name)   //Try again
    }

    //New budget on backend, save ID
    _budget_id = newBudget(name, dailyBudget)

    //builds campaign networks options (where you want the ad to be published)
    val networkSettings = NetworkSettings.newBuilder
      .setTargetGoogleSearch(BoolValue.of(true))
      .setTargetSearchNetwork(BoolValue.of(true))
      .setTargetContentNetwork(BoolValue.of(true))
      .setTargetPartnerSearchNetwork(BoolValue.of(false)) //this one is not allowed
      .build()

    //New campaign on backend, save name and ID
    newCampaign(name, networkSettings) match {
      case Some((i, n)) =>
        _campaign_id = Some(i)
        _name = Some(n)
        this
      case None => this
    }


  }

  //Create a new budget on the backend, automatically catching fixable errors and retrying
  //catches: duplicate name, budget not a multiple of 0.01
  private def newBudget(bName: String, dB: BigDecimal): Option[Long] = {
    //builds a new budget (not yet created)
    val b = CampaignBudget.newBuilder.setName(StringValue.of(bName))
      .setDeliveryMethod(BudgetDeliveryMethod.ACCELERATED)
      .setAmountMicros(Int64Value.of((dB * (1000000 / 2)).toLong))
      .build()

    //build budget create operation
    val bOp =
      CampaignBudgetOperation.newBuilder
        .setCreate(b)
        .build()

    //open budgets client
    val campaignBudgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient

    //try to create budget with mutate
    try {
      campaignBudgetServiceClient
        .mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp))

      campaignBudgetServiceClient.shutdown()

      //Query for newly created ID
      val budgetID = query(
        "campaign_budget.id",
        "campaign_budget"
      ).head
        .getAccountBudget
        .getId
        .getValue

      Some(budgetID)
    }

    //catch errors in budget creation and retry if necessary
    catch {
      case e: com.google.ads.googleads.v2.errors.GoogleAdsException =>
        campaignBudgetServiceClient.shutdown()  //shutdown client first

        //Look for duplicate name, try again but with some random numbers at the end
        e.getGoogleAdsFailure.getErrorsList.asScala.find({
            _.getErrorCode.getCampaignBudgetError == errors.CampaignBudgetErrorEnum.CampaignBudgetError.DUPLICATE_NAME
        }) match {
          case Some(_) => return newBudget(name + "-" + Random.nextInt(1000), dB) //Retry
          case None =>
        }

        //Look for not a multiple of 0.01, try again rounded to hundredth
        e.getGoogleAdsFailure.getErrorsList.asScala.find({
          _.getErrorCode.getCampaignBudgetError == errors.CampaignBudgetErrorEnum.CampaignBudgetError.NON_MULTIPLE_OF_MINIMUM_CURRENCY_UNIT
        }) match {
          case Some(_) => newBudget(name,dB.setScale(2, RoundingMode.CEILING)) //Retry
          case None => throw e  //Probably this should just return None, but much harder to debug that way
        }

        //Possible to continue this pattern with more errors if they show up (only throw/return on the last one)

      //This really should never happen
      case err: Throwable => campaignBudgetServiceClient.shutdown(); throw err
    }
  }

  //Create a new campaign on the backend, automatically catching fixable errors and retrying
  //catches: duplicate name
  //returns campaign name, campaign id
  private def newCampaign(cName: String, networkSettings: NetworkSettings): Option[(Long, String)] = {
    // Build the campaign
    val c = GoogleCampaign.newBuilder
      .setName(StringValue.of(cName))
      .setAdvertisingChannelType(AdvertisingChannelType.SEARCH)
      .setBiddingStrategyType(BiddingStrategyType.MANUAL_CPC)
      .setStatus(CampaignStatus.ENABLED)
      .setManualCpc(ManualCpc.newBuilder.build())
      .setCampaignBudget(StringValue.of(ResourceNames.campaignBudget(accountID, budget_id)))
      .setNetworkSettings(networkSettings)
      .build()

    // Create campaign op
    val cOp =
      CampaignOperation.newBuilder
        .setCreate(c)
        .build()

    //Opens campaign create client
    val campaignServiceClient =
      googleAdsClient.getLatestVersion.createCampaignServiceClient

    //Try to create campaign
    try {
      //Create campaign through mutate
      campaignServiceClient.mutateCampaigns(accountID.toString, ImmutableList.of(cOp))

      DebugLog(
        "Created campaign " + cName + " in account " + accountID, LogLevelInfo(), LogType.ADAPTER, qID
      )

      campaignServiceClient.shutdown()
      Some((queryFilter("campaign.id","campaign",List(s"customer.id = $accountID",s"campaign.name = '$cName'")).head.getCampaign.getId.getValue,
        queryFilter("campaign.name","campaign",List(s"customer.id = $accountID",s"campaign.name = '$cName'")).head.getCampaign.getName.getValue))
    }

    //Catch fixable errors and retry if possible
    catch {
      case e: com.google.ads.googleads.v2.errors.GoogleAdsException =>
        campaignServiceClient.shutdown()

        //Look for duplicate name, add some random numbers to the name if found
        e.getGoogleAdsFailure.getErrorsList.asScala.find({
          error: GoogleAdsError =>
            error.getErrorCode.getCampaignError == com.google.ads.googleads.v2.errors.CampaignErrorEnum.CampaignError.DUPLICATE_CAMPAIGN_NAME
        }) match {
          case Some(_) => newCampaign(name + "-" + Random.nextInt(1000), networkSettings) //Add some random numbers to the end
          case None => throw e //Probably should just return None, but much harder to debug that way
        }

      //Possible to continue this pattern with more errors if they show up (only throw/return on the last one)

      //This should never happen
      case err: Throwable => campaignServiceClient.shutdown(); throw err
    }
  }

  //Create a new ad group and its wrapper class under this campaign -> used when createAd is called
  private def createAdGroup(name: String, cpc: BigDecimal): AdGroup = {
    AdGroup(accountID, campaign_id, name, cpc, qID)
  }

  //Create a new ad under this campaign, automatically making an ad group for it and supplying keywords to add to this ad group
    //title       The first part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
    //subtitle    The second part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
    //description The second line of the ad as it will appear in Google search. Must be < 90 characters
    //url         The url for the ad to link to, which will be automatically shortened by Google when appearing in search results
    //keywords    A list of keywords to associate with the new ad (and its ad group) with broad match setting
  def createAd(title: String,
               subtitle: String,
               description: String,
               url: String,
               keywords: List[String],
               cpc: BigDecimal): Ad = {

      //Creates adgroup and ad under it
    createAdGroup(title,cpc).createAd(title, subtitle, description, url, keywords, qID)
  }

  //Set the daily budget of the campaign, in dollars
  def setBudget(newBudget: BigDecimal): Unit = {
    //Open budgets client
    val bClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient

    //Build campaign
    val b = CampaignBudget.newBuilder
      .setResourceName(ResourceNames.campaignBudget(accountID, budget_id))
      .setAmountMicros(Int64Value.of((newBudget * (1000000 / 2)).toLong))
      .build()

    //Build budget update operation
    val bOp = CampaignBudgetOperation.newBuilder
      .setUpdate(b)
      .setUpdateMask(FieldMasks.allSetFieldsOf(b))
      .build()

    //Set budget through mutate
    bClient.mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp))

    DebugLog(
      "Set budget of campaign " + name + " to $" + newBudget, LogLevelInfo(), LogType.ADAPTER, qID
    )
    bClient.shutdown()
  }

  //Querys campaign for all ad groups then loads them into wrapper classes.
  //Private because ad group is hopefully wholly abstracted away outside of this class
  private def getAdGroups: List[AdGroup] = {
    //Query for ad groups under this campaign
    val l : List[GoogleAdsRow] = query("campaign.id, ad_group.id, ad_group.name","ad_group").toList
    //Get id of all ad groups
    val id = l.map(_.getAdGroup.getId.getValue)
    //Return wrapped
    id.map(AdGroup(accountID, campaign_id, _, qID))
  }

  //Add search keywords to be added to all adgroups under this campaign (with broad match setting).
  def addKeyWords(words: List[String]): Unit = {
    //Can only be done on ad group level
    getAdGroups.foreach(_.addKeyWords(words))

    //Only log the first 5 for sanity
    words.length match {
      case x if x < 5 =>
        DebugLog(
          "Added keywords ot campaign " + name + ":" + words, LogLevelInfo(), LogType.ADAPTER, qID
        )
      case _ =>
        DebugLog(
          "Added keywords ot campaign " + name + ":" + words.splitAt(6)._1, LogLevelInfo(), LogType.ADAPTER, qID
        )
    }

  }

  //Sets the cost per click of all ads in this campaign. Only works if campaign already has ads
  def setCPC(costPerClick: BigDecimal): Unit = {
    //Can only be done on ad group level
    val ag = getAdGroups

    if (ag.isEmpty) {
      DebugLog(
        "Failed to set CPC of campaign " + name + ". Must create ad before setting CPC", LogLevelWarn(), LogType.ADAPTER, qID
      )
    } else {
      ag.foreach(_.setCPC(costPerClick))

      DebugLog(
        "Set CPC of campaign " + name + " to $" + costPerClick, LogLevelInfo(), LogType.ADAPTER, qID
      )
    }
  }


  //Generic method for pausing or resuming campaigns: true if status changed
  private def setStatus(s: CampaignStatus): Boolean = {

    //Don't set status to s if status is already s
    if (query("campaign.id","campaign").head.getCampaign.getStatus == s) {
      false
    } else {
      val client = googleAdsClient.getLatestVersion.createCampaignServiceClient() //opens campaign client

      //Build status set mask
      val c = GoogleCampaign.newBuilder
        .setResourceName(ResourceNames.campaign(accountID, campaign_id))
        .setStatus(s)
        .build()

      //Build campaign update operation from status set mask
      val op = CampaignOperation.newBuilder
        .setUpdate(c)
        .setUpdateMask(FieldMasks.allSetFieldsOf(c))
        .build()

      //Perform mutate operation
      client.mutateCampaigns(accountID.toString, ImmutableList.of(op))

      client.shutdown()
      true
    }
  }

  //Set the status of this campaign to paused. If already paused, return false
  def pause(): Boolean = {
    if (setStatus(CampaignStatus.PAUSED)) {
      DebugLog(
        "Paused campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )
      true
    }
    else false
  }

  //Set the status of this campaign to enabled. If already enabled, return false
  def resume(): Boolean = {
    if (setStatus(CampaignStatus.ENABLED)) {
      DebugLog(
        "Paused campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )
      true
    }
    else false
  }

  //Delete the Google campaign associated with this class. Return true if just now deleted, false if already deleted
  def delete(): Boolean = {
    //Query for campaign status to check if already deleted
    val qResponse: List[GoogleAdsRow] = query("campaign.status","campaign").toList
    if(qResponse.head.getCampaign.getStatus != CampaignStatus.REMOVED) {

      //Open create/delete campaign client
      val csc = googleAdsClient.getLatestVersion.createCampaignServiceClient()

      //Build remove campaign op
      val cOp = CampaignOperation.newBuilder
        .setRemove(ResourceNames.campaign(accountID, campaign_id))
        .build()

      //Remove campaign with mutate
      csc.mutateCampaigns(accountID.toString, ImmutableList.of(cOp))
      DebugLog(
        "Deleted campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )

      //Open create/delete campaign budget client
      val bsc = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()

      //Build remove budget op
      val bOp = CampaignBudgetOperation.newBuilder
        .setRemove(ResourceNames.campaignBudget(accountID, budget_id))
        .build()

      //Remove budget through mutate
      bsc.mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp))

      DebugLog(
        "Deleted budget " + budget_id, LogLevelInfo(), LogType.ADAPTER, qID
      )

      bsc.shutdown()
      csc.shutdown()
      true

    } else false
  }

  //Get all searches that ended in a click on an ad in this campaign
  //NOT ACCURATE FOR UP TO THREE HOUR DELAY DUE TO REPORTING LATENCY
  def searchTerms: List[String] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT search_term_view.search_term FROM search_term_view WHERE campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    var l: List[String] = Nil

    for (googleAdsRow: GoogleAdsRow <- response) {
      l = googleAdsRow.getSearchTermView.getSearchTerm.getValue :: l
    }

    gasc.shutdown()
    l
  }

  //Get the total number of clicks on ads in this campaign
  //NOT ACCURATE FOR UP TO THREE HOUR DELAY DUE TO REPORTING LATENCY
  def clicks: Int = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT metrics.clicks FROM campaign WHERE campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    var l: List[Int] = Nil

    for (googleAdsRow: GoogleAdsRow <- response) {
      l = googleAdsRow.getMetrics.getClicks.getValue.toInt :: l
    }

    gasc.shutdown()
    l.fold(0: Int)(_ + _)
  }

  //Restrict the campaign to only show to English speakers
  def restrictEnglish(): Unit = {
    //Open campaign criterion client
    val agcsc = googleAdsClient.getLatestVersion.createCampaignCriterionServiceClient()

    //Build new english restriction criterion
    val criterion = CampaignCriterion.newBuilder
      .setLanguage(LanguageInfo.newBuilder
        .setLanguageConstant(StringValue.of(LanguageConstantName.format(1000.toString)))
        .build)

      .setStatus(CampaignCriterionStatus.ENABLED)
      .setCampaign(StringValue.of(ResourceNames.campaign(accountID, campaign_id)))
      .build()

    //Build criterion create op
    val op = CampaignCriterionOperation.newBuilder
      .setCreate(criterion)
      .build()

    //Create criterion through mutate
    agcsc.mutateCampaignCriteria(accountID.toString, ImmutableList.of(op))

    agcsc.shutdown()
    DebugLog(
      "Added English language restriction to campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
    )
  }

  //Method to query for info about this campaign: ALL "GET" API CALLS SHOULD BE REPLACED WITH THIS\
  //See https://developers.google.com/google-ads/api/docs/query/interactive-gaql-builder
  private def query(field: String, resource: String): Iterable[GoogleAdsRow] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT $field FROM $resource WHERE customer.id = $accountID AND campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    gasc.shutdown()
    response
  }

  //Method to query for info about this campaign and specify filters
  //See https://developers.google.com/google-ads/api/docs/query/interactive-gaql-builder
  private def queryFilter(field: String, resource: String, filters: List[String]): Iterable[GoogleAdsRow] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT $field FROM $resource WHERE " + filters.mkString(" AND ")

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    gasc.shutdown()
    response
  }

}
