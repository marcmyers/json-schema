package com.cleo.labs.json_schema;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.cleo.lexicom.external.pojo.Connect;
import com.cleo.lexicom.external.pojo.Connection;
import com.cleo.lexicom.external.pojo.PostCert;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jsonschema.core.report.ProcessingMessage;
import com.jayway.restassured.RestAssured;
import org.json.JSONObject;
import org.testng.Assert;

import com.jayway.restassured.response.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JsonLoader;
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
    static String            schema_uri;
    static String            schema_dir;
    static JsonSchemaFactory schema_factory;
    static JsonSchema        connection_schema;
    static JsonSchema        certificate_schema;

    @BeforeClass
    public static void setup() throws Exception {
        schema_uri = "http://cleo.com/schemas/";
        schema_dir = new File("../rest-api/schema/").getCanonicalPath();
        
        schema_factory = JsonSchemaFactory.byDefault().thaw()
                         .setLoadingConfiguration(
                             LoadingConfiguration.byDefault().thaw()
                             .setURITranslatorConfiguration(
                                 URITranslatorConfiguration.newBuilder()
                                 .setNamespace(schema_uri)
                                 .addPathRedirect(schema_uri, "file:///G:/IdeaProjects/rest-api/schema/")
                                 .freeze())
                             .freeze())
                         .freeze();

        connection_schema = schema("connection.schema");
        certificate_schema = schema("cert.schema");

    }

    @BeforeTest
    public static void testSetup() {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        RestAssured.baseURI = "http://162.243.186.156:5080";
        RestAssured.basePath = "/api/";
        RestAssured.authentication = preemptive().basic("administrator", "Admin");

    }

    static JsonSchema schema(String fn) throws ProcessingException, IOException {
        return schema_factory.getJsonSchema(JsonLoader.fromFile(new File(schema_dir, fn)));

    }

    private static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);

    }

    static void assertSuccess(ProcessingReport report) throws JsonProcessingException {
        if (!report.isSuccess()) {
            System.out.println(report);

        }
        Assert.assertTrue(report.isSuccess());

    }

    // Mock Test - Validates JSON from a file using the Cleo RestAPI schemas
    @Test
    public void mockTest() throws Exception {
        String content = getResource("as2-connection.json");
        JsonNode node = new ObjectMapper().readTree(content);
        Assert.assertNotNull(node);
        assertSuccess(connection_schema.validate(node));

    }

    // POSTS a new connection using a JSON file and validates the schema
    @Test
    public void liveConTest() throws Exception {
        String jsonRequest = getResource("as2-basic-connection.json");
        JsonNode node = SchemaValidation(Post(jsonRequest, 201, "/connections"), "connection");
        JsonNode getNode = SchemaValidation(Get(node.get("id").asText(), 200, "/connections/"), "connection");
        Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    // Generates a new certificate using a request from a json file and then attempts to delete it
    @Test(enabled=false)
    public void liveCertTest() throws Exception {
        String jsonRequest = getResource("as2-qa-test-certificate.json");
        // Remove later
        String postResponse = Post(jsonRequest, 201, "/certs");
        System.out.println("Json Post Response: " + postResponse);
        // Remove later

        // JsonNode node = SchemaValidation(Post(jsonRequest, 201, "/certs"), "certificate");
        JsonNode node = SchemaValidation(postResponse, "certificate");

        Delete(node.get("id").asText(), 204, "/certs/");

    }

    // Generates a new certificate using a request generated from a POJO and then attempts to delete it
    @Test(enabled=false)
    public void liveCertTestPOJO() throws Exception {
        PostCert newCert = new PostCert();

        List<String> keyUsage = new ArrayList<String>();
        keyUsage.add("digitalSignature");

        newCert.setRequestType("generateCert");
        newCert.setAlias("QA_TEST_POJO");
        newCert.setDn("cn=test,c=us");
        newCert.setValidity("24 months");
        newCert.setKeyAlg("rsa");
        newCert.setKeySize(2048);
        newCert.setKeyUsage(keyUsage);
        newCert.setPassword("cleo");

        String postResponse = Post(newCert, 201, "/certs");

        System.out.println("Pojo Post Response: " + postResponse);

        JsonNode node = SchemaValidation(postResponse, "certificate");

        Delete(node.get("id").asText(), 204, "/certs/");

    }

    // POSTS a new connection using POJOs and validates the Responses using the schema
    // Then, attempts to do a Put on the connection to make it ready
    @Test
    public void liveConTestPOJO() throws Exception {
        Connection newConnection = new Connection();
        newConnection.setType("as2");

        // Attempt a POST to generate a new connection
        String postResp = Post(newConnection, 201, "/connections");

        // Validate the response against the schema
        JsonNode postNode = SchemaValidation(postResp, "connection");

        // ((ObjectNode)postNode).put();

        // Verify content pertinent to the test, the connection should not be ready
        Assert.assertEquals(postNode.get("ready").asText(), "false");
        Assert.assertEquals(postNode.get("notReadyReason").asText(), "Server Address is required.");

        // Attempt a Get to verify permanence, validate the JSON returned against the connection schema
        String getResp = Get(postNode.get("id").asText(), 200, "/connections/");
        JsonNode getNode = new ObjectMapper().readTree(getResp);
        Assert.assertNotNull(getNode);
        assertSuccess(connection_schema.validate(getNode));

        // Prep the connection for an attempted Put
        Connection postedConnection = new Connection();
        postedConnection = new ObjectMapper().readValue(postResp, Connection.class);
        Connect localHost = new Connect();
        localHost.setUrl("http://localhost:5080/as2");

        // Set the value needed to make the connection ready
        postedConnection.setConnect(localHost);

        // Null meta data, this data isn't valid when sent with a Put
        postedConnection.setMeta(null);

        // Attempt to do a Put on the already POSTED connection to update the trading partner's URL
        String putResp = Put(postedConnection, 200, "/connections/" + postedConnection.getId());
        JsonNode putNode = SchemaValidation(putResp, "connection");

        // Verify that the content has changed due to the Put
        Assert.assertEquals(putNode.get("ready").asText(), "true");
        Assert.assertEquals(putNode.get("connect").get("url").asText(), "http://localhost:5080/as2");

        // Attempt the final Get to verify permanence, validate the JSON returned against the connection schema
        String finalGet = Get(putNode.get("id").asText(), 200, "/connections/");
        JsonNode finalNode = SchemaValidation(finalGet, "connection");

        // Attempt a delete to clean up after the test
        Delete(putNode.get("id").asText(), 204, "/connections/");

        // Attempt a Get to ensure that it's been deleted
        Get(putNode.get("id").asText(), 404, "/connections/");

    }

    // Attempt to validate the response against the schema
    public static JsonNode SchemaValidation(String reqResp, String schemaType) throws IOException, ProcessingException {
        JsonNode myNode = new ObjectMapper().readTree(reqResp);

        // Verify that the response is not null
        Assert.assertNotNull(myNode);

        // Verify the response against the schema
        if (schemaType == "connection") {
            assertSuccess(connection_schema.validate(myNode));

        } else if (schemaType == "certificate") {
            assertSuccess(certificate_schema.validate(myNode));

        }

        return myNode;

    }

    public static String Post(String requestJson, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Post(Object reqObj, int expStatus, String url) {
        JSONObject jsonRequest = new JSONObject(reqObj);
        Response resp = given().contentType("application/json").and().body(jsonRequest.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Put(Object reqObj, int expStatus, String url) {
        JSONObject jsonRequest = new JSONObject(reqObj);
        Response resp = given().contentType("application/json").and().body(jsonRequest.toString()).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Get(String conId, int expStatus, String url) {
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static void Delete(String conId, int expStatus, String url) {
        Response resp = given().and().delete(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);

    }

}
