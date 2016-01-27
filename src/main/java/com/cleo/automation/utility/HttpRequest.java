package com.cleo.automation.utility;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.jayway.restassured.response.Response;
import org.json.JSONArray;
import org.json.JSONObject;
import org.testng.Assert;

import java.util.List;

import static com.jayway.restassured.RestAssured.given;

/**
 * Created by mmyers on 10/29/2015.
 */
public class HttpRequest {
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

    // Does a POST but returns an Object instead of a jsonResponse as string
    // Mostly used when avoiding schema validation
    public static ObjectNode PostReturnObj(Object reqObj, int expStatus, String url) throws Exception {
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        // JsonNode myNode = new ObjectMapper().readTree(reqResp);
        JsonNode postNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)postNode;

    }

    public static ObjectNode PostReturnObj(String requestJson, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(requestJson).post(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode postNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)postNode;

    }

    public static String Put(String requestJson, int expStatus, String url) {
        Response resp = given().contentType("application/json").and().body(requestJson).put(url);
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

    // Does a PUT but returns an Object instead of a jsonResponse as string
    // Mostly used when avoiding schema validation
    public static ObjectNode PutReturnObj(Object reqObj, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(reqObj.toString()).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode putNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)putNode;

    }

    public static ObjectNode PutReturnObj(String requestJson, int expStatus, String url) throws Exception{
        Response resp = given().contentType("application/json").and().body(requestJson).put(url);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode putNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)putNode;

    }

    public static String Get(String conId, int expStatus, String url) {
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static ObjectNode GetReturnObj(String conId, int expStatus, String url) throws Exception{
        Response resp = given().when().get(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode getNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)getNode;

    }

    public static void Delete(String conId, int expStatus, String url) {
        Response resp = given().and().delete(url + conId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);

    }

    public static String Run(String actId, int expStatus) {
        Response resp = given().contentType("application/json;").and().post("/actions/" + actId + "/run");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static ObjectNode RunReturnObj(String actId, int expStatus) throws Exception{
        Response resp = given().contentType("application/json;").and().post("/actions/" + actId + "/run");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode runNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)runNode;

    }

    public static String UploadFile(String conId, int expStatus, byte[] byteStream) {
        Response resp = given().contentType("application/octet-stream").content(byteStream).and().post("/connections/" + conId + "/incoming");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static ObjectNode UploadReturnObj(String conId, int expStatus, byte[] byteStream) throws Exception {
        Response resp = given().contentType("application/octet-stream").content(byteStream).and().post("/connections/" + conId + "/incoming");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode uploadNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)uploadNode;

    }

    // ATTENTION: Returns the resp rather than a String or an Object, the header is needed for testing
    public static Response DownloadFile(String conId, String fileId, int expStatus) {
        Response resp = given().when().get("/connections/" + conId + "/outgoing/" + fileId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        return resp;

    }

    public static ObjectNode DownloadReturnObj(String conId, String fileId, int expStatus) throws Exception {
        Response resp = given().when().get("/connections/" + conId + "/outgoing/" + fileId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode uploadNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)uploadNode;

    }

    // /connections/{connectionId}/incoming/{fileId}
    public static String GetInboxFile(String conId, String fileId, int expStatus) {
        Response resp = given().when().get("/connections/" + conId + "/incoming/" + fileId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONObject jsonResponse = new JSONObject(resp.asString());
        return jsonResponse.toString();

    }

    public static ObjectNode GetInboxFileReturnObj(String conId, String fileId, int expStatus) throws Exception {
        Response resp = given().when().get("/connections/" + conId + "/incoming/" + fileId);
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JsonNode fileNode = new ObjectMapper().readTree(resp.asString());
        return (ObjectNode)fileNode;

    }

    // private static List<JsonNode> jsonNodes = new ArrayList<>();
    public static JSONArray GetInbox(String conId, int expStatus) {
        Response resp = given().when().get("/connections/" + conId + "/incoming");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONArray inboxFiles = new JSONObject(resp.asString()).getJSONArray("resources");
        return inboxFiles;

    }

    public static JSONArray GetOutbox(String conId, int expStatus) {
        Response resp = given().when().get("/connections/" + conId + "/outgoing");
        Assert.assertEquals(resp.getStatusCode(), expStatus);
        JSONArray outboxFiles = new JSONObject(resp.asString()).getJSONArray("resources");
        return outboxFiles;

    }

}
