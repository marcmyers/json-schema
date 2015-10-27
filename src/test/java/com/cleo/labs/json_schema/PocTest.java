package com.cleo.labs.json_schema;

import static com.jayway.restassured.RestAssured.given;
import static com.jayway.restassured.RestAssured.preemptive;

import java.io.File;
import java.io.IOException;

import com.cleo.lexicom.external.pojo.Connect;
import com.cleo.lexicom.external.pojo.Connection;
import com.jayway.restassured.RestAssured;
import org.json.JSONObject;
import org.testng.Assert;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.response.Response;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.base.Charsets;
import com.google.common.io.Files;
import com.google.common.io.Resources;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

public class PocTest {

    static String            schema_uri;
    static String            schema_dir;
    static JsonSchemaFactory schema_factory;
    static JsonSchema        connection_schema;

    @BeforeTest
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

        // Setup the default URL and API base path to use throughout the test, as well as credentials
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

    static void assertSuccess(ProcessingReport report) {
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

    // POSTS a new cert and validates the schema
    @Test
    public void liveExpTest() throws Exception {
        String jsonRequest = getResource("as2-basic-connection.json");
        // JsonNode node = new ObjectMapper().readTree(POST("administrator", "Admin", jsonRequest, 201, "/connections"));
        JsonNode node = new ObjectMapper().readTree(POST(jsonRequest, 201, "/connections"));
        Assert.assertNotNull(node);
        assertSuccess(connection_schema.validate(node));
        DELETE(node.get("id").asText(), 204, "/connections/");

    }

    // POSTS a new cert using POJOs and validates the Responses using the schema
    @Test
    public void liveExpTestPOJO() throws Exception {
        Connection newConnection = new Connection();
        newConnection.setType("as2");

        // Attempt a POST to generate a new connection
        String postResp =
                POST(newConnection, 201, "/connections");
        JsonNode postNode = new ObjectMapper().readTree(postResp);

        // Verify that the response is not null
        Assert.assertNotNull(postNode);

        // Verify the response against the schema
        assertSuccess(connection_schema.validate(postNode));

        // Verify content pertinent to the test, the connection should not be ready
        Assert.assertEquals(postNode.get("ready").asText(), "false");
        Assert.assertEquals(postNode.get("notReadyReason").asText(), "Server Address is required.");

        // Attempt a GET to verify permanence, validate the JSON returned against the connection schema
        String getResp = GET(postNode.get("id").asText(), 200, "/connections/");
        JsonNode getNode = new ObjectMapper().readTree(getResp);
        Assert.assertNotNull(getNode);
        assertSuccess(connection_schema.validate(getNode));

        // Prep the connection for an attempted PUT
        Connection postedConnection = new Connection();
        postedConnection =
                new ObjectMapper().readValue(postResp, Connection.class);
        Connect localHost =
                new Connect();
        localHost.setUrl("http://localhost:5080/as2");

        // Set the value needed to make the connection ready
        postedConnection.setConnect(localHost);

        // Null meta data, this data isn't valid when sent with a PUT
        postedConnection.setMeta(null);

        // Attempt to do a PUT on the already POSTED connection to update the trading partner's URL
        String putResp = PUT(postedConnection, 200, "/connections/" + postedConnection.getId());
        JsonNode putNode = new ObjectMapper().readTree(putResp);
        Assert.assertNotNull(putNode);

        // Verify the response against the schema
        assertSuccess(connection_schema.validate(putNode));

        // Verify that the content has changed due to the PUT
        Assert.assertEquals(putNode.get("ready").asText(), "true");

        // Attempt the final GET to verify permanence, validate the JSON returned against the connection schema
        String finalGet = GET(putNode.get("id").asText(), 200, "/connections/");
        JsonNode finalNode = new ObjectMapper().readTree(finalGet);
        Assert.assertNotNull(finalNode);
        assertSuccess(connection_schema.validate(finalNode));

        // Attempt a delete to clean up after the test
        DELETE(putNode.get("id").asText(), 204, "/connections/");

        // Attempt a GET to ensure that it's been deleted
        GET(putNode.get("id").asText(), 404, "/connections/");

    }

    // public static String POST(String userName, String userPass, String requestJson, int expStatus, String url) {
    public static String POST(String requestJson, int expStatus, String url) {
        System.out.println(requestJson);
        Response resp = given().contentType("application/json").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String POST(Object reqObj, int expStatus, String url) {
        Connection requestedCon = ((Connection)reqObj);
        JSONObject jsonRequest = new JSONObject(requestedCon);
        Response resp = given().contentType("application/json").and().body(jsonRequest.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String PUT(Connection reqCon, int expStatus, String url) {
        JSONObject jsonRequest = new JSONObject(reqCon);
        Response resp = given().contentType("application/json").and().body(jsonRequest.toString()).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static String GET(String conId, int expStatus, String url) {
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static void DELETE(String conId, int expStatus, String url) {
        Response resp = given().and().delete(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);

    }

}
