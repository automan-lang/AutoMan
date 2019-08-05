package edu.umass.cs.automan.adapters.googleads.ads

import java.util.UUID
import java.util.concurrent.TimeUnit

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.lib.utils.FieldMasks
import com.google.ads.googleads.v2._
import services.CampaignCriterionOperation
import common.LanguageInfo
import resources.{CampaignBudget, CampaignCriterion, LanguageConstantName, Campaign => GoogleCampaign}
import common.ManualCpc
import enums.AdvertisingChannelTypeEnum.AdvertisingChannelType
import enums.BiddingStrategyTypeEnum.BiddingStrategyType
import enums.BudgetDeliveryMethodEnum.BudgetDeliveryMethod
import enums.CampaignCriterionStatusEnum.CampaignCriterionStatus
import enums.CampaignStatusEnum.CampaignStatus
import errors.GoogleAdsError
import resources.Campaign.NetworkSettings
import services.{CampaignBudgetOperation, CampaignOperation, GoogleAdsRow, SearchGoogleAdsRequest}
import utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.{BoolValue, Int64Value, StringValue}

import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.util.Service._
import edu.umass.cs.automan.core.logging._

import scala.io.StdIn.readLine
import scala.math.BigDecimal.RoundingMode
import scala.util.Random


object Campaign {
  /**
    * Construct a new campaign and wrapper class
    * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
    * @param dailyBudget The daily amount to be spent by this account. Limited by month, so a campaign can spend up 30x its daily budget (likely will only spend 2x)
    * @param name A new name for this campaign
    * @return A new AdCampaign wrapper class representing a newly created campaign
    */
  def apply(accountId: Long, dailyBudget: BigDecimal, name: String, qID: UUID): Campaign = {
    val googleAdsClient = googleClient
    val camp: Campaign = new Campaign(googleAdsClient, accountId, qID)
    camp.build(dailyBudget, name)
    camp
  }
  /**
    * Construct a new wrapper class for an existing campaign
    * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
    * @param campId The ID of the campaign to be loaded
    * @return A new AdCampaign wrapper class representing an existing campaign
    */
  def apply(accountId: Long, campId: Long, qID: UUID): Campaign = {
    val googleAdsClient = googleClient
    val camp: Campaign = new Campaign(googleAdsClient, accountId, qID)
    camp.load(accountId,campId)
    camp
  }
}

class Campaign(googleAdsClient: GoogleAdsClient, accountID: Long, qID: UUID) {

  private var _campaign_id : Option[Long] = None
  private var _budget_id : Option[Long] = None
  private var _name : Option[String] = None


  def campaign_id : Long = _campaign_id match {case Some(id) => id case None => throw new Exception("Campaign ID not initialized")}
  def name : String = _name match {case Some(n) => n case None => throw new Exception("Campaign name not initialized")}
  def budget_id : Long = _budget_id match {case Some(id) => id case None => throw new Exception("Budget ID not initialized")}

  def budget_amount : BigDecimal = {
    val client = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()
    val b = client.getCampaignBudget(ResourceNames.campaignBudget(accountID,budget_id))
      .getAmountMicros.getValue * (1000000/2)
    client.shutdown()
    b
  }

  def is_paused : Boolean = {
    val client = googleAdsClient.getLatestVersion.createCampaignServiceClient
    val p = client.getCampaign(ResourceNames.campaign(accountID,campaign_id)).getStatus == CampaignStatus.PAUSED
    client.shutdown()
    p
  }

  def cpc : BigDecimal = getAdGroups(0).cpc

  private def load(customerID: Long, campaignID: Long) : Unit = {
    val campaignServiceClient = googleAdsClient.getLatestVersion.createCampaignServiceClient //open campaigns client
    val budgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient

    val campaign = campaignServiceClient.getCampaign(ResourceNames.campaign(customerID,campaignID))

    // Initialize global variables
    _campaign_id = Some(campaign.getId.getValue)
    _name = Some(campaign.getName.getValue)
    val b = budgetServiceClient.getCampaignBudget(campaign.getCampaignBudget.getValue)
    _budget_id = Some(b.getId.getValue)

    budgetServiceClient.shutdown()
    campaignServiceClient.shutdown()

    DebugLog(
      "Created campaign " + name + " in account " + accountID, LogLevelInfo(), LogType.ADAPTER, qID
    )
  }

