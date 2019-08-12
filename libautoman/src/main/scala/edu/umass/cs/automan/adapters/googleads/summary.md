## The problem we were trying to solve

Our goal this  summer was to extend AutoMan to support crowdsourcing backends other than Amazon Mechanical Turk. Specifically, Google Ads seemed like it could potentially solve some of the biggest issues of using MTurk. The advantages that Google Ads have over MTurk are:

1. Removal of the monetary incentive that MTurk workers might feel to prioritize speed over quality of work

2. Ability to target worker demographics

3. Access to up to 2 billion potential workers

This *targeted crowdsourcing* framework draws from existing research [Quizz](https://static.googleusercontent.com/media/research.google.com/en//pubs/archive/42022.pdf) that proves that online advertising could produce higher quality results at lower or comparable cost to paid crowdsourcing platforms. 

## Approach

We used the [Google Ads](https://developers.google.com/google-ads/api/reference/rpc/) and [Google Apps Script](https://developers.google.com/apps-script/api/) APIs to create an interface between worker-facing ads and forms. Apps Script is a JavaScript-like language used for development in the G Suite platform, and the API allows us to programmatically create and execute Apps Script projects remotely. By deploying a library of functions that interacts with Google Forms, we can make calls to create, modify, and retrieve answers from forms. Similarly, the Ads API allows us to programmatically create ad campaigns, ad groups, and ads while controlling details such as budget, cost-per-click (CPC), keywords, demographics targeting, etc. These logistical details are hidden from the user, automatically readjusting as AutoMan runs to ensure the job finishes with an answer within a reasonable time range.

## How far we got

Most of the Google Ads adapter works as expected: ads and forms are programmatically created through calls of the DSL. Since Google Forms supports all the AutoMan question types, we were able to easily implement questions as forms. So far, the program flow looks like:
  
1. Authorization for first-time users includes creating a Google Cloud Project, acquiring a developer token, deploying the Apps Script library, etc. Running `Authenticate.setup()` leads users through this setup process.

2. Call a DSL function like `radio()` with the proper parameters; it will create a Google Form and Ad based on the information supplied here.

3. The scheduler will wait for the ad to get approved, then check for answers periodically (for checkbox and radio button questions, these retrieve calls also shuffles the question options order).

4. AutoMan will check if the answers reach the specified confidence interval and provide a final answer.

5. The Google Ads adapter is different from MTurk in that it can’t set a limit on the possible number of responses. If the adapter receives more answers than it was expecting, they’ll factor into the current confidence calculations automatically instead of getting ignored.

## How we know our approach worked

We ran multiple experiments using the Google Ads adapter, testing for the effects of CPC and budget on responses. We tested `simple_program` from the MTurk adapter’s example applications and other simple radio button questions. Through these, we saw the adapter successfully coordinate with the AutoMan core to schedule tasks, retrieve answers, and produce a final result. 

Resulting data from the ad campaigns suggests that it’s possible for cost per response to be as low as ~$0.20. Though speed of responses is limited by the time it takes for ad approval and Google’s internal rules, we’ve seen that once an ad shows, dozens of responses can come in within minutes—often enough to satisfy the AutoMan job.

## Future work

1. Forms should be independently hosted outside of Google to allow JavaScript to be added in the surrounding environment to receive signals about when responses are submitted and to ensure respondent uniqueness.

2. The above would allow us to incorporate Google Analytics to optimize for responses

3. Implementing thread concurrency for Google Ads would allow its tasks to run in parallel like MTurk tasks

4. It would also be interesting to incorporate some of the exploration/exploitation framework laid out in Quizz: in other words, use the *quiz* feature of Forms to introduce calibration questions to filter for qualified users. This data can be fed into Analytics to improve cost and answer quality over time by targeting websites that qualified users are found.
