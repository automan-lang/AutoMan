package edu.umass.cs.automan.adapters.googleads.ads

import com.google.ads.googleads.lib.GoogleAdsClient
import com.google.ads.googleads.v2.enums.AdGroupAdStatusEnum.AdGroupAdStatus
import com.google.ads.googleads.v2.common.ExpandedTextAdInfo
import com.google.ads.googleads.v2.enums.PolicyApprovalStatusEnum.PolicyApprovalStatus
import com.google.ads.googleads.v2.resources.{AdGroupAd, Ad => GoogleAd}
import com.google.ads.googleads.v2.services.AdGroupAdOperation
import com.google.ads.googleads.v2.utils.ResourceNames
import com.google.common.collect.ImmutableList
import com.google.protobuf.StringValue

class Ad(googleAdsClient: GoogleAdsClient, accountId: Long, adGroupId: Long, title: String, subtitle: String, description: String, url: String) {
  assert(title.length < 30, "Title too long: " + title)
  assert(subtitle.length < 30, "Subtitle too long: " + subtitle)
  assert(description.length < 90, "Description too long: " + description)

  private val client = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()

  // Creates the information within the ad i.e. title, subtitle, description
  private val expandedTextAdInfo = ExpandedTextAdInfo.newBuilder
    .setHeadlinePart1(StringValue.of(title))
    .setHeadlinePart2(StringValue.of(subtitle))
    .setDescription(StringValue.of(description))
    .build()

  // Wraps the info in an Ads.Ad object
  private val ad = GoogleAd.newBuilder
    .setExpandedTextAd(expandedTextAdInfo)
    .addFinalUrls(StringValue.of(url))
    .build()

  // Builds the final ad representation within ad group
  private val adGroupAd = AdGroupAd.newBuilder
    .setAdGroup(StringValue.of(ResourceNames.adGroup(accountId, adGroupId)))
    .setAd(ad)
    .build()

  private val operations = AdGroupAdOperation.newBuilder.setCreate(adGroupAd).build()
  private val response = client.mutateAdGroupAds(accountId.toString, ImmutableList.of(operations))
  private val id = client.getAdGroupAd(response.getResults(0).getResourceName).getAd.getId.getValue

  println("Added ad: " + title)

  //Saves resource name
  private val adResourceName = response.getResults(0).getResourceName
  client.shutdown()

  /**
    * Delete the Google ad associated with this class
    */
  def delete(): Unit = {
    val sc = googleAdsClient.getLatestVersion.createAdGroupAdServiceClient()

    val rmOp = AdGroupAdOperation.newBuilder.setRemove(adResourceName).build()
    sc.mutateAdGroupAds(accountId.toString, ImmutableList.of(rmOp))

    sc.shutdown()
    println("Deleted ad: " + title)
  }

  /**
    * Gets whether this ad is enabled. If false, ad is paused or removed
    * @return True if ad is enabled, false if paused or removed
    */
  def is_enabled: Boolean = {
    val client = googleAdsClient
      .getLatestVersion
      .createAdGroupAdServiceClient

    val b = client.getAdGroupAd(ResourceNames.adGroupAd(accountId, adGroupId, id))
      .getStatus == AdGroupAdStatus.ENABLED

    client.shutdown()
    b
  }

  /**
    * Checks whether ad has passed review and is approved to run. If false, ad will not run
    * @return True if ad is approved, false if awaiting approval or rejected
    */
  def is_approved: Boolean = {

    val client = googleAdsClient
      .getLatestVersion
      .createAdGroupAdServiceClient

    val b = client
      .getAdGroupAd(ResourceNames.adGroupAd(accountId, adGroupId, id))
      .getPolicySummary
      .getApprovalStatus == PolicyApprovalStatus.APPROVED

    client.shutdown()
    b
  }
}

