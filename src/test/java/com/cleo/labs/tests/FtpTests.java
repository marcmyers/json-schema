package com.cleo.labs.tests;

import com.cleo.labs.json_schema.HttpRequest;
import com.cleo.labs.json_schema.SchemaValidation;
import com.cleo.labs.json_schema.Utils;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by mmyers on 1/15/16.
 * This should test all the current functionality of the Ftp endpoint and necessary associated endpoints
 * No test should rely on any others except for logical functionality chains
 * EX: If you can't run actions you're not going to be able to run the events test
 * ALL jsonNodes used for testing should be stored in the jsonNodes list so that the test will attempt to clean up after itself @AfterTest
 */
// TODO: Add csv data providers to make the tests more data driven
public class FtpTests {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();
    // Used for gathering up all of the jsonNodes so that if a test fails, the cleanUp method will still run and cleanup after the tests.
    private static List<JsonNode> jsonNodes = new ArrayList<>();

    @BeforeTest
    @Parameters({"serverURL", "endpoint"})
    public static void beforeTest(String serverURL, String endpoint) {
        // Sets the default serverURL, endpoint, and the credentials
        // IMPORTANT: If you need to change creds for a certain test, use the util.changeCreds() method within your test
        util.testSetup(serverURL, endpoint, "administrator", "Admin");

    }

    // Tests the PUT of a basic ftp request w/only the type, validates throughout, deletes after to clean up
    // TODO: Disabled until ftp/sftp merged into dev because of schema validation
    @Test(enabled=false)
    public void ftpTypeOnlyPost() throws Exception{
        // Obtain the json we want to request from a .json file
        String jsonRequest = util.getResource("json/ftp-connection-type-request.json");

        // Attempt a POST request with the json from the json file
        String postResponse = httpRequest.Post(jsonRequest, 201, "/connections");

        // Validate the returned JSON against the appropriate schema - commented out until ftp merged into dev
        JsonNode postNode = schemaValid.validate(postResponse, "connection");

        // Make sure the connection was assigned an id and an alias
        Assert.assertNotNull(postNode.get("id"));
        Assert.assertNotNull(postNode.get("alias"));

        // Add the jsonNode to the list so that even if the test fails, we'll still clean up after ourselves
        jsonNodes.add(new ObjectMapper().readTree(postResponse));

        // Attempt to GET the new connection to ensure permanence
        String getResponse = httpRequest.Get(postNode.get("id").asText(), 200, "/connections/");

        // Validate the returned JSON against the appropriate schema
        JsonNode getNode = schemaValid.validate(getResponse, "connection");

    }

    // POST new Ftp Connection, do a PUT to make it ready
    // TODO: Disabled until ftp/sftp merged into dev because of schema validation
    @Test(enabled=false)
    public void ftpConReady() throws Exception{
        // Get the JSON we want to POST from a Json file
        String jsonRequest = util.getResource("json/ftp-connection-type-request.json");

        // Attempt the post
        String postResp = httpRequest.Post(jsonRequest, 201, "/connections");

        // Validate the Json returned against the appropriate schema
        JsonNode postNode = schemaValid.validate(postResp, "connection");

        // Attempt a get to verify that the connection was created
        String getResp = httpRequest.Get(postNode.get("id").asText(), 200, "/connections/");

        // Validate the Json returned against the appropriate schema
        JsonNode getNode = schemaValid.validate(getResp, "connection");

        // Once we've done a GET, we know it exists so we'll store it off to be cleaned up later
        jsonNodes.add(getNode);

        // Verify content that is pertinent to the test, the connection should not be ready, we're going to make it ready through a PUT
        Assert.assertEquals(getNode.get("ready").asText(), "false");
        Assert.assertEquals(getNode.get("notReadyReason").asText(), "Server Address is required.");

        // Prep an ObjectNode to do a PUT on the connection
        ObjectNode postCon = (ObjectNode)getNode;
        postCon.putObject("connect").put("port", 21).put("host", "localhost").put("username", "myTradingPartner").put("password", "cleo");

        // Attempt to do a PUT on the already POSTed connection to update the trading partner's URL, after that validate the Json returned against the schema
        String putResp = httpRequest.Put(postCon, 200, "/connections/" + getNode.get("id").asText());
        JsonNode putNode = schemaValid.validate(putResp, "connection");

        // Verify that the content has changed due to the PUT after getting the connection again
        String putGetResp = httpRequest.Get(putNode.get("id").asText(), 200, "/connections/");
        JsonNode putGetNode = schemaValid.validate(putGetResp, "connection");

        // Assert that all of our PUT values have changed
        Assert.assertEquals(putGetNode.get("ready").asText(), "true");
        Assert.assertEquals(putGetNode.get("connect").get("host").asText(), "localhost");
        Assert.assertEquals(putGetNode.get("connect").get("username").asText(), "myTradingPartner");

    }

