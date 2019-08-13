package edu.umass.cs.automan.adapters.googleads.ads

import java.util
import java.util.UUID

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.services.MutateAdGroupAdsResponse
import com.google.ads.googleads.v2.enums.AdGroupAdStatusEnum.AdGroupAdStatus
import com.google.ads.googleads.v2.common.{ExpandedTextAdInfo, PolicyTopicEntry}
import com.google.ads.googleads.v2.enums.PolicyApprovalStatusEnum.PolicyApprovalStatus
import com.google.ads.googleads.v2.errors.{GoogleAdsError, GoogleAdsException}
import com.google.ads.googleads.v2.resources.{AdGroupAd, Ad => GoogleAd}
import com.google.ads.googleads.v2.services.{AdGroupAdOperation, GoogleAdsRow, SearchGoogleAdsRequest}
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.StringValue

import scala.collection.JavaConverters._
import scala.io.StdIn.readLine
import edu.umass.cs.automan.core.logging._

class Ad(googleAdsClient: GoogleAdsClient, accountId: Long, adGroupId: Long, title: String, subtitle: String, description: String, url: String, qID: UUID) {
  if(title.length > 30)
    do {println("Ad title too long. Enter a new ad title (30 chars or less): ")} while (readLine().length() > 30)
  if(subtitle.length > 30)
    do {println("Ad subtitle too long. Enter a new ad subtitle (30 chars or less): ")} while (readLine().length() > 30)
  if(title.length > 90)
    do {println("Ad description too long. Enter a new ad description (90 chars or less): ")} while (readLine().length() > 90)

  private val client = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()


  def postAd (title: String, subtitle: String, description: String, url: String) : Long = {
    // Creates the information within the ad i.e. title, subtitle, description
    val expandedTextAdInfo =
    ExpandedTextAdInfo.newBuilder
      .setHeadlinePart1(StringValue.of(title))
      .setHeadlinePart2(StringValue.of(subtitle))
      .setDescription(StringValue.of(description))
      .build()

    // Wraps the info in an Ads.Ad object
    val ad =
    GoogleAd.newBuilder
      .setExpandedTextAd(expandedTextAdInfo)
      .addFinalUrls(StringValue.of(url))
      .build()

    // Builds the final ad representation within ad group
    val adGroupAd =
    AdGroupAd.newBuilder
      .setAdGroup(StringValue.of(ResourceNames.adGroup(accountId, adGroupId)))
      .setAd(ad)
      .build()

    val operations =
    AdGroupAdOperation.newBuilder.setCreate(adGroupAd).build()


    try {
      val response: MutateAdGroupAdsResponse = client.mutateAdGroupAds(accountId.toString, ImmutableList.of(operations))
    } catch {
      case gae: GoogleAdsException =>
        //Look for duplicate name, add some random numbers to the name if found
        gae.getGoogleAdsFailure.getErrorsList.asScala.find({
          error: GoogleAdsError =>
            error.getErrorCode.getPolicyFindingError == com.google.ads.googleads.v2.errors.PolicyFindingErrorEnum.PolicyFindingError.POLICY_FINDING
        }) match {
          case Some(p) => {
            val l = p.getDetails.getPolicyFindingDetails.getPolicyTopicEntriesList.asScala
            l.foreach(x => println(
              x.getTopic.getValue + " in ad text. \n" +
                "Please change the following " + x.getType.getValueDescriptor.getName + ": \n" +
                x.getEvidencesList.asScala.flatMap(_.getTextList.getTextsList.asScala).mkString(", ")))
            println("Please enter a new ad title: ")
            val t = readLine()
            println("Please enter a new ad subtitle: ")
            val s = readLine()
            println("Please enter a new ad description: ")
            val d = readLine()
            postAd(t,s,d,url)
          }
          case None => throw gae //Probably should just return None, but much harder to debug that way
        }


    }

    queryFilter("ad_group_ad.ad.id","ad_group_ad",List(s"ad_group.id = '$adGroupId'")).head.getAdGroupAd.getAd.getId.getValue
  }

  private val id = postAd(title,subtitle,description,url)
  DebugLog("Created ad " + title + " to ad group with ID " + adGroupId, LogLevelInfo(), LogType.ADAPTER, qID)
  client.shutdown()

  //Delete the Google ad associated with this class
  def delete(): Unit = {
    val sc = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()

    val rmOp = AdGroupAdOperation.newBuilder.setRemove(ResourceNames.adGroupAd(accountId,adGroupId,id)).build()
    sc.mutateAdGroupAds(accountId.toString, ImmutableList.of(rmOp))

    sc.shutdown()
    DebugLog("Deleted ad " + title, LogLevelInfo(), LogType.ADAPTER, qID)
  }

  //Gets whether this ad is enabled. If false, ad is paused or removed
  def is_enabled: Boolean = {
    query("ad_group_ad.status","ad_group_ad").head.getAdGroupAd.getStatus == AdGroupAdStatus.ENABLED
  }

  //Checks whether ad has passed review and is approved to run. If false, ad will not run
  def is_approved: Boolean = {
    query("ad_group_ad.policy_summary","ad_group_ad").head.getAdGroupAd.getPolicySummary.getApprovalStatus == PolicyApprovalStatus.APPROVED
  }

  //Queries for fields under this ad. Should be used in place of get API calls
  //See https://developers.google.com/google-ads/api/docs/query/interactive-gaql-builder
  private def query(field: String, resource: String): Iterable[GoogleAdsRow] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT $field FROM $resource WHERE ad_group_ad.ad.id = $id AND customer.id = $accountId"

    val request = SearchGoogleAdsRequest.newBuilder
      .setCustomerId(accountId.toString)
      .setPageSize(1)
      .setQuery(searchQuery)
      .build()
    val response: Iterable[GoogleAdsRow] = gasc.search(request).iterateAll.asScala

    gasc.shutdown()
    response
  }

  private def queryFilter(field: String, resource: String, filters: List[String]): Iterable[GoogleAdsRow] = {
    val gasc = googleAdsClient.getLatestVersion.createGoogleAdsServiceClient

    val searchQuery = s"SELECT $field FROM $resource WHERE " + filters.mkString(" AND ")

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