  private def build(dailyBudget : BigDecimal, name : String) : Unit = {
    if(dailyBudget > 50) do {println("Are you sure you want to spend >$50? y/n")} while (readLine() != "y")

    if(name.length > 30) {
      DebugLog("Budget '" + name + "' too long. Renamed to " + name.substring(0,29), LogLevelWarn(), LogType.ADAPTER, qID)
      return build(dailyBudget, name.substring(0,29))
    }
    if(dailyBudget < 0.02) {
      DebugLog("Budget less than $0.02 set to $0.02 in account " + accountID, LogLevelWarn(), LogType.ADAPTER, qID)
      return build(0.02,name)
    }
    _budget_id= newBudget(name, dailyBudget)

    def newBudget(bName : String, dB: BigDecimal): Option[Long] = {
      val b = CampaignBudget.newBuilder.setName(StringValue.of(bName)) //builds a new budget (not yet created)
        .setDeliveryMethod(BudgetDeliveryMethod.ACCELERATED)
        .setAmountMicros(Int64Value.of((dB * (1000000 / 2)).toLong))
        .build()

      val bOp =
        CampaignBudgetOperation.newBuilder //builds budget create operation
          .setCreate(b)
          .build()

      val campaignBudgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient //opens budgets client

      //create budget, catching for duplicate name and rounding issues (such a pain)
      try {
        val budgetResponse = campaignBudgetServiceClient.mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp))
        val budget: CampaignBudget = campaignBudgetServiceClient.getCampaignBudget(budgetResponse.getResults(0).getResourceName)

        campaignBudgetServiceClient.shutdown()
        Some(budget.getId.getValue)
      }
      catch {
        case e: com.google.ads.googleads.v2.errors.GoogleAdsException =>
          campaignBudgetServiceClient.shutdown()
          e.getGoogleAdsFailure.getErrorsList.asScala.find({
            error: GoogleAdsError =>
              error.getErrorCode.getCampaignBudgetError == errors.CampaignBudgetErrorEnum.CampaignBudgetError.DUPLICATE_NAME
          }) match {
            case Some(s) => newBudget(name + "-" + Random.nextInt(1000),dB) //Add some random numbers to the end
            case None =>
          }
          e.getGoogleAdsFailure.getErrorsList.asScala.find({
            error: GoogleAdsError =>
              error.getErrorCode.getCampaignBudgetError == errors.CampaignBudgetErrorEnum.CampaignBudgetError.NON_MULTIPLE_OF_MINIMUM_CURRENCY_UNIT
          }) match {
            case Some(s) => newBudget(name,dB.setScale(2, RoundingMode.CEILING))
            case None => None
          }
        case err: Throwable => campaignBudgetServiceClient.shutdown(); None
      }
    }


    val networkSettings = NetworkSettings.newBuilder //builds campaign networks options (where you want ad to be published)
      .setTargetGoogleSearch(BoolValue.of(true))
      .setTargetSearchNetwork(BoolValue.of(true))
      .setTargetContentNetwork(BoolValue.of(true))
      .setTargetPartnerSearchNetwork(BoolValue.of(false))
      .build()

    newCampaign(name) match {
      case Some((i,n)) => _campaign_id = Some(i); _name = Some(n)
      case None => (None,None)
    }
    def newCampaign(cName: String) : Option[(Long,String)] = {
      // Builds the campaign.
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

        val campaignServiceClient =
          googleAdsClient.getLatestVersion.createCampaignServiceClient //opens campaign create client

      try {
      val response =
          campaignServiceClient.mutateCampaigns(accountID.toString, ImmutableList.of(cOp)) //creates campaign through mutate

        DebugLog(
          "Created campaign " + cName + " in account " + accountID, LogLevelInfo(), LogType.ADAPTER, qID
        )

        //Save useful info
        val campaign = campaignServiceClient.getCampaign(response.getResultsList.get(0).getResourceName)
        _campaign_id = Some(campaign.getId.getValue)
        _name = Some(campaign.getName.getValue)

        campaignServiceClient.shutdown()
        Some((campaign.getId.getValue,campaign.getName.getValue))
      }
      catch {
        case e: com.google.ads.googleads.v2.errors.GoogleAdsException =>
          campaignServiceClient.shutdown()
          e.getGoogleAdsFailure.getErrorsList.asScala.find({
            error: GoogleAdsError =>
              error.getErrorCode.getCampaignError == com.google.ads.googleads.v2.errors.CampaignErrorEnum.CampaignError.DUPLICATE_CAMPAIGN_NAME
          }) match {
            case Some(s) => newCampaign(name + "-" + Random.nextInt(1000)) //Add some random numbers to the end
            case None => None
          }
        case err: Throwable => campaignServiceClient.shutdown(); None
      }
    }
  }

  /**
    * Create a new ad group and its wrapper class under this campaign
    * @param name The name of the newly created ad group
    * @return A new AdGroup wrapper class representing a newly created ad group
    */
  //Creates an ad group
  def createAdGroup (name: String) : AdGroup = {
    AdGroup(accountID, campaign_id, name, qID)
  }

  /**
    * Create a new ad under this campaign, automatically making an ad group for it and supplying keywords to add to this ad group
    * @param title The first part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
    * @param subtitle The second part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
    * @param description The second line of the ad as it will appear in Google search. Must be < 90 characters
    * @param url The url for the ad to link to, which will be automatically truncated by Google when appearing in search results
    * @param keywords A list of keywords to associate with the new ad (and its ad group) with broad match setting
    * @return A new Ad wrapper class representing a newly created ad
    */
  def createAd (title: String, subtitle: String, description: String, url: String, keywords: List[String]) : Ad = {
    createAdGroup(title).createAd(title, subtitle, description, url, keywords, qID)
  }

  /**
    * Set the daily budget of the campaign.
    * @param newBudget The daily budget to be set for this campaign, in dollars.
    */
  def setBudget (newBudget : BigDecimal) : Unit = {
    val bClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient //opens budgets client

    val b = CampaignBudget.newBuilder
      .setResourceName(ResourceNames.campaignBudget(accountID, budget_id))
      .setAmountMicros(Int64Value.of((newBudget * (1000000/2)).toLong))
      .build()

    val bOp = CampaignBudgetOperation.newBuilder //builds budget update operation
      .setUpdate(b)
      .setUpdateMask(FieldMasks.allSetFieldsOf(b))
      .build()

    bClient.mutateCampaignBudgets(accountID.toString,ImmutableList.of(bOp))

    DebugLog(
      "Set budget of campaign " + name + " to $" + newBudget, LogLevelInfo(), LogType.ADAPTER, qID
    )
    bClient.shutdown()
  }

  private def getAdGroups : List[AdGroup] = {
    val gasc =  googleAdsClient.getLatestVersion.createGoogleAdsServiceClient()

    val searchQuery = s"SELECT campaign.id, ad_group.id, ad_group.name FROM ad_group WHERE campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(100)
      .setQuery(searchQuery)
      .build()
    val response : Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    var l : List[Long] = Nil

    for(googleAdsRow : GoogleAdsRow <- response) {
      l = googleAdsRow.getAdGroup.getId.getValue :: l
    }

    gasc.shutdown()
    l.map(AdGroup(accountID,campaign_id,_,qID))
  }

  /**
    * Add search keywords to be added to all adgroups under this campaign (with broad match setting).
    * @param words A list of words to be added as keywords
    */
  def addKeyWords(words : List[String]) : Unit = {
    getAdGroups.foreach(_.addKeyWords(words))
  }

  /**
    * Sets the cost per click of all ads in this campaign
    * @param costPerClick Cost to pay per click on an ad: in dollars
    */
  def setCPC (costPerClick : BigDecimal) : Unit = {
    getAdGroups.foreach(_.setCPC(costPerClick))
    DebugLog(
      "Set CPC of campaign " + name + " to $" + costPerClick, LogLevelInfo(), LogType.ADAPTER, qID
    )
  }


  //Generic method for pausing or resuming campaigns: true if status changed
  private def setStatus (s : CampaignStatus) : Boolean = {
    val cClient = googleAdsClient.getLatestVersion.createCampaignServiceClient()//opens campaign client

    if (cClient.getCampaign(ResourceNames.campaign(accountID, campaign_id)).getStatus == s) {
      false
    } else {
      val c = GoogleCampaign.newBuilder
        .setResourceName(ResourceNames.campaign(accountID, campaign_id))
        .setStatus(s)
        .build()

      val op = CampaignOperation.newBuilder //builds campaign update operation
        .setUpdate(c)
        .setUpdateMask(FieldMasks.allSetFieldsOf(c))
        .build()

      cClient.mutateCampaigns(accountID.toString, ImmutableList.of(op))

      true
    }

    cClient.shutdown()
    cClient.awaitTermination(1, TimeUnit.SECONDS)
  }

  /**
    * Set the status of this campaign to paused
    */
  def pause () : Unit = {
    if (setStatus(CampaignStatus.PAUSED)) {
      DebugLog(
        "Paused campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )
    }
  }

  /**
    * Set the status of this campaign to enabled.
    */
  def resume () : Unit = {
    if (setStatus(CampaignStatus.ENABLED)) {
      DebugLog(
        "Paused campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )
    }
  }

  /**
    * Delete the Google campaign associated with this class
    */

  def delete() : Unit = {
    val csc = googleAdsClient.getLatestVersion.createCampaignServiceClient()
    if (csc.getCampaign(ResourceNames.campaign(accountID, campaign_id)).getStatus != CampaignStatus.REMOVED) {

      val cOp = CampaignOperation.newBuilder //create remove campaign op
        .setRemove(ResourceNames.campaign(accountID, campaign_id))
        .build()

      csc.mutateCampaigns(accountID.toString, ImmutableList.of(cOp)) // remove campaign mutate
      DebugLog(
        "Deleted campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
      )

      val bsc = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()

      val bOp = CampaignBudgetOperation.newBuilder
        .setRemove(ResourceNames.campaignBudget(accountID, budget_id))
        .build()

      DebugLog(
        "Deleted budget " + budget_id, LogLevelInfo(), LogType.ADAPTER, qID
      )
      bsc.mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp))
      bsc.shutdown()
    }
    csc.shutdown()
  }

  // Heavy on the API calls
  /**
    * Get all searches that ended in a click on an ad in this campaign
    * @return A list of search terms
    */
  def searchTerms : List[String] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT search_term_view.search_term FROM search_term_view WHERE campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response : Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    var l : List[String] = Nil

    for(googleAdsRow : GoogleAdsRow <- response) {
      l = googleAdsRow.getSearchTermView.getSearchTerm.getValue :: l
    }

    gasc.shutdown()
    l
  }

  /**
    * Get the total number of clicks on ads in this campaign
    * @return Total number of clicks
    */
  def clicks: Int = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT metrics.clicks FROM campaign WHERE campaign.id = $campaign_id"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountID.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response : Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    var l : List[Int] = Nil

    for(googleAdsRow : GoogleAdsRow <- response) {
      l = googleAdsRow.getMetrics.getClicks.getValue.toInt :: l
    }

    gasc.shutdown()
    l.fold(0 : Int)(_+_)
  }

  def restrictEnglish () : Unit = {
    val agcsc = googleAdsClient.getLatestVersion.createCampaignCriterionServiceClient()

    val criterion = CampaignCriterion.newBuilder
      .setLanguage(LanguageInfo.newBuilder
        .setLanguageConstant(StringValue.of(LanguageConstantName.format(1000.toString)))
        .build)

      .setStatus(CampaignCriterionStatus.ENABLED)
      .setCampaign(StringValue.of(ResourceNames.campaign(accountID,campaign_id)))
      .build()

    val op = CampaignCriterionOperation.newBuilder
      .setCreate(criterion)
      .build()


    agcsc.mutateCampaignCriteria(accountID.toString, ImmutableList.of(op))

    agcsc.shutdown()
    DebugLog(
      "Added English language restriction to campaign " + name, LogLevelInfo(), LogType.ADAPTER, qID
    )
  }

}
