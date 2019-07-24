package edu.umass.cs.automan.adapters.googleads.ads

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.lib.utils.FieldMasks
import com.google.ads.googleads.v2.services.CampaignCriterionOperation
import com.google.ads.googleads.v2.common.LanguageInfo
import com.google.ads.googleads.v2.resources.{CampaignBudget, CampaignCriterion, LanguageConstantName, Campaign => GoogleCampaign}
import com.google.ads.googleads.v2.common.ManualCpc
import com.google.ads.googleads.v2.enums.AdvertisingChannelTypeEnum.AdvertisingChannelType
import com.google.ads.googleads.v2.enums.BiddingStrategyTypeEnum.BiddingStrategyType
import com.google.ads.googleads.v2.enums.BudgetDeliveryMethodEnum.BudgetDeliveryMethod
import com.google.ads.googleads.v2.enums.CampaignCriterionStatusEnum.CampaignCriterionStatus
import com.google.ads.googleads.v2.enums.CampaignStatusEnum.CampaignStatus
import com.google.ads.googleads.v2.resources.Campaign.NetworkSettings
import com.google.ads.googleads.v2.services.{CampaignBudgetOperation, CampaignOperation, GoogleAdsRow, SearchGoogleAdsRequest}
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.{BoolValue, Int64Value, StringValue}


object Campaign {
  /**
    * Construct a new campaign and wrapper class
    * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
    * @param dailyBudget The daily amount to be spent by this account. Limited by month, so a campaign can spend up 30x its daily budget (likely will only spend 2x)
    * @param name A new name for this campaign
    * @return A new AdCampaign wrapper class representing a newly created campaign
    */
  def apply(accountId: Long, dailyBudget: BigDecimal, name: String): Campaign = {
    val googleAdsClient = Account.client()
    val camp: Campaign = new Campaign(googleAdsClient, accountId)
    camp.build(dailyBudget, name)
    camp
  }
  /**
    * Construct a new wrapper class for an existing campaign
    * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
    * @param campId The ID of the campaign to be loaded
    * @return A new AdCampaign wrapper class representing an existing campaign
    */
  def apply(accountId: Long, campId: Long): Campaign = {
    val googleAdsClient = client()
    val camp: Campaign = new Campaign(googleAdsClient, accountId)
    camp.load(accountId,campId)
    camp
  }
}

class Campaign(googleAdsClient: GoogleAdsClient, accountID: Long) {

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

