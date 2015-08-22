package com.cleo.labs.json_schema;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;

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
                                 .addPathRedirect(schema_uri, "file:"+schema_dir+"/")
                                 .freeze())
                             .freeze())
                         .freeze();

        yaml_factory = new YAMLFactory();
        connection_schema = schema("connection_schema.schema");
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
        String content = getResource("as2-connection_schema.json");
        JsonNode node = new ObjectMapper().readTree(content);
        assertNotNull(node);
        //System.out.println(node);
        assertSuccess(connection_schema.validate(node));
    }

    @Test
    public void test2() throws Exception {
        String content = getResource("as2-connection_schema.yaml");
        JsonNode node = new ObjectMapper().readTree(yaml_factory.createParser(content));
        assertNotNull(node);
        //System.out.println(node);
        assertSuccess(connection_schema.validate(node));
    }

}
