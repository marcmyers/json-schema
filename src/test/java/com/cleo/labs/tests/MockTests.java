package com.cleo.labs.tests;

import com.cleo.labs.json_schema.SchemaValidation;
import com.fasterxml.jackson.databind.JsonNode;
import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import org.testng.annotations.Test;

import java.io.IOException;

/**
 * Created by mmyers on 10/30/2015.
 */
// These methods are intended to gut check our schema validation against JSON files
public class MockTests {
    static SchemaValidation schemaValid = new SchemaValidation();

    // This is used more than once, it needs to live somewhere else
    private static String getResource(String resource) throws IOException {
        return Resources.toString(Resources.getResource(resource), Charsets.UTF_8);

    }

    @Test
    public void mockConTest() throws Exception {
        String content = getResource("as2-connection.json");
        JsonNode node = schemaValid.validate(content, "connection");

    }

    @Test
    public void mockCertTest() throws Exception {
        String content = getResource("as2-certificate.json");
        JsonNode node = schemaValid.validate(content, "certificate");

    }

    @Test
    public void mockActionTest() throws Exception {
        String content = getResource("as2-action.json");
        JsonNode node = schemaValid.validate(content, "action");

    }

    @Test
    public void mockTransferTest() throws Exception {
        String content = getResource("as2-transfer.json");
        JsonNode node = schemaValid.validate(content, "transfer");

    }

    @Test
    public void mockEventTest() throws Exception {
        String content = getResource("as2-event.json");
        JsonNode node = schemaValid.validate(content, "event");

    }

}
