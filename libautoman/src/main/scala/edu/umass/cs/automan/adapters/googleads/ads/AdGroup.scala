package edu.umass.cs.automan.adapters.googleads.ads

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.lib.utils.FieldMasks
import com.google.ads.googleads.v2.common.KeywordInfo
import com.google.ads.googleads.v2.enums.AdGroupCriterionStatusEnum.AdGroupCriterionStatus
import com.google.ads.googleads.v2.enums.AdGroupStatusEnum.AdGroupStatus
import com.google.ads.googleads.v2.enums.AdGroupTypeEnum.AdGroupType
import com.google.ads.googleads.v2.enums.KeywordMatchTypeEnum.KeywordMatchType
import com.google.ads.googleads.v2.resources.{AdGroupCriterion, AdGroup => Group}
import com.google.ads.googleads.v2.services._
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.{Int64Value, StringValue}

import edu.umass.cs.automan.adapters.googleads.util.Service._

object AdGroup {
    /**
      * Construct a new ad group and wrapper class
      * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
      * @param campaignId The ID of the parent campaign
      * @param name A new name for this ad group
      * @return A new AdGroup wrapper class representing a newly created ad group
      */
    def apply(accountId : Long, campaignId: Long, name: String) : AdGroup = {
        val googleAdsClient = googleClient()
        val ag = new AdGroup(googleAdsClient, accountId)
        ag.build(campaignId, name)
        ag
    }
    /**
      * Construct a new wrapper class for an existing ad group
      * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
      * @param campaignId The ID of the parent campaign
      * @param adGroupId The ID of the ad group to be loaded
      * @return A new AdGroup wrapper class representing an existing ad group
      */
    def apply(accountId: Long, campaignId : Long, adGroupId : Long) : AdGroup = {
        val googleAdsClient = googleClient()
        val ag = new AdGroup(googleAdsClient, accountId)
        ag.load(campaignId, adGroupId)
        ag
    }
}

class AdGroup (googleAdsClient: GoogleAdsClient, accountId: Long) {

    private var _adgroup_id : Option[Long] = None

    def adgroup_id : Long = _adgroup_id match {case Some(c) => c case None => throw new Exception("AdGroup not initialized")}

    def cpc : BigDecimal = {
        val client = googleAdsClient.getLatestVersion.createAdGroupServiceClient
        val c = BigDecimal(client.getAdGroup(ResourceNames.adGroup(accountId,adgroup_id)).getCpcBidMicros.getValue)/1000000
        client.shutdown()
        c
    }

    private def load(campaignId: Long, id: Long) : Unit = {
        val agsc = googleAdsClient.getLatestVersion.createAdGroupServiceClient()
        val ag = agsc.getAdGroup(ResourceNames.adGroup(accountId,id))

        _adgroup_id = Some(ag.getId.getValue)
        agsc.shutdown()
    }

    private def build(campaignId: Long, name: String): Unit = {
        val adGroupServiceClient =
        googleAdsClient.getLatestVersion.createAdGroupServiceClient()

        // Builds an ad group, setting cost per click (cpc) settings
        val ag =
        Group.newBuilder
          .setName(StringValue.of(name))
          .setStatus(AdGroupStatus.ENABLED)
          .setCampaign(StringValue.of(ResourceNames.campaign(accountId,campaignId)))
          .setType(AdGroupType.SEARCH_STANDARD)
          .setCpcBidMicros(Int64Value.of((100 * 10000).toLong)) //$1
          .build()

        val op =
        AdGroupOperation.newBuilder.setCreate(ag).build()
        val response =
        adGroupServiceClient.mutateAdGroups(accountId.toString, ImmutableList.of(op)) //Request to create adGroup

        println("Added ad group: " + name) //Finished creating ad group

        //Save resource name
        _adgroup_id = Some(adGroupServiceClient.getAdGroup(response.getResultsList.get(0).getResourceName).getId.getValue)
        adGroupServiceClient.shutdown()
    }

    /**
      * Create a new ad under this ad group, automatically making an ad group for it and supplying keywords to ad to this ad group
      * @param title The first part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
      * @param subtitle The second part of the first line of the ad as it will appear as a link on Google search: "Title | Subtitle" Must be < 30 characters
      * @param description The second line of the ad as it will appear in Google search. Must be < 90 characters
      * @param url The url for the ad to link to, which will be automatically truncated by Google when appearing in search results
      * @param keywords A list of keywords to associate with the new ad (and this ad group) with broad match setting
      * @return A new Ad wrapper class representing a newly created ad
      */
    def createAd (title: String, subtitle: String, description: String, url: String, keywords: List[String]) : Ad = {
        if (keywords.nonEmpty) addKeyWords(keywords)
        else addKeyWords(title.split(" ").toList)
        new Ad(googleAdsClient, accountId, adgroup_id, title, subtitle, description, url)
    }

    //Method to return an operation to add one keyword
    //Follows similar pattern of other mutates
    private def addKeyWordHelper (s: String, agcServiceClient: AdGroupCriterionServiceClient) : AdGroupCriterionOperation = {
        val keywordInfo = KeywordInfo.newBuilder
          .setText(StringValue.of(s.replaceAll("\\p{Punct}", "")))
          .setMatchType(KeywordMatchType.BROAD)
          .build()

        val criterion = AdGroupCriterion.newBuilder
          .setAdGroup(StringValue.of(ResourceNames.adGroup(accountId,adgroup_id)))
          .setStatus(AdGroupCriterionStatus.ENABLED)
          .setKeyword(keywordInfo)
          .build()

        AdGroupCriterionOperation.newBuilder
          .setCreate(criterion)
          .build()
    }

    /**
      * Add search keywords to be added to this ad group (with broad match setting).
      * @param words A list of words to be added as keywords
      */
    protected[ads] def addKeyWords (words: List[String]) : Unit = {
        val agcsc = googleAdsClient.getLatestVersion.createAdGroupCriterionServiceClient()
        val l : List[AdGroupCriterionOperation] = words.map(addKeyWordHelper(_, agcsc))
        agcsc.mutateAdGroupCriteria(accountId.toString, l.asJava)

        agcsc.shutdown()
        println("Added keywords '" + words + "' to ad group: " + adgroup_id)
    }

    /**
      * Delete the Google ad group associated with this class
      */
    def delete () : Unit = {
        val agsc = googleAdsClient.getLatestVersion.createAdGroupServiceClient()

        val rmOp = AdGroupOperation.newBuilder.setRemove(ResourceNames.adGroup(accountId,adgroup_id)).build()
        agsc.mutateAdGroups(accountId.toString, ImmutableList.of(rmOp))

        agsc.shutdown()
        println("Deleted ad group: " + adgroup_id)
    }

    protected[ads] def setCPC (costPerClick : BigDecimal) : Unit = {
        val agsc = googleAdsClient.getLatestVersion.createAdGroupServiceClient()//opens ad group client

        val ag = Group.newBuilder
          .setResourceName(ResourceNames.adGroup(accountId,adgroup_id))
          .setCpcBidMicros(Int64Value.of((costPerClick * 1000000).toLong))
          .build()

        val op = AdGroupOperation.newBuilder //builds ad group update operation
          .setUpdate(ag)
          .setUpdateMask(FieldMasks.allSetFieldsOf(ag))
          .build()

        agsc.mutateAdGroups(accountId.toString,ImmutableList.of(op))

        agsc.shutdown()
        println("Set CPC of ad group " + adgroup_id + " to " + costPerClick)
    }
}
