package com.cleo.labs.tests;

import com.cleo.labs.json_schema.HttpRequest;
import com.cleo.labs.json_schema.SchemaValidation;
import com.cleo.labs.json_schema.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Parameters;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by mmyers on 11/2/2015.
 * Determines if the application/patch/installer is appropriate for further testing
 * Runs through the basic features of the Versalex RestAPI focusing on the "happy path"
 * Shouldn't require a dataprovider, otherwise it may no longer be a smoke test
 * This set assumes that you have the default administrator Admin user and they have access/permission to the RestAPI
 */
public class SmokeTests {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();
    private static List<JsonNode> jsonNodes = new ArrayList<>();

    @BeforeTest
    @Parameters({"serverURL", "endpoint"})
    public static void beforeTest(String serverURL, String endpoint) {
        util.testSetup(serverURL, endpoint, "administrator", "Admin");

    }

    @Test()
    public void as2ConSmoke() throws Exception{
        String jsonRequest = util.getResource("as2-basic-connection.json");
        String postResp = httpRequest.Post(jsonRequest, 201, "/connections");
        jsonNodes.add(new ObjectMapper().readTree(postResp));
        JsonNode node = schemaValid.validate(postResp, "connection");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/connections/"), "connection");
        httpRequest.Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    @Test()
    public void ftpConSmoke() throws Exception{
        String jsonRequest = util.getResource("ftp-basic-connection.json");
        System.out.println(jsonRequest);
        String postResp = httpRequest.Post(jsonRequest, 201, "/connections");
        jsonNodes.add(new ObjectMapper().readTree(postResp));
        JsonNode node = schemaValid.validate(postResp, "connection");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/connections/"), "connection");
        httpRequest.Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    @Test()
    public void genCertSmoke() throws Exception{
        String jsonRequest = util.getResource("qa-test-certificate.json");
        String postResp = httpRequest.Post(jsonRequest, 201, "/certs");
        jsonNodes.add(new ObjectMapper().readTree(postResp));
        JsonNode node = schemaValid.validate(postResp, "certificate");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/certs/"), "certificate");
        httpRequest.Delete(node.get("id").asText(), 204, "/certs/");

    }

    @Test()
    public void importCertSmoke() throws Exception{
        String jsonRequest = util.getResource("import-certificate.json");
        String postResp = httpRequest.Post(jsonRequest, 201, "/certs");
        jsonNodes.add(new ObjectMapper().readTree(postResp));
        JsonNode node = schemaValid.validate(postResp, "certificate");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/certs/"), "certificate");
        httpRequest.Delete(node.get("id").asText(), 204, "/certs/");

    }

    // You need to be able to POST a new connection to POST an action, so this will not run if the connection smoke test has failed
    @Test(dependsOnMethods = {"as2ConSmoke"})
    public void actionSmoke() throws Exception{
        String jsonRequest = util.getResource("as2-basic-connection.json");
        String postResp = httpRequest.Post(jsonRequest, 201, "/connections");
        jsonNodes.add(new ObjectMapper().readTree(postResp));
        JsonNode node = schemaValid.validate(postResp, "connection");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/connections/"), "connection");

        // Prep an action with the self url of the connection
        JsonNode actionNode = new ObjectMapper().readTree(util.getResource("action-request.json"));
        ObjectNode actionRequest = (ObjectNode)actionNode;
        actionRequest.set("connection", getNode.get("_links").get("self"));

        actionNode = schemaValid.validate(httpRequest.Post(actionRequest, 201, "/actions"), "action");
        JsonNode getActNode = schemaValid.validate(httpRequest.Get(actionNode.get("id").asText(), 200, "/actions/"), "action");
        httpRequest.Delete(actionNode.get("id").asText(), 204, "/actions/");
        httpRequest.Delete(node.get("id").asText(), 204, "/connections/");

    }

    @AfterTest
    public void cleanUp() throws Exception{
        // Makes sure that no matter how the test run goes that it attempts to cleanup after itself
        for (JsonNode node : jsonNodes) {
            given().and().delete("/connections/" + node.get("id").asText());
            given().and().delete("/certs/" + node.get("id").asText());

        }

    }

}
