package edu.williams.cs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.mturk.AmazonMTurk;
import com.amazonaws.services.mturk.AmazonMTurkClientBuilder;
import com.amazonaws.services.mturk.model.*;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class DeleteAllFromSandbox {
    private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
    private static final String SIGNING_REGION = "us-east-1";

    private static class Tuple<X, Y> {
        public final X first;
        public final Y second;
        public Tuple(X first, Y second) {
            this.first = first;
            this.second = second;
        }
    }

    private static Tuple<String,String> getCredentials(String path) {
        try {
            InputStream input = new FileInputStream(path);
            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            // get the property values, box them into a tuple, and return
            return new Tuple<String, String>(
                    prop.getProperty("access_key"),
                    prop.getProperty("secret_key")
            );
        } catch (IOException ex) {
            ex.printStackTrace();
            System.exit(1);
            return null; // never happens
        }
    }

    private static AmazonMTurk getSandboxClient(String path) {
        Tuple<String,String> creds = getCredentials(path);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(creds.first, creds.second);
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds));
        builder.setEndpointConfiguration(new EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION));
        return builder.build();
    }

    private static String getAccountBalance(AmazonMTurk client) {
        GetAccountBalanceRequest getBalanceRequest = new GetAccountBalanceRequest();
        GetAccountBalanceResult result = client.getAccountBalance(getBalanceRequest);
        return result.getAvailableBalance();
    }

    private static List<HIT> listHITs(AmazonMTurk client) {
        ListHITsRequest req = new ListHITsRequest();
        return client.listHITs(req).getHITs();
    }

    private static List<Assignment> assignmentsForHIT(HIT hit, AmazonMTurk client) {
        ListAssignmentsForHITRequest req = new ListAssignmentsForHITRequest();
        req.setHITId(hit.getHITId());

        List<Assignment> assns = new ArrayList<>();

        // We need to 'page' through the results.
        // Each result page returns a "pagination token" for the next page.
        // The pagination token becomes null when no pages remain.
        String nextToken = null;
        do {
            ListAssignmentsForHITResult res = client.listAssignmentsForHIT(req);
            List<Assignment> iterAssns = res.getAssignments();
            assns.addAll(iterAssns);
            nextToken = res.getNextToken();
            req.setNextToken(nextToken);
        } while (nextToken != null);

        return assns;
    }

    private static void expireHIT(HIT hit, AmazonMTurk client) {
        System.err.println("INFO: Expiring HIT with HIT ID: " + hit.getHITId());
        UpdateExpirationForHITRequest req = new UpdateExpirationForHITRequest();
        req.setHITId(hit.getHITId());
        req.setExpireAt(oneMinuteAgo());
        client.updateExpirationForHIT(req);
    }

    private static Date oneMinuteAgo() {
        final long ONE_MINUTE_IN_MILLIS = 60000; // millis in one minute

        try {
            Calendar date = Calendar.getInstance();
            long t = date.getTimeInMillis();
            return new Date(t - ONE_MINUTE_IN_MILLIS);
        }
        catch (Exception e){
            e.printStackTrace();
            System.exit(1);
            return null;
        }
    }

    private static void approveAssignment(Assignment a, AmazonMTurk client) {
        String status = a.getAssignmentStatus();
        if (!status.equals("Approved")) {
            System.err.println("INFO: Approving assignment with Assignment ID: " +
                    a.getAssignmentId() + " for HIT with HIT ID: " + a.getHITId());
            ApproveAssignmentRequest req = new ApproveAssignmentRequest();
            req.setAssignmentId(a.getAssignmentId());
            client.approveAssignment(req);
        } else {
            System.err.println("INFO: Assignment with Assignment ID: " +
                    a.getAssignmentId() + " for HIT with HIT ID: " + a.getHITId() +
                    " already has status 'Approved'.  Skipping...");
        }

    }

    private static void setHITToReviewing(HIT hit, AmazonMTurk client) {
        String status = hit.getHITStatus();
        if (!status.equals("Reviewing")) {
            System.err.println("INFO: Set HIT status to Reviewing for HIT ID: " + hit.getHITId());
            UpdateHITReviewStatusRequest req = new UpdateHITReviewStatusRequest();
            req.setHITId(hit.getHITId());
            client.updateHITReviewStatus(req);
        } else {
            System.err.println("INFO: Set HIT status to Reviewing for HIT ID: " + hit.getHITId() +
            " already set to 'Reviewing'.  Skipping...");
        }
    }

    private static void deleteHIT(HIT hit, AmazonMTurk client) {
        System.err.println("INFO: Deleting HIT with HIT ID: " + hit.getHITId());
        DeleteHITRequest req = new DeleteHITRequest();
        req.setHITId(hit.getHITId());
        client.deleteHIT(req);
    }

    private static void Usage() {
        System.out.println("Usage:");
        System.out.println("  You should use the \"run.sh\" shell script.");
        System.out.println();
        System.out.println("  ./run <path to mturk.properties file>");
        System.out.println();
        System.out.println("Gory details:");
        System.out.println("  run.sh actually calls Maven, which performs the following incantation to Cthulhu:");
        System.out.println("  mvn -X exec:java -Dexec.args=\"<path to mturk.properties file>\"");
    }

    public static void main(String[] args) {
        if (args.length != 1) {
            Usage();
            System.exit(1);
        }

        final AmazonMTurk client = getSandboxClient(args[0]);

        List<HIT> hits = listHITs(client);

        if (hits.size() == 0) {
            System.out.println("No HITs.  Nothing to do.  Exiting...");
            System.exit(0);
        }

        for (HIT hit : hits) {
            // cancel each HIT
            expireHIT(hit, client);

            // change to reviewing
            setHITToReviewing(hit, client);

            // approve each assignment
            for (Assignment a : assignmentsForHIT(hit, client)) {
                approveAssignment(a, client);
            }

            // delete the HIT
            deleteHIT(hit, client);
        }
    }
}
