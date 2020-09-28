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

public class DeleteAllQualifications {
    private static final String SANDBOX_ENDPOINT = "mturk-requester-sandbox.us-east-1.amazonaws.com";
    private static final String PRODUCTION_ENDPOINT = "https://mturk-requester.us-east-1.amazonaws.com";
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

    private static List<QualificationType> listQualifications(AmazonMTurk client) {
        boolean more = true;
        String paginationToken = null;
        List<QualificationType> data = new ArrayList<>();

        while (more) {
            ListQualificationTypesRequest req =
                    new ListQualificationTypesRequest()
                            .withMustBeRequestable(false)
                            .withMustBeOwnedByCaller(true);

            // if we have a pagination token, use it
            if (paginationToken != null) {
                req.withNextToken(paginationToken);
            }

            // get results
            ListQualificationTypesResult response = client.listQualificationTypes(req);

            // get results
            List<QualificationType> results = response.getQualificationTypes();

            // are there more results?
            if (response.getNumResults() == 0) {
                more = false;
            } else {
                paginationToken = response.getNextToken();
            }

            // append to master list
            data.addAll(results);
        }

        return data;
    }

    private static void Usage() {
        System.out.println("Usage:");
        System.out.println("  You should use the \"run.sh\" shell script.");
        System.out.println();
        System.out.println("  ./run <path to mturk.properties file> <sandbox mode true/false>");
        System.out.println();
        System.out.println("  For example:");
        System.out.println("    /run ~/mturk.properties false");
        System.out.println("Gory details:");
        System.out.println("  run.sh actually calls Maven, which performs the following incantation to Cthulhu:");
        System.out.println("  mvn -X exec:java -Dexec.args=\"<path to mturk.properties file>\"");
    }

    private static void deleteQualification(QualificationType qual, AmazonMTurk client) {
        // make request
        DeleteQualificationTypeRequest req =
                new DeleteQualificationTypeRequest().withQualificationTypeId(qual.getQualificationTypeId());

        // issue request
        client.deleteQualificationType(req);
    }

    public static void main(String[] args) {
        if (args.length != 1 && args.length != 2) {
            Usage();
            System.exit(1);
        }

        // sandbox mode?
        final boolean sandbox = !(args.length == 2 && args[1].equals("false"));

        // initialize client
        final AmazonMTurk client = getClient(args[0], sandbox);

        List<QualificationType> quals = listQualifications(client);

        if (quals.size() == 0) {
            System.out.println("No qualifications.  Nothing to do.  Exiting...");
            System.exit(0);
        }

        System.out.println("Removing " + quals.size() + " qualification types.");
        if (quals.size() > 20) {
            System.out.println("This may take awhile.  Please be patient.");
        }
        System.out.print("Deleting ");
        for (QualificationType qual : quals) {
            // delete
            System.out.print(".");
            deleteQualification(qual, client);

            // rate limit
            try {
                Thread.sleep(500);
            } catch (java.lang.InterruptedException e) {
                // do nothing
            }
        }
        System.out.println();
    }
}