    @Test(enabled=true)
    public void ftpPostTypeOnly() throws Exception{
        // POST new FTP Connection
        String jsonRequest = util.getResource("json/ftp-connection-type-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");

        // Validate
        Assert.assertNotNull(postResponse.get("id"));
        Assert.assertNotNull(postResponse.get("alias"));

        jsonNodes.add(postResponse);

        // Attempt GET
        ObjectNode getResponse = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        // Validate
        Assert.assertNotNull(getResponse.get("id"));
        Assert.assertNotNull(getResponse.get("alias"));

    }

    @Test(enabled=true)
    public void ftpPut() throws Exception{
        // POST new FTP Connection
        String jsonRequest = util.getResource("json/ftp-connection-type-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");

        // Verify that it's not ready
        Assert.assertEquals(postResponse.get("ready").asText(), "false");
        Assert.assertEquals(postResponse.get("notReadyReason").asText(), "Server Address is required.");

        jsonNodes.add(postResponse);

        // Attempt GET
        ObjectNode getResponse = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        // Prep connection for a PUT that should make it ready
        getResponse.putObject("connect").put("port", 5021).put("host", "localhost").put("username", "myTradingPartner").put("password", "cleo");
        getResponse.remove("_links");

        // Attempt PUT
        ObjectNode putResponse = httpRequest.PutReturnObj(getResponse, 200, "/connections/" + getResponse.get("id").asText());

        // Attempt GET
        ObjectNode getPostPut = httpRequest.GetReturnObj(putResponse.get("id").asText(), 200, "/connections/");

        // Validate
        Assert.assertEquals(getPostPut.get("ready").asText(), "true");
        Assert.assertEquals(getPostPut.get("connect").get("port").asText(), "5021");
        Assert.assertEquals(getPostPut.get("connect").get("host").asText(), "localhost");
        Assert.assertEquals(getPostPut.get("connect").get("username").asText(), "myTradingPartner");

    }

    // POST an Ftp Connection, POST an Action with the Ftp Connections self link
    @Test(enabled=true)
    public void ftpPostAction() throws Exception{
        String jsonRequest = util.getResource("json/ftp-connection-ready-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");

        // String getCon = httpRequest.Get(postResponse.get("id").asText(), 200, "/connections/");
        ObjectNode getCon = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actObj = util.getObjResource("json/action-request.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actObj.putObject("connection").put("href", selfLink);

        ObjectNode postAct = httpRequest.PostReturnObj(actObj, 201, "/actions");
        ObjectNode getAct = httpRequest.GetReturnObj(postAct.get("id").asText(), 200, "/actions/");

        // Verify that the action's connect is set to the connection
        Assert.assertEquals(getAct.get("connection"), getCon.get("_links").get("self"));

        String getConVerify = httpRequest.Get(getCon.get("id").asText(), 200, "/connections/");

        String actSelfLink = ((getAct.get("_links").get("self").get("href")).asText());

        // Verify that the connection lists the action
        Assert.assertTrue(getConVerify.contains(actSelfLink));

    }

    // Intended to clean up after the testing regardless of the test outcome. Nothing is asserted in the method because we'll attempt to delete all the jsonNodes
    @AfterTest
    public void cleanUp() throws Exception{
        util.cleanUp(jsonNodes);

    }

}
