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

import com.jayway.restassured.response.Response;

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
    static JsonSchemaFactory schema_factory;
    static JsonSchema        connection_schema;
    static JsonSchema        certificate_schema;

    static {
        UrlSchemeRegistry.register("resource", ResourceHandler.class);

    }

    private static final URI schema_uri = uriOrElse("http://cleo.com/schemas/");
    private static final URI schema_dir = uriOrElse("resource:/com/cleo/versalex/json/schemas/");

    private static URI uriOrElse(String u) {
        try {
            return new URI(u);

        } catch (URISyntaxException ignore) {}
        return null;

    }

    @BeforeClass
    public static void setup() throws Exception {
        schema_factory = JsonSchemaFactory.byDefault().thaw()
                         .setLoadingConfiguration(
                             LoadingConfiguration.byDefault().thaw()
                             .setURITranslatorConfiguration(
                                 URITranslatorConfiguration.newBuilder()
                                 .setNamespace(schema_uri)
                                 .addPathRedirect(schema_uri, schema_dir)
                                 .freeze())
                             .freeze())
                         .freeze();


        connection_schema = schema("connection.schema");
        certificate_schema = schema("cert.schema");

    }

    public static org.hamcrest.Matcher<?> matchesSchema(String schema) {
        return matchesJsonSchema(uriOrElse(schema_uri.toString()+schema)).using(schema_factory);

    }

    @BeforeTest
    public static void testSetup() {
        // Setup the default URL, API base path, and Preemptive Credentials to use throughout the test
        // Later these can be set from a data provider, test xml file, and/or maven test profile
        RestAssured.baseURI = "http://162.243.186.156:5080";
        RestAssured.basePath = "/api/";
        RestAssured.authentication = preemptive().basic("administrator", "Admin");

    }

    static JsonSchema schema(String fn) throws ProcessingException, IOException {
        // return schema_factory.getJsonSchema(JsonLoader.fromFile(new File(schema_dir, fn)));
        return schema_factory.getJsonSchema(fn);

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
        JsonNode node = schemaValidation(Post(jsonRequest, 201, "/connections"), "connection");
        JsonNode getNode = schemaValidation(Get(node.get("id").asText(), 200, "/connections/"), "connection");
        Delete(getNode.get("id").asText(), 204, "/connections/");

    }

    // Generates a new certificate using a request from a json file and then attempts to delete it
    @Test(enabled = false)
    public void liveCertTest() throws Exception {
        String jsonRequest = getResource("as2-qa-test-certificate.json");
        // Remove later
        String postResponse = Post(jsonRequest, 201, "/certs");
        System.out.println("Json Post Response: " + postResponse);
        // Remove later

        // JsonNode node = schemaValidation(Post(jsonRequest, 201, "/certs"), "certificate");
        JsonNode node = schemaValidation(postResponse, "certificate");

        Delete(node.get("id").asText(), 204, "/certs/");

    }

    // Generates a new certificate using a request generated from a POJO and then attempts to delete it
    @Test(enabled = true)
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

        System.out.println("JSONObject: " + newCert.toString());

        String postResponse = Post(newCert, 201, "/certs");

        JsonNode node = schemaValidation(postResponse, "certificate");

        Delete(node.get("id").asText(), 204, "/certs/");

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
        String postResp = Post(newConnection, 201, "/connections");

        // Validate the response against the schema
        JsonNode postNode = schemaValidation(postResp, "connection");

        // Verify content pertinent to the test, the connection should not be ready
        Assert.assertEquals(postNode.get("ready").asText(), "false");
        Assert.assertEquals(postNode.get("notReadyReason").asText(), "Server Address is required.");

        // Attempt a Get to verify permanence, validate the JSON returned against the connection schema
        String getResp = Get(postNode.get("id").asText(), 200, "/connections/");
        JsonNode getNode = schemaValidation(getResp, "connection");

        // Prep an ObjectNode to do a put on the Connection
        ObjectNode postCon = (ObjectNode)getNode;
        postCon.remove("meta");
        postCon.putObject("connect").put("url", "http://localhost:5080/as2");

        // Attempt to do a Put on the already POSTED connection to update the trading partner's URL
        String putResp = Put(postCon, 200, "/connections/" + getNode.get("id").asText());
        JsonNode putNode = schemaValidation(putResp, "connection");

        // Verify that the content has changed due to the Put
        Assert.assertEquals(putNode.get("ready").asText(), "true");
        Assert.assertEquals(putNode.get("connect").get("url").asText(), "http://localhost:5080/as2");

        // Attempt the final Get to verify permanence, validate the JSON returned against the connection schema
        String finalGet = Get(putNode.get("id").asText(), 200, "/connections/");
        JsonNode finalNode = schemaValidation(finalGet, "connection");

        // Attempt a delete to clean up after the test
        Delete(putNode.get("id").asText(), 204, "/connections/");

        // Attempt a Get to ensure that it's been deleted
        Get(putNode.get("id").asText(), 404, "/connections/");

    }

    // Attempt to validate the response against the schema and then return the JsonNode used for validation
    public static JsonNode schemaValidation(String reqResp, String schemaType) throws IOException, ProcessingException {
        JsonNode myNode = new ObjectMapper().readTree(reqResp);

        // Verify that the response is not null
        Assert.assertNotNull(myNode);

        // Verify the response against the requested schema
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
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String Put(Object reqObj, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).put(url);
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
