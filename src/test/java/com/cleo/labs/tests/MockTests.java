package com.cleo.labs.tests;

import com.cleo.labs.json_schema.SchemaValidation;
import com.cleo.labs.json_schema.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import org.testng.annotations.Test;

/**
 * Created by mmyers on 10/30/2015.
 */
// These methods are intended to gut check our schema validation against JSON files
public class MockTests {
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();

    @Test
    public void mockAs2ConTest() throws Exception {
        String content = util.getResource("as2-connection.json");
        JsonNode node = schemaValid.validate(content, "connection");

    }

    @Test
    public void mockFtpConTest() throws Exception {
        String content = util.getResource("ftp-connection.json");
        JsonNode node = schemaValid.validate(content, "connection");

    }

    @Test
    public void mockCertTest() throws Exception {
        String content = util.getResource("certificate.json");
        JsonNode node = schemaValid.validate(content, "certificate");

    }

    @Test
    public void mockActionTest() throws Exception {
        String content = util.getResource("action.json");
        JsonNode node = schemaValid.validate(content, "action");

    }

    @Test
    public void mockTransferTest() throws Exception {
        String content = util.getResource("as2-transfer.json");
        JsonNode node = schemaValid.validate(content, "transfer");

    }

    @Test
    public void mockEventTest() throws Exception {
        String content = util.getResource("event.json");
        JsonNode node = schemaValid.validate(content, "event");

    }

}