    println("Loaded campaign: " + name) //End of campaign creation
  }

  private def build(dailyBudget : BigDecimal, name : String) : Unit = {
    assert(name.length < 30, "Budget name too long")
    assert(dailyBudget > 0.01F, "Budget cannot be less than $0.01")

    val b = CampaignBudget.newBuilder.setName(StringValue.of(name)) //builds a new budget (not yet created)
      .setDeliveryMethod(BudgetDeliveryMethod.ACCELERATED)
      .setAmountMicros(Int64Value.of((dailyBudget * (1000000/2)).toLong))
      .build()

    val bOp =
    CampaignBudgetOperation.newBuilder //builds budget create operation
      .setCreate(b)
      .build()

    val campaignBudgetServiceClient = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient //opens budgets client

    val budgetResponse = campaignBudgetServiceClient.mutateCampaignBudgets(accountID.toString, ImmutableList.of(bOp)) //creates budget through mutate


    val budget: CampaignBudget = campaignBudgetServiceClient.getCampaignBudget(budgetResponse.getResults(0).getResourceName)
    _budget_id = Some(budget.getId.getValue)

    println("Added budget: " + name)

    val networkSettings = NetworkSettings.newBuilder //builds campaign networks options (where you want ad to be published)
      .setTargetGoogleSearch(BoolValue.of(true))
      .setTargetSearchNetwork(BoolValue.of(true))
      .setTargetContentNetwork(BoolValue.of(true))
      .setTargetPartnerSearchNetwork(BoolValue.of(false))
      .build()


    // Builds the campaign.
    val c = GoogleCampaign.newBuilder
      .setName(StringValue.of(name))
      .setAdvertisingChannelType(AdvertisingChannelType.SEARCH)
      .setBiddingStrategyType(BiddingStrategyType.MANUAL_CPC)
      .setStatus(CampaignStatus.ENABLED)
      .setManualCpc(ManualCpc.newBuilder.build())
      .setCampaignBudget(StringValue.of(ResourceNames.campaignBudget(accountID,budget_id)))
      .setNetworkSettings(networkSettings)
      .build()

    // Create campaign op
    val cOp =
    CampaignOperation.newBuilder
      .setCreate(c)
      .build()

    val campaignServiceClient =
    googleAdsClient.getLatestVersion.createCampaignServiceClient //opens campaign create client
    val response =
    campaignServiceClient.mutateCampaigns(accountID.toString, ImmutableList.of(cOp)) //creates campaign through mutate

    println("Added campaign: " + name) //End of campaign creation

    //Save useful info
    val campaign = campaignServiceClient.getCampaign(response.getResultsList.get(0).getResourceName)
    _campaign_id = Some(campaign.getId.getValue)
    _name = Some(campaign.getName.getValue)

    campaignBudgetServiceClient.shutdown()
    campaignServiceClient.shutdown()
  }

  /**
    * Create a new ad group and its wrapper class under this campaign
    * @param name The name of the newly created ad group
    * @return A new AdGroup wrapper class representing a newly created ad group
    */
  //Creates an ad group
  def createAdGroup (name: String) : AdGroup = {
    AdGroup(accountID, campaign_id, name: String)
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
    createAdGroup(title).createAd(title,subtitle,description,url,keywords)
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

    println("Set Budget to $" + newBudget)
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
    l.map(AdGroup(accountID,campaign_id,_))
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
    println("Set " + name + " CPC to " + costPerClick)
  }


  //Generic method for pausing or resuming campaigns
  private def setStatus (s : CampaignStatus) : Unit = {
    val cClient = googleAdsClient.getLatestVersion.createCampaignServiceClient()//opens campaign client

    val c = GoogleCampaign.newBuilder
      .setResourceName(ResourceNames.campaign(accountID,campaign_id))
      .setStatus(s)
      .build()

    val op = CampaignOperation.newBuilder //builds campaign update operation
      .setUpdate(c)
      .setUpdateMask(FieldMasks.allSetFieldsOf(c))
      .build()

    cClient.mutateCampaigns(accountID.toString,ImmutableList.of(op))

    cClient.shutdown()
  }

  /**
    * Set the status of this campaign to paused
    */
  def pause () : Unit = {
    setStatus(CampaignStatus.PAUSED)
    println("Paused campaign " + name)
  }

  /**
    * Set the status of this campaign to enabled.
    */
  def resume () : Unit = {
    setStatus(CampaignStatus.ENABLED)
    println("Resumed campaign " + name)
  }

  /**
    * Delete the Google campaign associated with this class
    */

  def delete() : Unit = {
    val csc = googleAdsClient.getLatestVersion.createCampaignServiceClient()

    val cOp = CampaignOperation.newBuilder //create remove campaign op
      .setRemove(ResourceNames.campaign(accountID,campaign_id))
      .build()

    csc.mutateCampaigns(accountID.toString,ImmutableList.of(cOp)) // remove campaign mutate
    println("Deleted campaign " + name)

    csc.shutdown()

    val bsc = googleAdsClient.getLatestVersion.createCampaignBudgetServiceClient()

    val bOp = CampaignBudgetOperation.newBuilder
        .setRemove(ResourceNames.accountBudget(accountID,budget_id))
        .build()

    bsc.mutateCampaignBudgets(accountID.toString,ImmutableList.of(bOp))
    bsc.shutdown()
    println("Deleted budget " + name)

    bsc.awaitTermination(1,TimeUnit.SECONDS)
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
    println("Added english language restriction to: " + name)
  }

}
