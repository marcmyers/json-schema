package com.cleo.labs.json_schema;

import static org.junit.Assert.*;
import static com.jayway.restassured.RestAssured.given;

import java.io.File;
import java.io.IOException;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.response.Response;
import org.junit.BeforeClass;
import org.junit.Test;

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

public class YamlTest {

    static String            schema_uri;
    static String            schema_dir;
    static JsonSchemaFactory schema_factory;
    static YAMLFactory       yaml_factory;
    static JsonSchema        connection_schema;

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
        String content = (POST("administrator", "Admin", jsonRequest, "http://162.243.186.156:5080/api/connections")).toString();
        JsonNode node = new ObjectMapper().readTree(content);
        assertNotNull(node);
        System.out.println(node);
        assertSuccess(connection_schema.validate(node));
        System.out.println(node.get("alias"));

    }

    public static JSONObject POST(String userName, String userPass, String requestJson, String url) {
        Response resp = given().auth().preemptive().basic(userName, userPass).contentType("application/json; charset=UTF-8").and().body(requestJson).post(url);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse;

    };

    // If this works, it should live in a helper class
    public JsonNode responseAsNode(com.jayway.restassured.response.Response response) {
        ObjectNode node = JsonNodeFactory.instance.objectNode();
        node.set("status", JsonNodeFactory.instance.numberNode(response.statusCode()));
        node.set("type", JsonNodeFactory.instance.textNode(response.contentType()));
        ObjectNode headers = JsonNodeFactory.instance.objectNode();
        response.getHeaders().forEach((h)->headers.set(h.getName(),JsonNodeFactory.instance.textNode(h.getValue())));
        if (headers.size()>0) {
            node.set("headers", headers);
        }
        ObjectNode cookies = JsonNodeFactory.instance.objectNode();
        response.getCookies().forEach((name,value)->cookies.set(name,JsonNodeFactory.instance.textNode(value)));
        if (cookies.size()>0) {
            node.set("cookies", cookies);
        }
        JsonNode body = null;
        if (response.contentType().equals("application/json")) {
            try {
                body = new ObjectMapper().readTree(response.asByteArray());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (body==null) {
            String text = response.asString();
            if (text.matches(".*[^\\p{Print}\\p{Space}].*")) {
                body = JsonNodeFactory.instance.binaryNode(response.asByteArray());
            } else {
                body = JsonNodeFactory.instance.textNode(text);
            }
        }
        node.set("body", body);
        return node;
    }

}
