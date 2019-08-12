package edu.umass.cs.automan.adapters.googleads.ads

import java.util.UUID

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

import scala.collection.JavaConverters._
import edu.umass.cs.automan.adapters.googleads.util.Service._
import edu.umass.cs.automan.core.logging.{DebugLog, LogLevelInfo, LogType}

object AdGroup {
    /**
      * Construct a new ad group and wrapper class
      * @param accountId The ID of the parent account, found in the format xxx-xxx-xxxx
      * @param campaignId The ID of the parent campaign
      * @param name A new name for this ad group
      * @return A new AdGroup wrapper class representing a newly created ad group
      */
    def apply(accountId : Long, campaignId: Long, name: String, qID: UUID) : AdGroup = {
        val googleAdsClient = googleClient
        val ag = new AdGroup(googleAdsClient, accountId, qID)
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
    def apply(accountId: Long, campaignId : Long, adGroupId : Long, qID: UUID) : AdGroup = {
        val googleAdsClient = googleClient
        val ag = new AdGroup(googleAdsClient, accountId, qID)
        ag.load(campaignId, adGroupId)
        ag
    }
}

class AdGroup (googleAdsClient: GoogleAdsClient, accountId: Long, qID: UUID) {

    private var _adgroup_id : Option[Long] = None

    def adgroup_id : Long = _adgroup_id match {case Some(c) => c case None => throw new Exception("AdGroup not initialized")}

    def cpc : BigDecimal = {
        val c = BigDecimal(query("ad_group.cpc_bid_micros","ad_group").head.getAdGroup.getCpcBidMicros.getValue)/1000000
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

        DebugLog(
            "Added adgroup " + name + " to campaign with ID " + campaignId, LogLevelInfo(), LogType.ADAPTER, qID
        )
        adGroupServiceClient.shutdown()

        //Save ID
        val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

        val searchQuery = s"SELECT ad_group.id FROM ad_group WHERE campaign.id = $campaignId AND customer.id = $accountId"

        val request = SearchGoogleAdsRequest.newBuilder
            .setCustomerId(accountId.toString)
            .setPageSize(1)
            .setQuery(searchQuery)
            .build()

        val searchResponse: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

        gasc.shutdown()

        _adgroup_id = Some(searchResponse.head.getAdGroup.getId.getValue)
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
    def createAd (title: String, subtitle: String, description: String, url: String, keywords: List[String],qID: UUID) : Ad = {
        if (keywords.nonEmpty) addKeyWords(keywords)
        else addKeyWords(title.split(" ").toList)
        new Ad(googleAdsClient, accountId, adgroup_id, title, subtitle, description, url,qID)
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
        DebugLog(
            "Added keywords " +
              (if (words.length > 5) {words.take(5) + "..."} else words) +
              " to ad group with ID " + adgroup_id,
            LogLevelInfo(), LogType.ADAPTER, qID
        )
    }

    /**
      * Delete the Google ad group associated with this class
      */
    def delete () : Unit = {
        val agsc = googleAdsClient.getLatestVersion.createAdGroupServiceClient()

        val rmOp = AdGroupOperation.newBuilder.setRemove(ResourceNames.adGroup(accountId,adgroup_id)).build()
        agsc.mutateAdGroups(accountId.toString, ImmutableList.of(rmOp))

        agsc.shutdown()
        DebugLog(
            "Deleted ad group with ID " + adgroup_id, LogLevelInfo(), LogType.ADAPTER, qID
        )
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
        DebugLog(
            "Set CPC of ad group with ID " + adgroup_id + " to $" + costPerClick, LogLevelInfo(), LogType.ADAPTER, qID
        )
    }

    def query(field: String, resource: String): Iterable[GoogleAdsRow] = {
        val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

        val searchQuery = s"SELECT $field FROM $resource WHERE ad_group.id = $adgroup_id AND customer.id = $accountId"

        val request = SearchGoogleAdsRequest.newBuilder
          .setCustomerId(accountId.toString)
          .setPageSize(1)
          .setQuery(searchQuery)
          .build()
        val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

        gasc.shutdown()
        response
    }
}
