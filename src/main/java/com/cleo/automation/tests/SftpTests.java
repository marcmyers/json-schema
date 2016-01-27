package com.cleo.automation.tests;

import com.cleo.automation.utility.HttpRequest;
import com.cleo.automation.utility.SchemaValidation;
import com.cleo.automation.utility.Utils;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;
import org.testng.annotations.AfterTest;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mmyers on 1/27/16.
 */
public class SftpTests {
    static HttpRequest httpRequest = new HttpRequest();
    static SchemaValidation schemaValid = new SchemaValidation();
    static Utils util = new Utils();
    private static List<JsonNode> jsonNodes = new ArrayList<>();

    @Test(enabled=true)
    public void sftpPostTypeOnly() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-type-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");

        // Validate
        Assert.assertNotNull(postResponse.get("id"));
        Assert.assertNotNull(postResponse.get("alias"));

        jsonNodes.add(postResponse);

        // Attempt GET
        ObjectNode getResponse = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        // Validate
        Assert.assertNotNull(getResponse.get("id"));
        Assert.assertNotNull(getResponse.get("alias"));

    }

    @Test(dependsOnMethods = {"sftpPostTypeOnly"}, enabled=true)
    public void sftpPut() throws Exception{
        // POST new FTP Connection
        String jsonRequest = util.getResource("json/sftp-connection-type-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");

        // Verify that it's not ready
        Assert.assertEquals(postResponse.get("ready").asText(), "false");
        Assert.assertEquals(postResponse.get("notReadyReason").asText(), "Server Address is required.");

        jsonNodes.add(postResponse);

        // Attempt GET
        ObjectNode getResponse = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        // Prep connection for a PUT that should make it ready
        getResponse.putObject("connect").put("port", 5022).put("host", "localhost").put("username", "myTradingPartner").put("password", "cleo");

        // Attempt PUT
        ObjectNode putResponse = httpRequest.PutReturnObj(getResponse, 200, "/connections/" + getResponse.get("id").asText());

        // Attempt GET
        ObjectNode getPostPut = httpRequest.GetReturnObj(putResponse.get("id").asText(), 200, "/connections/");

        // Validate
        Assert.assertEquals(getPostPut.get("ready").asText(), "true");
        Assert.assertEquals(getPostPut.get("connect").get("port").asText(), "5022");
        Assert.assertEquals(getPostPut.get("connect").get("host").asText(), "localhost");
        Assert.assertEquals(getPostPut.get("connect").get("username").asText(), "myTradingPartner");

    }

    // POST an FTP Connection, attempt a DELETE, then attempt a GET to ensure it was deleted
    @Test(dependsOnMethods = {"sftpPostTypeOnly"}, enabled=true)
    public void sftpDelete() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-type-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");
        httpRequest.Delete(getCon.get("id").asText(), 204, "/connections/");
        String attemptGet = httpRequest.Get(getCon.get("id").asText(), 404, "/connections/");

    }

    // POST an Ftp Connection, POST an Action with the Ftp Connections self link
    @Test(dependsOnMethods = {"sftpPostTypeOnly"}, enabled=true)
    public void sftpPostAction() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-ready-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actObj = util.getObjResource("json/action-request.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actObj.putObject("connection").put("href", selfLink);

        ObjectNode postAct = httpRequest.PostReturnObj(actObj, 201, "/actions");
        ObjectNode getAct = httpRequest.GetReturnObj(postAct.get("id").asText(), 200, "/actions/");

        // Verify that the action's connect is set to the connection
        Assert.assertEquals(getAct.get("connection"), getCon.get("_links").get("self"));

        String getConVerify = httpRequest.Get(getCon.get("id").asText(), 200, "/connections/");
        String actSelfLink = ((getAct.get("_links").get("self").get("href")).asText());

        // Verify that the connection lists the action
        Assert.assertTrue(getConVerify.contains(actSelfLink));

    }

    @Test(dependsOnMethods = {"sftpPostAction"}, enabled=true)
    public void sftpRunAction() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-ready-no-alias-request.json");
        ObjectNode postResponse = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postResponse.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actObj = util.getObjResource("json/action-request.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actObj.putObject("connection").put("href", selfLink);

        ObjectNode postAct = httpRequest.PostReturnObj(actObj, 201, "/actions");
        ObjectNode getAct = httpRequest.GetReturnObj(postAct.get("id").asText(), 200, "/actions/");

        // Verify that the action's connect is set to the connection
        Assert.assertEquals(getAct.get("connection"), getCon.get("_links").get("self"));

        String getConVerify = httpRequest.Get(getCon.get("id").asText(), 200, "/connections/");
        String actSelfLink = ((getAct.get("_links").get("self").get("href")).asText());

        // Verify that the connection lists the action
        Assert.assertTrue(getConVerify.contains(actSelfLink));

        // Run the action
        ObjectNode runAttempt = httpRequest.RunReturnObj(getAct.get("id").asText(), 200);

        // Verify the action was run and successful - {"status":"completed","result":"success",
        Assert.assertEquals(runAttempt.get("status").asText(), "completed");
        Assert.assertEquals(runAttempt.get("result").asText(), "success");

        util.cleanBoxes();

    }

    // List File Outbox
    @Test(dependsOnMethods = {"sftpRunAction"}, enabled=true)
    public void sftpListOutbox() throws Exception{
        Boolean listCheck = true;
        String jsonRequest = util.getResource("json/sftp-connection-ready-no-alias-request.json");
        ObjectNode postCon = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postCon.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actRequest = util.getObjResource("json/action-fill-outbox.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actRequest.putObject("connection").put("href", selfLink);

        ObjectNode actObj = httpRequest.PostReturnObj(actRequest, 201, "/actions");
        httpRequest.Run(actObj.get("id").asText(), 200);

        JSONArray inboxFiles = httpRequest.GetOutbox(getCon.get("id").asText(), 200);

        // TODO: Expected files should eventually come from a csv data provider
        List<String> expFiles = new ArrayList<>();
        expFiles.add("test.edi");
        expFiles.add("test.HL7");
        expFiles.add("test3B2.xml");
        expFiles.add("testEBICS.xml");

        for (String file : expFiles) {
            if (!inboxFiles.toString().contains(file.toString())) {
                listCheck = false;
                break;

            }
        }

        Assert.assertTrue(listCheck);

        util.cleanBoxes();

    }

    // List Files Inbox
    @Test(dependsOnMethods = {"sftpRunAction"}, enabled=true)
    public void sftpListInbox() throws Exception{
        Boolean listCheck = true;
        String jsonRequest = util.getResource("json/sftp-connection-ready-no-alias-request.json");
        ObjectNode postCon = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postCon.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actRequest = util.getObjResource("json/action-fill-inbox.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actRequest.putObject("connection").put("href", selfLink);

        ObjectNode actObj = httpRequest.PostReturnObj(actRequest, 201, "/actions");
        httpRequest.Run(actObj.get("id").asText(), 200);

        JSONArray inboxFiles = httpRequest.GetInbox(getCon.get("id").asText(), 200);

        // TODO: Expected files should eventually come from a csv data provider
        List<String> expFiles = new ArrayList<>();
        expFiles.add("test.edi");
        expFiles.add("test.HL7");
        expFiles.add("test3B2.xml");
        expFiles.add("testEBICS.xml");

        for (String file : expFiles) {
            if (!inboxFiles.toString().contains(file.toString())) {
                listCheck = false;
                break;

            }
        }

        Assert.assertTrue(listCheck);

        util.cleanBoxes();

    }

    // Contingency Download
    @Test(dependsOnMethods = {"sftpPostTypeOnly"}, enabled=true)
    public void sftpContDownload() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-ready-no-alias-request.json");
        ObjectNode postCon = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postCon.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode actRequest = util.getObjResource("json/action-fill-outbox.json");

        String selfLink = (getCon.get("_links").get("self").get("href")).asText();
        actRequest.putObject("connection").put("href", selfLink);

        ObjectNode actObj = httpRequest.PostReturnObj(actRequest, 201, "/actions");
        httpRequest.Run(actObj.get("id").asText(), 200);

        JSONArray outboxFiles = httpRequest.GetOutbox(getCon.get("id").asText(), 200);
        JSONObject downloadFile = outboxFiles.getJSONObject(0);

        // Download the file
        Response resp = httpRequest.DownloadFile(getCon.get("id").asText(), downloadFile.get("id").toString(), 200);

        Assert.assertTrue(resp.getHeader("Content-Disposition").contains(downloadFile.get("name").toString()));
        Assert.assertEquals(resp.getHeader("Content-Length").toString(), downloadFile.get("length").toString());
        Assert.assertEquals(resp.getHeader("Content-Type").toString(), "application/octet-stream");

        // Clean out the inbox
        util.cleanBoxes();

    }

    // Contingency Upload
    // new byte[128]
    @Test(dependsOnMethods = {"sftpPostTypeOnly"}, enabled=true)
    public void sftpContUpload() throws Exception{
        String jsonRequest = util.getResource("json/sftp-connection-ready-no-alias-request.json");
        ObjectNode postCon = httpRequest.PostReturnObj(jsonRequest, 201, "/connections");
        ObjectNode getCon = httpRequest.GetReturnObj(postCon.get("id").asText(), 200, "/connections/");

        jsonNodes.add(getCon);

        ObjectNode uploadNode = httpRequest.UploadReturnObj(getCon.get("id").asText(), 201, new byte[128]);

        // Validate the upload
        Assert.assertEquals(uploadNode.get("length").asInt(), 128);
        Assert.assertEquals(uploadNode.get("meta").get("resourceType").asText(), "incoming");
        Assert.assertNotNull(uploadNode.get("name"));
        Assert.assertNotNull(uploadNode.get("id"));

        // TODO: uncomment once the defect around cont upload self links is fixed
        /*

        // Attempt to get just the one file to verify it landed
        ObjectNode getFile = httpRequest.GetInboxFileReturnObj(getCon.get("id").asText(), uploadNode.get("id").asText(), 200);

        Assert.assertEquals(uploadNode.get("length"), getFile.get("length"));
        Assert.assertEquals(uploadNode.get("meta").get("resourceType"), getFile.get("meta").get("resourceType"));
        Assert.assertEquals(uploadNode.get("name"), getFile.get("name"));
        Assert.assertEquals(uploadNode.get("id"), getFile.get("id"));

        */

        // Clean out the inbox
        util.cleanBoxes();

    }

    // TODO: write these tests once Defect D-02166 is fixed
    // Ftp Transfers /connections/{connectionId}/transfers
    // Ftp Events
    // Re-send
    // Re-receive

    // Intended to clean up after the testing regardless of the test outcome. Nothing is asserted in the method because we'll attempt to delete all the jsonNodes
    @AfterTest
    public void cleanUp() throws Exception{
        util.cleanUp(jsonNodes);
        util.cleanBoxes();

    }
}
