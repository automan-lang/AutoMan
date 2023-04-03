package edu.williams.cs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.mturk.AmazonMTurk;
import com.amazonaws.services.mturk.AmazonMTurkClientBuilder;
import com.amazonaws.services.mturk.model.*;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

public class GetQualificationsForWorkers {
    private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
    private static final String PRODUCTION_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";
    private static final String SIGNING_REGION = "us-east-1";

    private static final String QUAL_MASTERS = "2F1QJWKUDD8XADTFD2Q0G6UTO95ALH";
    private static final String QUAL_NUMHITS_APPROVED = "00000000000000000040";
    private static final String QUAL_LOCALE = "00000000000000000071";
    private static final String QUAL_ADULT = "00000000000000000060";
    private static final String QUAL_PCT_APPROVED = "000000000000000000L0";


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

    private static AmazonMTurk getClient(String path, boolean useSandbox) {
        Tuple<String,String> creds = getCredentials(path);
        BasicAWSCredentials awsCreds = new BasicAWSCredentials(creds.first, creds.second);
        AmazonMTurkClientBuilder builder = AmazonMTurkClientBuilder.standard()
                .withCredentials(new AWSStaticCredentialsProvider(awsCreds));
        if (useSandbox) {
            builder.setEndpointConfiguration(new EndpointConfiguration(SANDBOX_ENDPOINT, SIGNING_REGION));
        } else {
            builder.setEndpointConfiguration(new EndpointConfiguration(PRODUCTION_ENDPOINT, SIGNING_REGION));
        }
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
        if (status.equals("Submitted")) {
            System.err.println("INFO: Approving assignment with Assignment ID: " +
                    a.getAssignmentId() + " for HIT with HIT ID: " + a.getHITId());
            ApproveAssignmentRequest req = new ApproveAssignmentRequest();
            req.setAssignmentId(a.getAssignmentId());
            client.approveAssignment(req);
        } else {
            System.err.println("INFO: Assignment with Assignment ID: " +
                    a.getAssignmentId() + " for HIT with HIT ID: " + a.getHITId() +
                    " already has status '" + status + "'.  Skipping...");
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

    private static void printWorkerStats(AmazonMTurk client, String workerId, String qualificationId, String debugstr) {
        GetQualificationScoreRequest qsr = new GetQualificationScoreRequest();
        qsr.setWorkerId(workerId);
        qsr.setQualificationTypeId(qualificationId);
        try {
            GetQualificationScoreResult res = client.getQualificationScore(qsr);
            System.out.println(debugstr + ": " +res.toString());
            Thread.sleep(500);
        } catch (com.amazonaws.services.mturk.model.RequestErrorException e) {
            System.out.println(debugstr + ": not allowed to ask about that!");
        } catch (InterruptedException e) {
            // seriously Java?  come on.
        }
    }

    private static void Usage() {
        System.out.println("Usage:");
        System.out.println("  You should use the \"run.sh\" shell script.");
        System.out.println();
        System.out.println("  ./run.sh <path to mturk.properties file> <worker ID CSV> <sandbox mode true/false>");
        System.out.println();
        System.out.println("  For example:");
        System.out.println("    /run.sh ~/mturk.properties workers.csv false");
        System.out.println("Gory details:");
        System.out.println("  run.sh actually calls Maven, which performs the following incantation to Cthulhu:");
        System.out.println("  mvn -X exec:java -Dexec.args=\"<path to mturk.properties file>\"");
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            Usage();
            System.exit(1);
        }

        // sandbox mode?
        final boolean sandbox = !args[2].equals("false");

        // path to worker CSV?
        final String csvPath = args[1];

        // initialize client
        final AmazonMTurk client = getClient(args[0], sandbox);

        // read file
        try {
            FileInputStream fio = new FileInputStream(csvPath);
            Scanner s = new Scanner(fio);

            while (s.hasNextLine()) {
                // should just be a worker ID by itself on a line
                String workerId = s.nextLine();

                // ask MTurk
                printWorkerStats(client, workerId, QUAL_MASTERS, workerId + " -> masters");
                printWorkerStats(client, workerId, QUAL_ADULT, workerId + " -> adult");
                printWorkerStats(client, workerId, QUAL_LOCALE, workerId + " -> locale");
                printWorkerStats(client, workerId, QUAL_NUMHITS_APPROVED, workerId + " -> num hits approved");
                printWorkerStats(client, workerId, QUAL_PCT_APPROVED, workerId + " -> pct hits approved");
            }
        } catch (FileNotFoundException e) {
            System.err.println("Hey idiot! Use a real path!");
            System.exit(1);
        }
    }
}