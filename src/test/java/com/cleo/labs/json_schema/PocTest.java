package com.cleo.labs.json_schema;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.RestAssured;
import org.apache.commons.lang3.RandomStringUtils;
import org.json.JSONObject;
import org.skife.url.UrlSchemeRegistry;
import org.testng.Assert;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class PocTest {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();

    @BeforeTest
    public static void testSetup() {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        // Later these can be set from a data provider, test xml file, and/or maven test profile
        RestAssured.baseURI = "http://162.243.186.156:5080";
        RestAssured.basePath = "/api/";
        RestAssured.authentication = preemptive().basic("administrator", "Admin");

    }

    private static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);

    }

    // Mock Test - Validates JSON from a file using the Cleo RestAPI schemas from Nexus
    @Test
    public void mockConTest() throws Exception {
        String content = getResource("as2-connection.json");
        JsonNode node = schemaValid.validate(content, "connection");

    }

    // Mock Test - Validates JSON from a file using the Cleo RestAPI schemas from Nexus
    @Test
    public void mockCertTest() throws Exception {
        String content = getResource("as2-certificate.json");
        JsonNode node = schemaValid.validate(content, "certificate");

    }

    // POSTS a new connection using a JSON file and validates the schema
    @Test
    public void liveConPostTest() throws Exception {
        String jsonRequest = getResource("as2-basic-connection.json");
        // JsonNode node = validate(Post(jsonRequest, 201, "/connections"), "connection");
        JsonNode node = schemaValid.validate(httpRequest.Post(jsonRequest, 201, "/connections"), "connection");
        JsonNode getNode = schemaValid.validate(httpRequest.Get(node.get("id").asText(), 200, "/connections/"), "connection");
        httpRequest.Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    // Generates a new certificate using a request from a json file and then attempts to delete it
    // Disabled until schema validation succeeds
    @Test(enabled = false)
    public void liveCertTest() throws Exception {
        String jsonRequest = getResource("as2-qa-test-certificate.json");
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
