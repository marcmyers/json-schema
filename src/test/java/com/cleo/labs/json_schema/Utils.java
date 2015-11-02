package com.cleo.labs.json_schema;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.restassured.RestAssured;

import java.io.IOException;

import static com.jayway.restassured.RestAssured.preemptive;

/**
 * Created by mmyers on 11/2/2015.
 */
public class Utils {
    // Used to get files from the resources directory
    public static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);

    }

    public static void testSetup(String baseURI, String basePath, String userName, String userPass) {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        RestAssured.baseURI = baseURI;
        RestAssured.basePath = basePath;
        RestAssured.authentication = preemptive().basic(userName, userPass);

    }

}
