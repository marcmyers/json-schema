package com.cleo.labs.json_schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.load.configuration.LoadingConfiguration;
import com.github.fge.jsonschema.core.load.uri.URITranslatorConfiguration;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.github.fge.jsonschema.main.JsonSchemaFactory;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.skife.url.UrlSchemeRegistry;
import org.testng.Assert;
import org.testng.annotations.BeforeClass;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.HashMap;

import static com.jayway.restassured.module.jsv.JsonSchemaValidator.matchesJsonSchema;

/**
 * Created by mmyers on 10/29/2015.
 */
public class SchemaValidation {
    private static HashMap<String, JsonSchema> schemas = Maps.newHashMap(new ImmutableMap.Builder<String, JsonSchema>()
                    .put("connection", schema("connection.schema") )
                    .put("certificate", schema("cert.schema"))
                    .build()

    );

    private static JsonSchema schema(String reqSchema) {
        try {
            return JsonSchemaFactorySingleton.getInstance().getJsonSchema(reqSchema);

        } catch (Exception Ex) {
            Ex.printStackTrace();

        }
        return null;

    }

    static void assertSuccess(ProcessingReport report) throws JsonProcessingException {
        if (!report.isSuccess()) {
            System.out.println(report);

        }
        Assert.assertTrue(report.isSuccess());

    }

    // Attempt to validate the response against the schema and then return the JsonNode used for validation
    public JsonNode validate(String reqResp, String schemaType) throws IOException, ProcessingException {
        JsonNode myNode = new ObjectMapper().readTree(reqResp);

        // Verify that the response is not null
        Assert.assertNotNull(myNode);

        assertSuccess(schemas.get(schemaType).validate(myNode));

        return myNode;

    }


}
