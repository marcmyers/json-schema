package com.cleo.labs.json_schema;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.jayway.restassured.RestAssured;

import java.io.IOException;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;

/**
 * Created by mmyers on 11/2/2015.
 */
public class Utils {
    // Used to get files from the resources directory
    public static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);

    }

    // Used when you need to manipulate a request from sending it
    public static ObjectNode getObjResource(String resource) throws IOException {
        String getResource = Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
        ObjectMapper mapper = new ObjectMapper();
        JsonNode jsonNode = mapper.readTree(getResource);
        return (ObjectNode)jsonNode;

    }

    public static void testSetup(String baseURI, String basePath, String userName, String userPass) {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        RestAssured.baseURI = baseURI;
        RestAssured.basePath = basePath;
        RestAssured.authentication = preemptive().basic(userName, userPass);

    }

    public static void cleanUp(List<JsonNode> jsonNodes) {
        for (JsonNode node : jsonNodes) {
            given().and().delete("/connections/" + node.get("id").asText());
            given().and().delete("/certs/" + node.get("id").asText());

        }
    }

    // Use this if credentials other than the default administrator Admin creds are needed
    public static void changeCreds(String userName, String userPass) {
        RestAssured.authentication = preemptive().basic(userName, userPass);

    }

}
