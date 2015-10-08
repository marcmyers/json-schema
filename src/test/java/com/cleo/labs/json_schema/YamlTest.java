package com.cleo.labs.json_schema;

import static org.junit.Assert.*;
import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.IOException;

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

public class YamlTest {

    static String            schema_uri;
    static String            schema_dir;
    static JsonSchemaFactory schema_factory;
    static YAMLFactory       yaml_factory;
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

        yaml_factory = new YAMLFactory();
        connection_schema = schema("connection.schema");
    }

    static JsonSchema schema(String fn) throws ProcessingException, IOException {
        return schema_factory.getJsonSchema(JsonLoader.fromFile(new File(schema_dir, fn)));
    }

    private static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);
    }

    @SuppressWarnings("unused")
    private static String getFile(String filename) throws IOException {
        return Files.toString(new File(filename), Charsets.UTF_8);
    }

    static void assertSuccess(ProcessingReport report) {
        if (!report.isSuccess()) {
            System.out.println(report);
        }
        assertTrue(report.isSuccess());
    }

    @Test
    public void test1() throws Exception {
        String content = getResource("as2-connection.json");
        JsonNode node = new ObjectMapper().readTree(content);
        assertNotNull(node);
        //System.out.println(node);
        assertSuccess(connection_schema.validate(node));
    }

    @Test
    public void test2() throws Exception {
        String content = getResource("as2-connection.yaml");
        JsonNode node = new ObjectMapper().readTree(yaml_factory.createParser(content));
        assertNotNull(node);
        //System.out.println(node);
        assertSuccess(connection_schema.validate(node));
    }

    // POSTS a new cert and then validates the schema
    @Test
    public void liveExpTest() throws Exception {
        String jsonRequest = getResource("as2-basic-connection.json");
        JsonNode node = new ObjectMapper().readTree(POST("administrator", "Admin", jsonRequest, 201, "http://162.243.186.156:5080/api/connections"));
        assertNotNull(node);
        assertSuccess(connection_schema.validate(node));

    }

    public static String POST(String userName, String userPass, String requestJson, int expStatus, String url) {
        Response resp = given().auth().preemptive().basic(userName, userPass).contentType("application/json; charset=UTF-8").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    };

}
