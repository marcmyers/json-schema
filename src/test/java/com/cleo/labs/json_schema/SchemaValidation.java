package com.cleo.labs.json_schema;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonschema.core.exceptions.ProcessingException;
import com.github.fge.jsonschema.core.report.ProcessingReport;
import com.github.fge.jsonschema.main.JsonSchema;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import org.testng.Assert;

import java.io.IOException;
import java.util.HashMap;

/**
 * Created by mmyers on 10/29/2015.
 */
public class SchemaValidation {
    private static HashMap<String, JsonSchema> schemas = Maps.newHashMap(new ImmutableMap.Builder<String, JsonSchema>()
                    .put("connection", schema("connection.schema") )
                    .put("certificate", schema("cert.schema"))
                    .put("action", schema("action.schema"))
                    .put("collection", schema("collection.schema"))
                    .put("common",schema("common.schema"))
                    .put("event", schema("event.schema"))
                    .put("job", schema("job.schema"))
                    .put("transfer", schema("transfer.schema"))
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
