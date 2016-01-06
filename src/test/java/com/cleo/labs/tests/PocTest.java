package com.cleo.labs.tests;

import com.cleo.labs.json_schema.HttpRequest;
import com.cleo.labs.json_schema.SchemaValidation;
import com.cleo.labs.json_schema.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * These tests and methods are not intended for use in Jenkins or any other automated build platform.
 * Their main purpose was to attempt to explore alternative methods of testing Rest w/out POJOs or complex utility methods
 */
public class PocTest {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();

    @BeforeTest
    @Parameters({"url"})
    public static void beforeTest(String url) {
        util.testSetup(url, "/api/", "administrator", "Admin");

    }

    // POSTS a new connection using a JSON file and validates the schema
    @Test
    public void liveConPostTest() throws Exception {
        String jsonRequest = util.getResource("as2-basic-connection.json");
        JsonNode node = schemaValid.validate(httpRequest.Post(jsonRequest, 201, "/connections"), "connection");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/connections/"), "connection");
        httpRequest.Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    // Generates a new certificate using a request from a json file and then attempts to delete it
    // Disabled until schema validation succeeds
    @Test(enabled = false)
    public void liveCertTest() throws Exception {
        String jsonRequest = util.getResource("qa-test-certificate.json");
        JsonNode node = schemaValid.validate(httpRequest.Post(jsonRequest, 201, "/certs"), "certificate");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/certs/"), "certificate");
        httpRequest.Delete(node.get("id").asText(), 204, "/certs/");

    }

    // Generates a new certificate using a request generated from a JSON Object and then attempts to delete it
    // Disabled until schema validation succeeds
    @Test(enabled = false)
    public void liveCertTestJsonObject() throws Exception {
        List<String> keyUsage = new ArrayList<String>();
        keyUsage.add("digitalSignature");
        String randomName = RandomStringUtils.randomAlphabetic(10);

        JSONObject newCert = new JSONObject();
        newCert.put("requestType", "generateCert");
        newCert.put("alias", "QA_TEST_" + randomName.toUpperCase());
        newCert.put("dn", "cn=test,c=us");
        newCert.put("validity", "24 months");
        newCert.put("keyAlg", "rsa");
        newCert.put("keySize", 2048);
        newCert.put("password", "cleo");
        newCert.put("keyUsage", keyUsage);

        String postResponse = httpRequest.Post(newCert, 201, "/certs");

        JsonNode node = schemaValid.validate(postResponse, "certificate");

        httpRequest.Delete(node.get("id").asText(), 204, "/certs/");

    }

    /*
    POSTS a new connection, validates the Responses using the schema
    Then, attempts to do a Put on the connection to make it ready
    */
    @Test
    public void liveConPutTest() throws Exception {
        JSONObject newConnection = new JSONObject();
        newConnection.put("type", "as2");

        // Attempt a POST to generate a new connection
        String postResp = httpRequest.Post(newConnection, 201, "/connections");

        // Validate the response against the schema
        JsonNode postNode = schemaValid.validate(postResp, "connection");

        // Verify content pertinent to the test, the connection should not be ready
        Assert.assertEquals(postNode.get("ready").asText(), "false");
        Assert.assertEquals(postNode.get("notReadyReason").asText(), "Server Address is required.");

        // Attempt a Get to verify permanence, validate the JSON returned against the connection schema
        String getResp = httpRequest.Get(postNode.get("id").asText(), 200, "/connections/");
        JsonNode getNode = schemaValid.validate(getResp, "connection");

        // Prep an ObjectNode to do a put on the Connection
        ObjectNode postCon = (ObjectNode)getNode;
        postCon.remove("meta");
        postCon.putObject("connect").put("url", "http://localhost:5080/as2");

        // Attempt to do a Put on the already POSTED connection to update the trading partner's URL
        String putResp = httpRequest.Put(postCon, 200, "/connections/" + getNode.get("id").asText());
        JsonNode putNode = schemaValid.validate(putResp, "connection");

        // Verify that the content has changed due to the Put
        Assert.assertEquals(putNode.get("ready").asText(), "true");
        Assert.assertEquals(putNode.get("connect").get("url").asText(), "http://localhost:5080/as2");

        // Attempt the final Get to verify permanence, validate the JSON returned against the connection schema
        String finalGet = httpRequest.Get(putNode.get("id").asText(), 200, "/connections/");
        JsonNode finalNode = schemaValid.validate(finalGet, "connection");

        // Attempt a delete to clean up after the test
        httpRequest.Delete(putNode.get("id").asText(), 204, "/connections/");

        // Attempt a Get to ensure that it's been deleted
        httpRequest.Get(putNode.get("id").asText(), 404, "/connections/");

    }

}
